package de.simplepvparena.gui;

import de.simplepvparena.arena.ActiveArenaState;
import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBorderConfirmation;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaGapGuide;
import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.arena.ArenaHeightMarkers;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.BorderVisualEffect;
import de.simplepvparena.border.BorderWallPreview;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.config.ConfigurationReloadReport;
import de.simplepvparena.diagnostics.PluginDryRunArena;
import de.simplepvparena.diagnostics.TournamentDryRun;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateItemService;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.input.ChatInputService;
import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.kit.KitSaveService;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.EventTeamImportFile;
import de.simplepvparena.team.EventTeamImportService;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamMemberLookup;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.tournament.AutomaticArenaCleanup;
import de.simplepvparena.tournament.TournamentManager;
import de.simplepvparena.tournament.TournamentOperationResult;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdminMenuFixture {

    private static final String WORLD_NAME = "admin-menu";
    private static final int GROUND_Y = 64;
    private static final double INSIDE_COORDINATE = 3.5D;
    private static final double OUTSIDE_COORDINATE = 11.5D;

    private static final int WALL_MINIMUM = 0;
    private static final int WALL_MAXIMUM = 6;
    private static final int WALL_TOP_Y = GROUND_Y + 3;
    private static final int SEED_COORDINATE = 3;

    private static final Logger LOGGER = createQuietLogger();

    private final RecordingAdminMenuWindows windows = new RecordingAdminMenuWindows();
    private final List<String> messages = new ArrayList<>();
    private final UUID adminUniqueId = UUID.randomUUID();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final ArenaRegistry arenaRegistry = new ArenaRegistry();
    private final BorderMarkIndex borderMarks = new BorderMarkIndex();
    private final ArenaHeightMarkers heightMarkers = new ArenaHeightMarkers();
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final StartGateDuration gateDuration = new StartGateDuration();
    private final BorderSelectorModeRegistry borderSelectorModes = new BorderSelectorModeRegistry();
    private final KitService kitService = new KitService();
    private final List<List<String>> reloadedConfigurations = new ArrayList<>();
    private final World world;
    private final Plugin plugin;
    private final MatchService matchService;
    private final ArenaSetupService arenaSetupService;
    private final TournamentManager tournamentManager;
    private final ChatInputService chatInputService;
    private final AdminMenuService menuService;
    private final Player admin;

    private boolean adminPermissionGranted = true;
    private Location adminLocation;

    AdminMenuFixture() {
        SharedBukkitServer.install();
        world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
            case "getName" -> WORLD_NAME;
            default -> throw new UnsupportedOperationException("World." + method.getName());
        });
        plugin = createPlugin(createServer());
        adminLocation = new Location(world, INSIDE_COORDINATE, GROUND_Y, INSIDE_COORDINATE);
        matchService = new MatchService(plugin, kitService,
                new SpectatorService(arenaRegistry, new SpectatorItemFactory(plugin)),
                SharedBukkitServer.proxy(FighterColours.class, (method, arguments) -> null), StartGates.NONE);
        ArenaCleaningService cleaningService = new ArenaCleaningService(plugin, (cleanedWorld, shape) -> 0);
        arenaSetupService = new ArenaSetupService(
                new ActiveArenaState(arenaRegistry, heightMarkers), matchService,
                new SpectatorBorderService(plugin, borderMarks, () -> { }),
                new BorderWallPreview(plugin, SharedBukkitServer.proxy(BorderVisualEffect.class,
                        (method, arguments) -> null)),
                new ArenaGapGuide(plugin), cleaningService,
                new ArenaBaselineService(plugin, new ArenaBaselineRegistry()),
                new ArenaBorderConfirmation(arenaRegistry.getArena()::hasShape), () -> { });
        tournamentManager = new TournamentManager(plugin, teamRegistry, arenaRegistry, matchService,
                new AutomaticArenaCleanup(arenaSetupService, cleaningService), message -> { });
        TournamentDryRun tournamentDryRun = new TournamentDryRun(tournamentManager::getState,
                matchService::hasActiveMatch, teamRegistry,
                new PluginDryRunArena(arenaRegistry.getArena(), arenaSetupService), kitService, gateMarks,
                gateDuration);
        chatInputService = new ChatInputService(plugin);
        admin = createAdmin();
        Server server = plugin.getServer();
        menuService = new AdminMenuService(plugin, tournamentManager, tournamentDryRun, teamRegistry,
                new TeamMemberLookup(server),
                new EventTeamImportService(new File("target/admin-menu-fixture/" + EventTeamImportFile.FILE_NAME),
                        teamRegistry, playerName -> Optional.empty(), () -> { }),
                matchService, arenaSetupService, new ArenaHeightMarkerService(plugin, heightMarkers),
                new StartGateItemService(plugin, gateMarks, gateDuration, () -> { }),
                borderSelectorModes,
                new KitSaveService(kitService, serializedKit -> { }, problem -> { }),
                chatInputService, () -> { },
                this::reloadConfiguration,
                windows);
    }

    void registerTeams(String... teamNames) {
        for (String teamName : teamNames) {
            teamRegistry.registerTeam(teamName,
                    new TeamMember(UUID.randomUUID(), teamName + "-first"),
                    new TeamMember(UUID.randomUUID(), teamName + "-second"));
        }
    }

    void configureArena() {
        storeTheFightArea();
        arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME,
                OUTSIDE_COORDINATE, GROUND_Y, OUTSIDE_COORDINATE, 0F, 0F));
    }

    void storeTheFightArea() {
        for (int coordinate = WALL_MINIMUM; coordinate <= WALL_MAXIMUM; coordinate++) {
            for (int wallY = GROUND_Y; wallY <= WALL_TOP_Y; wallY++) {
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MINIMUM);
                borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MAXIMUM);
                borderMarks.mark(WORLD_NAME, WALL_MINIMUM, wallY, coordinate);
                borderMarks.mark(WORLD_NAME, WALL_MAXIMUM, wallY, coordinate);
            }
        }
        ArenaShape shape = ArenaShapeFloodFill.calculate(WORLD_NAME, SEED_COORDINATE, GROUND_Y, SEED_COORDINATE,
                        ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, borderMarks.createColumnReader(WORLD_NAME))
                .getShape().orElseThrow();
        arenaRegistry.getArena().setShape(shape);
    }

    void startTournament() {
        assertSuccessful(tournamentManager.startTournament(false));
    }

    void pauseTournament() {
        assertSuccessful(tournamentManager.togglePause());
    }

    void startFinal() {
        assertSuccessful(tournamentManager.startFinalMatch());
    }

    void decideRunningMatchFor(String teamName) {
        assertSuccessful(tournamentManager.forceMatchResult(runningMatch().getMatchId(), teamName));
    }

    void startMatchWithoutATournament(String firstTeamName, String secondTeamName) {
        matchService.startMatch(Match.regularMatch("M1", teamRegistry.findTeam(firstTeamName).orElseThrow(),
                teamRegistry.findTeam(secondTeamName).orElseThrow(), arenaRegistry.getArena()));
    }

    Match runningMatch() {
        return matchService.getActiveMatch().orElseThrow();
    }

    String runningFirstTeamName() {
        return runningMatch().getFirstTeam().getTeamName();
    }

    String runningSecondTeamName() {
        return runningMatch().getSecondTeam().getTeamName();
    }

    void fillKit() {
        kitService.setKitDefinition(new KitDefinition(
                Map.of(KitEquipmentSlot.HEAD, new FakeItemStack("helmet"),
                        KitEquipmentSlot.CHEST, new FakeItemStack("chestplate")),
                Map.of(0, new FakeItemStack("sword"), 1, new FakeItemStack("bow"), 2,
                        new FakeItemStack("arrows"))));
    }

    List<String> describeKit() {
        return kitService.getKitDefinition().describe();
    }

    void switchBorderSelectorModeOn() {
        assertTrue(borderSelectorModes.toggle(adminUniqueId));
    }

    void standInsideTheFightArea() {
        adminLocation = new Location(world, INSIDE_COORDINATE, GROUND_Y, INSIDE_COORDINATE);
    }

    void standOutsideTheFightArea() {
        adminLocation = new Location(world, OUTSIDE_COORDINATE, GROUND_Y, OUTSIDE_COORDINATE);
    }

    void revokeAdminPermission() {
        adminPermissionGranted = false;
    }

    void openMainMenu() {
        menuService.openMainMenu(admin);
    }

    void openMenuFromTheMainMenu(int mainMenuSlot) {
        openMainMenu();
        click(mainMenuSlot);
        messages.clear();
    }

    void click(int slot) {
        menuService.handleClick(admin, lastMenu().holder(), slot);
    }

    RecordingAdminMenuWindows.RecordedMenu lastMenu() {
        return windows.lastOpenedMenu(admin);
    }

    boolean isMenuClosed() {
        return windows.isMenuClosed(admin);
    }

    String describeLastMenu() {
        RecordingAdminMenuWindows.RecordedMenu menu = lastMenu();
        StringBuilder description = new StringBuilder(menu.title()).append(" | ").append(menu.size()).append('\n');
        for (int slot = 0; slot < menu.size(); slot++) {
            int describedSlot = slot;
            menu.findTile(slot).ifPresent(tile -> description.append(describeTile(describedSlot, tile))
                    .append('\n'));
        }
        return description.toString();
    }

    private static String describeTile(int slot, MenuTile tile) {
        String line = slot + " | " + tile.material() + " | " + tile.displayName();
        if (tile.loreLines().isEmpty()) {
            return line;
        }
        return line + " | " + String.join(" / ", tile.loreLines());
    }

    List<String> messages() {
        return messages;
    }

    List<List<String>> reloadedConfigurations() {
        return reloadedConfigurations;
    }

    Player admin() {
        return admin;
    }

    TournamentManager tournamentManager() {
        return tournamentManager;
    }

    MatchService matchService() {
        return matchService;
    }

    ArenaRegistry arenaRegistry() {
        return arenaRegistry;
    }

    TeamRegistry teamRegistry() {
        return teamRegistry;
    }

    ChatInputService chatInputService() {
        return chatInputService;
    }

    boolean isBorderSelectorModeOn() {
        return borderSelectorModes.isActive(adminUniqueId);
    }

    private ConfigurationReloadReport reloadConfiguration() {
        ConfigurationReloadReport report = ConfigurationReloadReport.of(List.of());
        reloadedConfigurations.add(report.describe());
        return report;
    }

    private static void assertSuccessful(TournamentOperationResult result) {
        assertTrue(result.successful(), result.message());
    }

    private Player createAdmin() {
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> adminUniqueId;
            case "getName" -> "admin";
            case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0])
                    && adminPermissionGranted;
            case "getLocation" -> adminLocation.clone();
            case "sendMessage" -> {
                messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private Plugin createPlugin(Server server) {
        return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            case "getLogger" -> LOGGER;
            case "getName", "namespace" -> "simplepvparena";
            case "isEnabled" -> true;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    private Server createServer() {
        BukkitScheduler scheduler = SharedBukkitServer.proxy(BukkitScheduler.class,
                (method, arguments) -> switch (method.getName()) {
                    case "runTask", "runTaskLater", "runTaskTimer" -> schedule();
                    default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
                });
        return SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScheduler" -> scheduler;
            case "getWorld" -> WORLD_NAME.equals(arguments[0]) ? world : null;
            case "getPlayer" -> null;
            case "getLogger" -> LOGGER;
            case "broadcast" -> 0;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
    }

    private static BukkitTask schedule() {
        boolean[] cancelled = new boolean[1];
        return SharedBukkitServer.proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                cancelled[0] = true;
                yield null;
            }
            case "isCancelled" -> cancelled[0];
            case "getTaskId" -> 1;
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
    }

    private static Logger createQuietLogger() {
        Logger logger = Logger.getLogger("AdminMenuFixture");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
