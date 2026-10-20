package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaAudience;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaShapeCalculation;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGateOpeningService;
import de.simplepvparena.gate.StartGateVisualEffect;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.support.FakeBlockData;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

final class ArenaMatchHarness {

    static final String WORLD_NAME = "event";
    static final String LOBBY_WORLD_NAME = "lobby";
    static final int WALL_MINIMUM = 0;
    static final int WALL_MAXIMUM = 10;
    static final int GROUND_Y = 64;
    static final int WALL_HEIGHT = 4;
    static final int WALL_TOP_Y = GROUND_Y + WALL_HEIGHT - 1;
    static final int INVENTORY_SLOT_COUNT = 43;
    static final long TICKS_PER_SECOND = 20L;

    private static final Logger LOGGER = Logger.getLogger("ArenaMatchHarness");

    private final Map<UUID, FakePlayer> playersByUniqueId = new LinkedHashMap<>();
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();
    private final List<ScheduledTask> repeatingTasks = new ArrayList<>();
    private final Map<Long, Material> blockMaterials = new LinkedHashMap<>();
    private final BorderMarkIndex borderMarks = new BorderMarkIndex();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final ArenaAudience arenaAudience = new ArenaAudience(this::findOnlinePlayers, arenaRegistry);
    private final List<String> broadcasts = new ArrayList<>();
    private final SpectatorBorderService borderService;
    private final SpectatorService spectatorService;
    private final RecordingFighterColours fighterColours = new RecordingFighterColours();
    private final List<String> roundStartSteps = new ArrayList<>();
    private final List<World> temporaryEntityRemovalWorlds = new ArrayList<>();
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final StartGateOpeningService gateOpeningService;
    private final KitService kitService;
    private final MatchService matchService;
    private final Plugin plugin;
    private final World world;

    private RuntimeException temporaryEntityRemovalFailure;
    private long currentTick;

    ArenaMatchHarness() {
        this(plugin -> new KitService());
    }

    ArenaMatchHarness(Function<Plugin, KitService> kitServiceFactory) {
        this.world = proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            case "getBlockAt" -> createBlock((Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]);
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        this.plugin = createPlugin(createServer());
        installBukkitServer();
        this.borderService = new SpectatorBorderService(borderMarks, () -> { });
        this.spectatorService = new SpectatorService(arenaRegistry, new FakeSpectatorItems());
        this.gateOpeningService = new StartGateOpeningService(plugin, gateMarks, new StartGateDuration(),
                StartGateVisualEffect.NONE);
        this.kitService = kitServiceFactory.apply(plugin);
        this.matchService = new MatchService(plugin, kitService, spectatorService, fighterColours,
                gateOpeningService, (removalWorld, removalShape) -> {
                    temporaryEntityRemovalWorlds.add(removalWorld);
                    roundStartSteps.add("remove temporary entities in " + removalShape.getWorldName());
                    if (temporaryEntityRemovalFailure != null) {
                        throw temporaryEntityRemovalFailure;
                    }
                    return 0;
                });
        buildWallAndShape();
    }

    Plugin getPlugin() {
        return plugin;
    }

    World getWorld() {
        return world;
    }

    ArenaRegistry getArenaRegistry() {
        return arenaRegistry;
    }

    MatchService getMatchService() {
        return matchService;
    }

    ArenaAudience getArenaAudience() {
        return arenaAudience;
    }

    void useArenaAudience() {
        matchService.setArenaAudience(arenaAudience);
    }

    List<String> getBroadcasts() {
        return broadcasts;
    }

    KitService getKitService() {
        return kitService;
    }

    StartGateMarks getGateMarks() {
        return gateMarks;
    }

    RecordingFighterColours getFighterColours() {
        return fighterColours;
    }

    SpectatorService getSpectatorService() {
        return spectatorService;
    }

    SpectatorBorderService getBorderService() {
        return borderService;
    }

    List<String> getRoundStartSteps() {
        return roundStartSteps;
    }

    List<World> getTemporaryEntityRemovalWorlds() {
        return temporaryEntityRemovalWorlds;
    }

    void failTemporaryEntityRemovalWith(RuntimeException failure) {
        this.temporaryEntityRemovalFailure = failure;
    }

    ArenaShape getShape() {
        return arenaRegistry.getArena().getShape().orElseThrow();
    }

    Location at(double x, double z) {
        return new Location(world, x, GROUND_Y, z);
    }

    FakePlayer addPlayer(String playerName) {
        FakePlayer player = new FakePlayer(playerName, at(WALL_MAXIMUM + 5.5D, WALL_MAXIMUM + 5.5D));
        playersByUniqueId.put(player.uniqueId, player);
        return player;
    }

