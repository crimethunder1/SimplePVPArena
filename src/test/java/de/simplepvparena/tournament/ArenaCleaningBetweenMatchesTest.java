package de.simplepvparena.tournament;

import de.simplepvparena.arena.Arena;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningBetweenMatchesTest {

    private static final String CLEAN_OUTCOME = "The arena is clean\n4 of 27 checked blocks were restored";

    private static final String EXPECTED_CLEANING_REFUSAL = "The arena is still being cleaned. Try again in a moment.";

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
    void warnsTheAdminsOnceWhenTheCleaningAfterAMatchIsSkipped() {
        Fixture fixture = new Fixture();
        fixture.cleanup.skipWith("no baseline of the fight area is stored"
                + "\nRun the calculation once, that takes one");
        fixture.startTournament();

        fixture.decideRunningMatch();

        List<String> skippedCleaningWarnings = fixture.adminMessages.stream()
                .filter(message -> message.contains("not cleaned"))
                .toList();
        assertEquals(1, skippedCleaningWarnings.size(), "exactly one warning: " + fixture.adminMessages);
        String warning = skippedCleaningWarnings.get(0);
        assertTrue(warning.contains("round1-match1"), warning);
        assertTrue(warning.contains("no baseline of the fight area is stored"), warning);
        assertFalse(warning.contains("Run the calculation once"), "only the first line of the reason: " + warning);
        assertFalse(warning.contains("\n"), warning);
        assertTrue(warning.contains("as it is now"), warning);
        assertEquals("round1-match2", fixture.runningMatchId(), "the warning never holds the tournament back");
    }

    @Test
    void keepsTheWordingOfTheSkippedCleaningWarningForAMatchBeforeTheFinal() {
        Fixture fixture = new Fixture();
        fixture.cleanup.skipWith("no baseline of the fight area is stored");
        fixture.startTournament();

        fixture.decideRunningMatch();

        assertEquals(List.of("The arena was not cleaned after match round1-match1: no baseline of the fight area "
                        + "is stored. The next match starts in the arena as it is now"),
                fixture.adminMessages.stream().filter(message -> message.contains("not cleaned")).toList());
        assertTrue(fixture.logMessages.stream().anyMatch(
                        message -> message.contains("not cleaned automatically after this match")
                                && message.contains("The tournament carries on")),
                fixture.logMessages.toString());
    }

    @Test
    void asksForANewCalculationWhenTheCleaningAfterTheFinalIsSkipped() {
        Fixture fixture = new Fixture();
        fixture.cleanup.skipWith("no baseline of the fight area is stored"
                + "\nRun the calculation once, that takes one");
        fixture.startTournament();
        fixture.decideRunningMatch();
        fixture.decideRunningMatch();
        assertTrue(fixture.tournamentManager.startFinalMatch().successful());
        assertEquals("round2-match1", fixture.runningMatchId());
        fixture.adminMessages.clear();
        fixture.logMessages.clear();

        fixture.decideRunningMatch();

        assertEquals(TournamentState.FINISHED, fixture.tournamentManager.getState());
        List<String> skippedCleaningWarnings = fixture.adminMessages.stream()
                .filter(message -> message.contains("not cleaned"))
                .toList();
        assertEquals(1, skippedCleaningWarnings.size(), "exactly one warning: " + fixture.adminMessages);
        String warning = skippedCleaningWarnings.get(0);
        assertEquals("The arena was not cleaned after match round2-match1: no baseline of the fight area is "
                + "stored. Run " + Arena.CALCULATE_ACTION + " again before the next tournament", warning);
        assertFalse(warning.contains("The next match starts"), warning);
        assertTrue(fixture.logMessages.contains("The arena is not cleaned automatically after the final: "
                        + "no baseline of the fight area is stored Run the calculation once, that takes one"),
                fixture.logMessages.toString());
        assertFalse(fixture.logMessages.stream().anyMatch(message -> message.contains("carries on")),
                "no match follows the final: " + fixture.logMessages);
    }

    @Test
    void sendsNoSkippedCleaningWarningWhileTheCleaningRuns() {
        Fixture fixture = new Fixture();
        fixture.startTournament();

        fixture.decideRunningMatch();
        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        assertEquals(List.of(), fixture.adminMessages);
        assertEquals("round1-match2", fixture.runningMatchId());
    }

    @Test
    void warnsTheAdminsOnceWhenATournamentStartsWithoutABaseline() {
        Fixture fixture = new Fixture();
        fixture.cleanup.forgetBaseline();

        TournamentOperationResult start = fixture.tournamentManager.startTournament(false);

        assertTrue(start.successful(), start.message());
        assertEquals("Tournament started", start.message());
        assertEquals(List.of(TournamentManager.NO_BASELINE_WARNING), fixture.adminMessages);
        assertTrue(fixture.adminMessages.get(0).contains(Arena.CALCULATE_ACTION));
        assertEquals("round1-match1", fixture.runningMatchId());
    }

    @Test
    void sendsNoBaselineWarningWhenATournamentStartsWithABaseline() {
        Fixture fixture = new Fixture();

        TournamentOperationResult start = fixture.tournamentManager.startTournament(false);

        assertTrue(start.successful(), start.message());
        assertEquals(List.of(), fixture.adminMessages);
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

    @Test
    void refusesToStartATournamentWhileTheArenaIsBeingCleanedAndKeepsTheStateAsItIs() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        fixture.playUntilTheTournamentIsOver();
        assertTrue(fixture.cleanup.isCleaning(), "the end of the final starts a cleaning");
        Bracket finishedBracket = fixture.tournamentManager.getBracket().orElseThrow();
        FinalMatchStage finishedFinalStage = fixture.tournamentManager.getFinalMatchStage();
        int attemptCountBeforeTheStart = fixture.cleanup.attemptCount;

        TournamentOperationResult refusal = fixture.tournamentManager.startTournament(false);

        assertFalse(refusal.successful(), "no tournament may start into an arena that is being cleaned");
        assertEquals(EXPECTED_CLEANING_REFUSAL, refusal.message());
        assertEquals(TournamentState.FINISHED, fixture.tournamentManager.getState());
        assertSame(finishedBracket, fixture.tournamentManager.getBracket().orElseThrow(),
                "the refused start may not build a new bracket");
        assertEquals(finishedFinalStage, fixture.tournamentManager.getFinalMatchStage());
        assertFalse(fixture.matchService.hasActiveMatch());
        assertTrue(fixture.cleanup.isCleaning(), "the running cleaning is left alone");
        assertEquals(attemptCountBeforeTheStart, fixture.cleanup.attemptCount);
    }

    @Test
    void refusesToStartATournamentWhileACleaningNotStartedByATournamentRuns() {
        Fixture fixture = new Fixture();
        fixture.cleanup.startCleaningFromTheArenaMenu();

        TournamentOperationResult refusal = fixture.tournamentManager.startTournament(false);

        assertFalse(refusal.successful(), "every cleaning counts, not only the one of a tournament");
        assertEquals(EXPECTED_CLEANING_REFUSAL, refusal.message());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager.getState());
        assertTrue(fixture.tournamentManager.getBracket().isEmpty());
        assertFalse(fixture.matchService.hasActiveMatch());
    }

    @Test
    void startsTheTournamentOnceTheCleaningIsDone() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        fixture.playUntilTheTournamentIsOver();
        assertFalse(fixture.tournamentManager.startTournament(false).successful());

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        TournamentOperationResult start = fixture.tournamentManager.startTournament(false);
        assertTrue(start.successful(), start.message());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager.getState());
        assertEquals("round1-match1", fixture.runningMatchId());
    }

    @Test
    void refusesTheStartAfterACancelDuringACleaningUntilTheCleaningIsDone() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        fixture.decideRunningMatch();
        assertTrue(fixture.cleanup.isCleaning());

        assertTrue(fixture.tournamentManager.cancelTournament().successful());
        assertEquals(TournamentState.CANCELLED, fixture.tournamentManager.getState());
        assertTrue(fixture.tournamentManager.getBracket().isEmpty());

        TournamentOperationResult refusal = fixture.tournamentManager.startTournament(false);
        assertFalse(refusal.successful());
        assertEquals(EXPECTED_CLEANING_REFUSAL, refusal.message());
        assertEquals(TournamentState.CANCELLED, fixture.tournamentManager.getState());
        assertTrue(fixture.tournamentManager.getBracket().isEmpty());

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        assertFalse(fixture.matchService.hasActiveMatch(),
                "the cleaning of the cancelled tournament may not start a match");
        assertEquals(TournamentState.CANCELLED, fixture.tournamentManager.getState());

        TournamentOperationResult start = fixture.tournamentManager.startTournament(false);
        assertTrue(start.successful(), start.message());
        assertEquals("round1-match1", fixture.runningMatchId());
        assertTrue(fixture.tournamentManager.getBracket().orElseThrow().findMatch("round1-match1")
                .orElseThrow().getWinnerTeamName().isEmpty(), "the new tournament starts with an open bracket");
    }

    @Test
    void startsATournamentWithoutARunningCleaningAsBefore() {
        Fixture fixture = new Fixture();

        TournamentOperationResult start = fixture.tournamentManager.startTournament(false);

        assertTrue(start.successful(), start.message());
        assertEquals("Tournament started", start.message());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager.getState());
        assertEquals("round1-match1", fixture.runningMatchId());
        assertEquals(0, fixture.cleanup.attemptCount, "starting a tournament cleans nothing by itself");
    }

    @Test
    void refusesToStartTheFinalWhileACleaningFromTheArenaMenuRuns() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        fixture.decideRunningMatch();
        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);
        fixture.decideRunningMatch();
        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager.getFinalMatchStage());
        fixture.cleanup.startCleaningFromTheArenaMenu();

        TournamentOperationResult refusal = fixture.tournamentManager.startFinalMatch();

        assertFalse(refusal.successful(), "the final may not start into a cleaning started from the arena menu");
        assertEquals(EXPECTED_CLEANING_REFUSAL, refusal.message());
        assertFalse(fixture.matchService.hasActiveMatch());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager.getFinalMatchStage(),
                "the refused final keeps its stage");

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        TournamentOperationResult start = fixture.tournamentManager.startFinalMatch();
        assertTrue(start.successful(), start.message());
        assertEquals("round2-match1", fixture.runningMatchId());
    }

    @Test
    void refusesToResumeTheTournamentWhileACleaningFromTheArenaMenuRuns() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        assertTrue(fixture.tournamentManager.togglePause().successful());
        fixture.decideRunningMatch();
        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);
        assertFalse(fixture.matchService.hasActiveMatch(), "a paused tournament starts no match");
        fixture.cleanup.startCleaningFromTheArenaMenu();

        TournamentOperationResult refusal = fixture.tournamentManager.togglePause();

        assertFalse(refusal.successful(),
                "the tournament may not resume into a cleaning started from the arena menu");
        assertEquals(EXPECTED_CLEANING_REFUSAL, refusal.message());
        assertEquals(TournamentState.PAUSED, fixture.tournamentManager.getState());
        assertTrue(fixture.matchService.isRoundTimerPaused(), "the refused resume leaves the round timer paused");
        assertFalse(fixture.matchService.hasActiveMatch());

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        TournamentOperationResult resume = fixture.tournamentManager.togglePause();
        assertTrue(resume.successful(), resume.message());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager.getState());
        assertFalse(fixture.matchService.isRoundTimerPaused());
        assertEquals("round1-match2", fixture.runningMatchId(), "the waiting match starts after the resume");
    }

    @Test
    void resumesTheTournamentDuringItsOwnCleaningAndStartsTheNextMatchOnceTheCleaningIsDone() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        assertTrue(fixture.tournamentManager.togglePause().successful());
        fixture.decideRunningMatch();
        assertTrue(fixture.cleanup.isCleaning(), "the end of the match starts the cleaning of the tournament");

        TournamentOperationResult resume = fixture.tournamentManager.togglePause();

        assertTrue(resume.successful(), resume.message());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager.getState());
        assertFalse(fixture.matchService.hasActiveMatch(),
                "the next match waits for the cleaning the tournament started itself");

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        assertEquals("round1-match2", fixture.runningMatchId());
    }

    @Test
    void refusesToDecideAStuckMatchWhileACleaningFromTheArenaMenuRunsAndAcceptsItAfterwards() {
        Fixture fixture = new Fixture();
        fixture.startTournament();
        BracketMatch stuckMatch = fixture.tournamentManager.getBracket().orElseThrow()
                .findMatch("round1-match2").orElseThrow();
        String stuckWinnerTeamName = stuckMatch.getFirstTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry.removeTeam(stuckWinnerTeamName));
        assertTrue(fixture.teamRegistry.removeTeam(stuckMatch.getSecondTeamName().orElseThrow()));
        fixture.decideRunningMatch();
        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager.getState());
        assertFalse(fixture.matchService.hasActiveMatch(),
                "a match without any registered team waits for an admin decision");
        assertFalse(stuckMatch.isDecided());
        fixture.cleanup.startCleaningFromTheArenaMenu();

        TournamentOperationResult refusal = fixture.tournamentManager.forceMatchResult("round1-match2",
                stuckWinnerTeamName);

        assertFalse(refusal.successful(),
                "the decision may not start a match into a cleaning started from the arena menu");
        assertEquals(EXPECTED_CLEANING_REFUSAL, refusal.message());
        assertFalse(fixture.matchService.hasActiveMatch());
        assertFalse(stuckMatch.isDecided(), "the refused decision leaves the bracket as it is");
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager.getState());

        fixture.cleanup.finishCleaning(CLEAN_OUTCOME);

        TournamentOperationResult decision = fixture.tournamentManager.forceMatchResult("round1-match2",
                stuckWinnerTeamName);
        assertTrue(decision.successful(), decision.message());
        assertEquals(Optional.of(stuckWinnerTeamName), stuckMatch.getWinnerTeamName());
    }

    private static final class Fixture {

        private static final String WORLD_NAME = "tournament-flow";
        private static final int WALL_MINIMUM = 0;
        private static final int WALL_MAXIMUM = 6;
        private static final int GROUND_Y = 64;
        private static final int WALL_TOP_Y = GROUND_Y + 3;

        private final List<Object> scheduledTasks = new ArrayList<>();
        private final List<String> logMessages = new ArrayList<>();
        private final List<String> adminMessages = new ArrayList<>();
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
                    matchService, cleanup, adminMessages::add);
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

        private void playUntilTheTournamentIsOver() {
            decideRunningMatch();
            cleanup.finishCleaning(CLEAN_OUTCOME);
            decideRunningMatch();
            cleanup.finishCleaning(CLEAN_OUTCOME);
            assertTrue(tournamentManager.startFinalMatch().successful());
            decideRunningMatch();
            assertEquals(TournamentState.FINISHED, tournamentManager.getState());
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
                teamRegistry.registerTeam(teamName, List.of(
                        new TeamMember(UUID.randomUUID(), teamName + "-first"),
                        new TeamMember(UUID.randomUUID(), teamName + "-second")));
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
                        case "getWorld" -> null;
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
        private boolean baselineStored = true;

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

        @Override
        public boolean isCleaning() {
            return completionHandler != null;
        }

        @Override
        public boolean hasBaseline() {
            return baselineStored;
        }

        private void forgetBaseline() {
            this.baselineStored = false;
        }

        private void startCleaningFromTheArenaMenu() {
            this.completionHandler = outcome -> { };
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
