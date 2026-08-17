package de.simplepvparena.tournament;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorItemFactory;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.gate.StartGates;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.FighterColours;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningBetweenMatchesTest {

    private static final String CLEAN_OUTCOME = "The arena is clean\n4 of 27 checked blocks were restored";

    @Test
    void startsTheNextMatchOnlyAfterTheAutomaticCleaningIsDone() {
        Fixture fixture = new Fixture();
        fixture.startTournament();

        assertEquals("round1-match1", fixture.runningMatchId());
        fixture.decideRunningMatch();

        assertEquals(1, fixture.cleanup.attemptCount,
                "the end of a match asks for a cleaning by itself, nobody clicks a tile for it");
        assertTrue(fixture.cleanup.isCleaning(), "and that cleaning is running");
        assertFalse(fixture.matchService.hasActiveMatch(),
                "no match may be put onto an arena that is still being written");

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        assertEquals("round1-match2", fixture.runningMatchId(),
                "the next match of the bracket follows the cleaning");
        assertTrue(fixture.logMessages.stream().anyMatch(message -> message.contains("The arena is clean")),
                "the outcome of a cleaning nobody asked for goes into the log: " + fixture.logMessages);
    }

    @Test
    void recordsTheWinnerWhileTheArenaIsStillBeingCleaned() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        String winnerTeamName = fixture.runningMatch().getFirstTeam().getTeamName();

        fixture.decideRunningMatch();

        assertTrue(fixture.cleanup.isCleaning());
        assertEquals(Optional.of(winnerTeamName),
                fixture.tournamentManager.getBracket().orElseThrow().findMatch("round1-match1")
                        .orElseThrow().getWinnerTeamName());
    }

    @Test
    void carriesOnWithTheNextMatchWhenNoCleaningCanBeStarted() {
        Fixture fixture = new Fixture();
        fixture.cleanup.skipWith("no baseline of the fight area is stored");
        fixture.startTournament();

        fixture.decideRunningMatch();

        assertEquals(1, fixture.cleanup.attemptCount, "the cleaning was asked for");
        assertFalse(fixture.cleanup.isCleaning(), "and it refused to run");
        assertEquals("round1-match2", fixture.runningMatchId(),
                "a missing baseline may never hold the tournament back");
        assertTrue(fixture.logMessages.stream().anyMatch(
                        message -> message.contains("not cleaned automatically")
                                && message.contains("no baseline of the fight area is stored")),
                "the skipped cleaning is written into the log: " + fixture.logMessages);
    }

    @Test
    void carriesOnWhenTheCleaningIsGivenUpOnHalfway() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        fixture.decideRunningMatch();

        fixture.cleanup.finishCleaning("The arena cleaning was stopped: the chunk is no longer loaded");

        assertEquals("round1-match2", fixture.runningMatchId());
        assertTrue(fixture.logMessages.stream().anyMatch(
                        message -> message.contains("the chunk is no longer loaded")),
                "the failure is written into the log: " + fixture.logMessages);
    }

    @Test
    void refusesToStartTheFinalWhileTheArenaIsBeingCleaned() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        fixture.decideRunningMatch();
        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);
        fixture.decideRunningMatch();

        assertEquals(FinalMatchStage.READY, fixture.tournamentManager.getFinalMatchStage());
        TournamentOperationResult refusal = fixture.tournamentManager.startFinalMatch();

        assertFalse(refusal.successful(), "the final may not start into an arena that is being cleaned");
        assertTrue(refusal.message().contains("still being cleaned"), refusal.message());
        assertFalse(fixture.matchService.hasActiveMatch());

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        assertTrue(fixture.tournamentManager.startFinalMatch().successful());
        assertEquals("round2-match1", fixture.runningMatchId());
    }

    private static final class Fixture {

        private static final String WORLD_NAME = "tournament-flow";
        private static final int WALL_MINIMUM = 0;
        private static final int WALL_MAXIMUM = 6;
        private static final int GROUND_Y = 64;
        private static final int WALL_TOP_Y = GROUND_Y + 3;

        private final List<Object> scheduledTasks = new ArrayList<>();
        private final List<String> logMessages = new ArrayList<>();
        private final TeamRegistry teamRegistry = new TeamRegistry();
        private final ArenaRegistry arenaRegistry = new ArenaRegistry();
        private final RecordingArenaCleanup cleanup = new RecordingArenaCleanup();
        private final Plugin plugin;
        private final MatchService matchService;
        private final TournamentManager tournamentManager;

        private Fixture() {
            SharedBukkitServer.register(createWorld(), playerUniqueId -> null);
            this.plugin = createPlugin();
            this.matchService = new MatchService(plugin, new KitService(),
                    new SpectatorService(arenaRegistry, new SpectatorItemFactory(plugin)),
                    new SilentFighterColours(), StartGates.NONE);
            this.tournamentManager = new TournamentManager(plugin, teamRegistry, arenaRegistry,
                    matchService, cleanup, message -> { });
            buildArena();
            registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        }

        private void startTournament() {
            assertTrue(tournamentManager.startTournament(false).successful());
        }

        private void decideRunningMatch() {
            Match match = runningMatch();
            assertTrue(tournamentManager.forceMatchResult(match.getMatchId(),
                    match.getFirstTeam().getTeamName()).successful());
        }

        private Match runningMatch() {
            return matchService.getActiveMatch().orElseThrow();
        }

        private String runningMatchId() {
            return runningMatch().getMatchId();
        }

        private void buildArena() {
            BorderMarkIndex borderMarks = new BorderMarkIndex();
            for (int coordinate = WALL_MINIMUM; coordinate <= WALL_MAXIMUM; coordinate++) {
                for (int wallY = GROUND_Y; wallY <= WALL_TOP_Y; wallY++) {
                    borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MINIMUM);
                    borderMarks.mark(WORLD_NAME, coordinate, wallY, WALL_MAXIMUM);
                    borderMarks.mark(WORLD_NAME, WALL_MINIMUM, wallY, coordinate);
                    borderMarks.mark(WORLD_NAME, WALL_MAXIMUM, wallY, coordinate);
                }
            }
            ArenaShape shape = ArenaShapeFloodFill.calculate(WORLD_NAME, 3, GROUND_Y, 3,
                            ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT,
                            borderMarks.createColumnReader(WORLD_NAME))
                    .getShape().orElseThrow();
            arenaRegistry.getArena().setShape(shape);
            arenaRegistry.getArena().setTribuneSpawn(new PositionSnapshot(WORLD_NAME,
                    WALL_MAXIMUM + 5.5D, GROUND_Y, WALL_MAXIMUM + 5.5D, 0F, 0F));
        }

        private void registerTeams(String... teamNames) {
            for (String teamName : teamNames) {
                teamRegistry.registerTeam(teamName,
                        new TeamMember(UUID.randomUUID(), teamName + "-first"),
                        new TeamMember(UUID.randomUUID(), teamName + "-second"));
            }
        }

        private World createWorld() {
            return SharedBukkitServer.proxy(World.class, (method, arguments) -> switch (method.getName()) {
                case "getName" -> WORLD_NAME;
                default -> throw new UnsupportedOperationException("World." + method.getName());
            });
        }

        private Plugin createPlugin() {
            Object server = createServer();
            Logger logger = createRecordingLogger();
            return SharedBukkitServer.proxy(Plugin.class, (method, arguments) -> switch (method.getName()) {
                case "getServer" -> server;
                case "getLogger" -> logger;
                case "getName", "namespace" -> "simplepvparena";
                default -> throw new UnsupportedOperationException("Plugin." + method.getName());
            });
        }

        private Object createServer() {
            Object scheduler = createScheduler();
            return SharedBukkitServer.proxy(org.bukkit.Server.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "getScheduler" -> scheduler;
                        case "getPlayer" -> null;
                        case "broadcast" -> 0;
                        default -> throw new UnsupportedOperationException("Server." + method.getName());
                    });
        }

        private Object createScheduler() {
            return SharedBukkitServer.proxy(BukkitScheduler.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "runTask", "runTaskLater", "runTaskTimer" -> schedule(arguments[1]);
                        default -> throw new UnsupportedOperationException(
                                "BukkitScheduler." + method.getName());
                    });
        }

        private Object schedule(Object runnable) {
            scheduledTasks.add(runnable);
            return SharedBukkitServer.proxy(BukkitTask.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "cancel" -> {
                            scheduledTasks.remove(runnable);
                            yield null;
                        }
                        case "isCancelled" -> !scheduledTasks.contains(runnable);
                        case "getTaskId" -> 1;
                        default -> throw new UnsupportedOperationException("BukkitTask." + method.getName());
                    });
        }

        private Logger createRecordingLogger() {
            Logger logger = Logger.getLogger("ArenaCleaningBetweenMatchesTest." + UUID.randomUUID());
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            logger.addHandler(new Handler() {

                @Override
                public void publish(LogRecord logRecord) {
                    logMessages.add(logRecord.getMessage());
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
            return logger;
        }
    }

    private static final class RecordingArenaCleanup implements ArenaCleanup {

        private int attemptCount;
        private String skipReason;
        private Consumer<String> completionHandler;

        @Override
        public Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler) {
            attemptCount++;
            if (skipReason != null) {
                return Optional.of(skipReason);
            }
            this.completionHandler = completionHandler;
            return Optional.empty();
        }

        private void skipWith(String reason) {
            this.skipReason = reason;
        }

        private boolean isCleaning() {
            return completionHandler != null;
        }

        private void finishCleaning(String outcome) {
            Consumer<String> runningHandler = completionHandler;
            assertTrue(runningHandler != null, "no cleaning is running");
            completionHandler = null;
            runningHandler.accept(outcome);
        }
    }

    private static final class SilentFighterColours implements FighterColours {

        @Override
        public void applySettings(TeamColourSettings settings) {
        }

        @Override
        public void colourFighters(Match match) {
        }

        @Override
        public void clearColours() {
        }
    }
}
