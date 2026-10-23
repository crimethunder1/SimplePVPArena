package de.simplepvparena.placement;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.support.InMemoryScoreboard;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class SidebarFixture {

    static final String WORLD_NAME = "arena";
    static final int EFFECT_MARGIN = 10;

    private final World world = SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
        case "getName" -> WORLD_NAME;
        default -> throw new UnsupportedOperationException("World." + method.getName());
    });
    private final InMemoryScoreboard mainScoreboard = new InMemoryScoreboard();
    private final List<InMemoryScoreboard> newScoreboards = new ArrayList<>();
    private final Map<UUID, FakePlayer> players = new LinkedHashMap<>();
    private final List<Runnable> repeatingTasks = new ArrayList<>();
    private final List<Long> repeatingPeriods = new ArrayList<>();
    private final Server server;
    private final Plugin plugin;

    private PlacementStandings standings = PlacementStandings.none();
    private boolean scoreboardManagerReady = true;
    private boolean taskCancelled;

    SidebarFixture() {
        SharedBukkitServer.install();
        ScoreboardManager scoreboardManager = SharedBukkitServer.proxy(ScoreboardManager.class,
                (method, arguments) -> switch (method.getName()) {
                    case "getMainScoreboard" -> mainScoreboard.scoreboard();
                    case "getNewScoreboard" -> {
                        InMemoryScoreboard newScoreboard = new InMemoryScoreboard();
                        newScoreboards.add(newScoreboard);
                        yield newScoreboard.scoreboard();
                    }
                    default -> throw new UnsupportedOperationException("ScoreboardManager." + method.getName());
                });
        BukkitTask task = SharedBukkitServer.proxy(BukkitTask.class, (method, arguments) -> switch (method.getName()) {
            case "cancel" -> {
                taskCancelled = true;
                yield null;
            }
            default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
        });
        BukkitScheduler scheduler = SharedBukkitServer.proxy(BukkitScheduler.class,
                (method, arguments) -> switch (method.getName()) {
                    case "runTaskTimer" -> {
                        repeatingTasks.add((Runnable) arguments[1]);
                        repeatingPeriods.add((Long) arguments[3]);
                        yield task;
                    }
                    default -> throw new UnsupportedOperationException("BukkitScheduler." + method.getName());
                });
        server = SharedBukkitServer.proxy(Server.class, (method, arguments) -> switch (method.getName()) {
            case "getScoreboardManager" -> scoreboardManagerReady ? scoreboardManager : null;
            case "getOnlinePlayers" -> players.values().stream().filter(player -> player.online)
                    .map(player -> player.proxy).toList();
            case "getScheduler" -> scheduler;
            default -> throw new UnsupportedOperationException("Server." + method.getName());
        });
        plugin = SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
            case "getServer" -> server;
            default -> throw new UnsupportedOperationException("Plugin." + method.getName());
        });
    }

    PlacementSidebar createSidebar() {
        return new PlacementSidebar(plugin, () -> Optional.of(effectArea()), () -> standings);
    }

    Server server() {
        return server;
    }

    Plugin plugin() {
        return plugin;
    }

    void showStandings(PlacementStandings standings) {
        this.standings = standings;
    }

    void withholdTheScoreboardManager() {
        scoreboardManagerReady = false;
    }

    InMemoryScoreboard mainScoreboard() {
        return mainScoreboard;
    }

    List<InMemoryScoreboard> newScoreboards() {
        return newScoreboards;
    }

    List<Runnable> repeatingTasks() {
        return repeatingTasks;
    }

    List<Long> repeatingPeriods() {
        return repeatingPeriods;
    }

    boolean isTaskCancelled() {
        return taskCancelled;
    }

    Location inside() {
        return new Location(world, 2.5D, 64.0D, 2.5D);
    }

    Location outside() {
        return new Location(world, 300.5D, 64.0D, 300.5D);
    }

    FakePlayer addPlayer(String playerName, Location location) {
        FakePlayer player = new FakePlayer(playerName, location);
        players.put(player.uniqueId, player);
        return player;
    }

    InMemoryScoreboard scoreboardOf(FakePlayer player) {
        return newScoreboards.stream()
                .filter(newScoreboard -> newScoreboard.scoreboard() == player.scoreboard)
                .findFirst()
                .orElseThrow(() -> new AssertionError(player.name + " has no own scoreboard"));
    }

    private static EffectArea effectArea() {
        List<Long> cells = new ArrayList<>();
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                cells.add(HorizontalCellCodec.encode(x, z));
            }
        }
        long[] cellArray = cells.stream().mapToLong(Long::longValue).toArray();
        return EffectArea.around(new ArenaShape(WORLD_NAME, 64, 70, cellArray), EFFECT_MARGIN);
    }

    final class FakePlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final String name;
        private final Player proxy;

        private Location location;
        private Scoreboard scoreboard = mainScoreboard.scoreboard();
        private boolean online = true;
        private int scoreboardChangeCount;

        private FakePlayer(String name, Location location) {
            this.name = name;
            this.location = location;
            this.proxy = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "getName" -> name;
                case "getLocation" -> this.location;
                case "isOnline" -> online;
                case "getScoreboard" -> scoreboard;
                case "setScoreboard" -> {
                    scoreboard = (Scoreboard) arguments[0];
                    scoreboardChangeCount++;
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        }

        Player player() {
            return proxy;
        }

        void moveTo(Location destination) {
            location = destination;
        }

        Location location() {
            return location;
        }

        Scoreboard scoreboard() {
            return scoreboard;
        }

        void useScoreboard(Scoreboard otherScoreboard) {
            scoreboard = otherScoreboard;
        }

        void goOffline() {
            online = false;
        }

        int scoreboardChangeCount() {
            return scoreboardChangeCount;
        }
    }
}