    Match startMatch(FakePlayer firstFighter, FakePlayer firstMate, FakePlayer secondFighter, FakePlayer secondMate) {
        return startMatch(Match.regularMatch("M1", buildFirstTeam(firstFighter, firstMate),
                buildSecondTeam(secondFighter, secondMate), arenaRegistry.getArena()));
    }

    Match startFinalMatch(FakePlayer firstFighter, FakePlayer firstMate, FakePlayer secondFighter,
                          FakePlayer secondMate) {
        return startMatch(Match.finalMatch("M1", buildFirstTeam(firstFighter, firstMate),
                buildSecondTeam(secondFighter, secondMate), arenaRegistry.getArena()));
    }

    private Match startMatch(Match match) {
        matchService.startMatch(match);
        return match;
    }

    private Team buildFirstTeam(FakePlayer fighter, FakePlayer mate) {
        return new Team("Alpha", List.of(member(fighter), member(mate)));
    }

    private Team buildSecondTeam(FakePlayer fighter, FakePlayer mate) {
        return new Team("Beta", List.of(member(fighter), member(mate)));
    }

    void forgetTribuneSpawn() {
        arenaRegistry.getArena().setTribuneSpawn(null);
    }

    void runTick() {
        for (ScheduledTask task : new ArrayList<>(scheduledTasks)) {
            scheduledTasks.remove(task);
            if (!task.cancelled) {
                task.runnable.run();
            }
        }
    }

    void runRepeatingTasks() {
        for (ScheduledTask task : new ArrayList<>(repeatingTasks)) {
            if (task.cancelled) {
                repeatingTasks.remove(task);
            } else {
                task.runnable.run();
            }
        }
    }

    void advanceSeconds(long seconds) {
        advanceTicks(seconds * TICKS_PER_SECOND);
    }

    void advanceTicks(long tickCount) {
        for (long tick = 0; tick < tickCount; tick++) {
            currentTick++;
            for (ScheduledTask task : new ArrayList<>(scheduledTasks)) {
                if (task.dueTick <= currentTick) {
                    scheduledTasks.remove(task);
                    if (!task.cancelled) {
                        task.runnable.run();
                    }
                }
            }
        }
    }

    int getScheduledTaskCount() {
        return scheduledTasks.size();
    }

    int getPendingTaskCount() {
        return (int) scheduledTasks.stream().filter(task -> !task.cancelled).count();
    }

    private void buildWallAndShape() {
        for (int coordinate = WALL_MINIMUM; coordinate <= WALL_MAXIMUM; coordinate++) {
            for (int wallY = GROUND_Y; wallY <= WALL_TOP_Y; wallY++) {
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MINIMUM);
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MAXIMUM);
                borderMarks.mark(WORLD_NAME, WALL_MINIMUM, wallY, coordinate);
                borderMarks.mark(WORLD_NAME, WALL_MAXIMUM, wallY, coordinate);
            }
        }
        ArenaShapeCalculation calculation = ArenaShapeFloodFill.calculate(WORLD_NAME, 5, GROUND_Y, 5,
                ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, borderMarks.createColumnReader(WORLD_NAME));
        ArenaShape shape = calculation.getShape().orElseThrow();
        arenaRegistry.getArena().setShape(shape);
        for (int cellIndex = 0; cellIndex < shape.getCellCount(); cellIndex++) {
            long cell = shape.getCell(cellIndex);
            setBlock(HorizontalCellCodec.decodeX(cell), GROUND_Y, HorizontalCellCodec.decodeZ(cell),
                    Material.STONE);
        }
        arenaRegistry.getArena().setFirstTeamSpawn(new PositionSnapshot(WORLD_NAME, 2.5D, GROUND_Y, 2.5D, 0F, 0F));
        arenaRegistry.getArena().setSecondTeamSpawn(new PositionSnapshot(WORLD_NAME, 8.5D, GROUND_Y, 8.5D, 0F, 0F));
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME,
                WALL_MAXIMUM + 5.5D, GROUND_Y, WALL_MAXIMUM + 5.5D, 0F, 0F));
    }

    private TeamMember member(FakePlayer player) {
        return new TeamMember(player.uniqueId, player.playerName);
    }

    private void installBukkitServer() {
        SharedBukkitServer.register(world, this::findFakePlayer);
    }

    private Player findFakePlayer(UUID playerUniqueId) {
        FakePlayer player = playersByUniqueId.get(playerUniqueId);
        return player == null ? null : player.player;
    }

    private Plugin createPlugin(Server server) {
        return proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> LOGGER;
            case "getName", "namespace" -> "simplepvparena";
            case "isEnabled" -> true;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private Server createServer() {
        Object scheduler = createScheduler();
        return proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScheduler" -> scheduler;
            case "getWorld" -> WORLD_NAME.equals(arguments[0]) ? world : null;
            case "getPlayer" -> findPlayer(arguments[0]);
            case "getOnlinePlayers" -> findOnlinePlayers();
            case "broadcast" -> {
                broadcasts.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield findOnlinePlayers().size();
            }
            case "getLogger" -> LOGGER;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
    }

    private List<Player> findOnlinePlayers() {
        List<Player> onlinePlayers = new ArrayList<>();
        for (FakePlayer player : playersByUniqueId.values()) {
            if (player.online) {
                onlinePlayers.add(player.player);
            }
        }
        return onlinePlayers;
    }

    private Object findPlayer(Object playerUniqueId) {
        FakePlayer player = playersByUniqueId.get(playerUniqueId);
        return player == null ? null : player.player;
    }

    private Object createScheduler() {
        return proxy(BukkitScheduler.class, (method, arguments) -> switch (method.getName()) {
            case "runTaskTimer" -> scheduleRepeating((Runnable) arguments[1]);
            case "runTask" -> schedule(new ScheduledTask((Runnable) arguments[1], currentTick + 1L));
            case "runTaskLater" -> schedule(new ScheduledTask((Runnable) arguments[1],
                    currentTick + Math.max(1L, (Long) arguments[2])));
            default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
        });
    }

    private Object scheduleRepeating(Runnable runnable) {
        ScheduledTask task = new ScheduledTask(runnable, Long.MAX_VALUE);
        repeatingTasks.add(task);
        return schedule(task);
    }

    private Object schedule(ScheduledTask task) {
        scheduledTasks.add(task);
        return proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                task.cancelled = true;
                yield null;
            }
            case "isCancelled" -> task.cancelled;
            case "getTaskId" -> 1;
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
    }

    Block createBlock(int blockX, int blockY, int blockZ) {
        return proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getX" -> blockX;
            case "getY" -> blockY;
            case "getZ" -> blockZ;
            case "getWorld" -> world;
            case "getType" -> findMaterial(blockX, blockY, blockZ);
            case "getBlockData" -> FakeBlockData.of(findMaterial(blockX, blockY, blockZ));
            case "setType" -> {
                setBlock(blockX, blockY, blockZ, (Material) arguments[0]);
                yield null;
            }
            case "getLocation" -> new Location(world, blockX, blockY, blockZ);
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    Block createBlockInTheLobby(int blockX, int blockY, int blockZ) {
        World lobbyWorld = proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> LOBBY_WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        return proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getX" -> blockX;
            case "getY" -> blockY;
            case "getZ" -> blockZ;
            case "getWorld" -> lobbyWorld;
            case "getType" -> Material.STONE;
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    void setBlock(int blockX, int blockY, int blockZ, Material material) {
        blockMaterials.put(BlockPositionCodec.encode(blockX, blockY, blockZ), material);
    }

    Material findMaterial(int blockX, int blockY, int blockZ) {
        return blockMaterials.getOrDefault(BlockPositionCodec.encode(blockX, blockY, blockZ), Material.AIR);
    }

    static <T> T proxy(Class<T> type, FakeCall call) {
        InvocationHandler handler = (proxyInstance, method, arguments) -> switch (method.getName()) {
            case "equals" -> proxyInstance == arguments[0];
            case "hashCode" -> System.identityHashCode(proxyInstance);
            case "toString" -> "fake " + type.getSimpleName();
            default -> call.invoke(method, arguments);
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    @FunctionalInterface
    interface FakeCall {

        Object invoke(Method method, Object[] arguments);
    }

    private static final class ScheduledTask {

        private final Runnable runnable;
        private final long dueTick;

        private boolean cancelled;

        private ScheduledTask(Runnable runnable, long dueTick) {
            this.runnable = runnable;
            this.dueTick = dueTick;
        }
    }

    final class FakePlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final String playerName;
        private final Map<Object, Object> persistentData = new LinkedHashMap<>();
        private final List<String> messages = new ArrayList<>();
        private final List<String> actionBars = new ArrayList<>();
        private final List<Title> titles = new ArrayList<>();
        private final Player player;

        private Location location;
        private GameMode gameMode = GameMode.SURVIVAL;
        private boolean operator;
        private boolean adminPermissionGranted;
        private boolean online = true;
        private int inventoryClearCount;
        private ItemStack[] contents = new ItemStack[INVENTORY_SLOT_COUNT];
        private double health = 20.0;
        private int foodLevel = 20;
        private float saturation;
        private float exp;
        private int level;
        private RuntimeException inventoryClearFailure;
        private RuntimeException teleportFailure;
        private Runnable damageReaction = () -> { };

        private FakePlayer(String playerName, Location location) {
            this.playerName = playerName;
            this.location = location;
            this.player = proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getName" -> playerName;
                case "isOp" -> operator;
                case "isOnline" -> online;
                case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0])
                        && (operator || adminPermissionGranted);
                case "getLocation" -> this.location;
                case "getGameMode" -> gameMode;
                case "setGameMode" -> {
                    gameMode = (GameMode) arguments[0];
                    yield null;
                }
                case "teleport" -> {
                    if (teleportFailure != null) {
                        throw teleportFailure;
                    }
                    this.location = (Location) arguments[0];
                    roundStartSteps.add("teleport " + playerName);
                    yield true;
                }
                case "setHealth" -> {
                    health = (Double) arguments[0];
                    roundStartSteps.add("kit " + playerName);
                    yield null;
                }
                case "setFoodLevel" -> {
                    foodLevel = (Integer) arguments[0];
                    yield null;
                }
                case "setSaturation" -> {
                    saturation = (Float) arguments[0];
                    yield null;
                }
                case "setExp" -> {
                    exp = (Float) arguments[0];
                    yield null;
                }
                case "setLevel" -> {
                    level = (Integer) arguments[0];
                    yield null;
                }
                case "damage" -> {
                    damageReaction.run();
                    yield null;
                }
                case "sendMessage" -> {
                    recordMessage(arguments[0]);
                    yield null;
                }
                case "sendActionBar" -> {
                    actionBars.add(TournamentMessages.toPlainText((Component) arguments[0]));
                    yield null;
                }
                case "showTitle" -> {
                    titles.add((Title) arguments[0]);
                    yield null;
                }
                case "setFallDistance", "setFireTicks",
                     "removePotionEffect", "setAbsorptionAmount", "setArrowsInBody", "setFreezeTicks",
                     "setRemainingAir", "setVelocity", "setCooldown" -> null;
                case "getMaximumAir" -> 300;
                case "getCooldown" -> 0;
                case "getHealth" -> health;
                case "getFoodLevel" -> foodLevel;
                case "getSaturation" -> saturation;
                case "getExp" -> exp;
                case "getLevel" -> level;
                case "getActivePotionEffects" -> List.of();
                case "getAttribute" -> null;
                case "getInventory" -> inventory();
                case "getPersistentDataContainer" -> SharedBukkitServer.createPersistentDataContainer(persistentData);
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        }

        Player player() {
            return player;
        }

        String name() {
            return playerName;
        }

        List<String> messages() {
            return messages;
        }

        List<String> actionBars() {
            return actionBars;
        }

        List<Title> titles() {
            return titles;
        }

        List<String> titleTexts() {
            return titles.stream()
                    .map(title -> TournamentMessages.toPlainText(title.title()) + " / "
                            + TournamentMessages.toPlainText(title.subtitle()))
                    .toList();
        }

        void forgetReceivedTexts() {
            messages.clear();
            actionBars.clear();
            titles.clear();
        }

        private void recordMessage(Object message) {
            if (message instanceof Component component) {
                messages.add(TournamentMessages.toPlainBody(component));
            } else {
                messages.add(String.valueOf(message));
            }
        }

        UUID uniqueId() {
            return uniqueId;
        }

        Location location() {
            return location;
        }

        GameMode gameMode() {
            return gameMode;
        }

        void moveTo(Location destination) {
            this.location = destination;
        }

        void makeOperator() {
            this.operator = true;
        }

        void grantAdminPermission() {
            this.adminPermissionGranted = true;
        }

        void setOnline(boolean online) {
            this.online = online;
        }

        int inventoryClearCount() {
            return inventoryClearCount;
        }

        ItemStack[] contents() {
            return contents.clone();
        }

        void setContents(ItemStack[] contents) {
            this.contents = contents.clone();
        }

        int level() {
            return level;
        }

        void setLevel(int level) {
            this.level = level;
        }

        void failInventoryClearWith(RuntimeException failure) {
            this.inventoryClearFailure = failure;
        }

        void failTeleportWith(RuntimeException failure) {
            this.teleportFailure = failure;
        }

        void reactToDamageWith(Runnable damageReaction) {
            this.damageReaction = damageReaction;
        }

        private Object inventory() {
            return proxy(org.bukkit.inventory.PlayerInventory.class, (method, arguments) -> switch (method.getName()) {
                case "clear" -> {
                    if (inventoryClearFailure != null) {
                        throw inventoryClearFailure;
                    }
                    inventoryClearCount++;
                    contents = new ItemStack[INVENTORY_SLOT_COUNT];
                    yield null;
                }
                case "getContents" -> contents.clone();
                case "setContents" -> {
                    contents = ((ItemStack[]) arguments[0]).clone();
                    yield null;
                }
                case "setItem" -> null;
                default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
            });
        }
    }
}
