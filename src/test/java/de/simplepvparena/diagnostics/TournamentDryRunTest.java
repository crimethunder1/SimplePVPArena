package de.simplepvparena.diagnostics;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.tournament.TournamentState;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentDryRunTest {

    private static final String WORLD = "event";

    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final FakeDryRunArena arena = new FakeDryRunArena();
    private final KitService kitService = new KitService();
    private final StartGateMarks gateMarks = new StartGateMarks();
    private final StartGateDuration gateDuration = new StartGateDuration();

    private TournamentState tournamentState = TournamentState.IDLE;
    private boolean matchRunning;

    @Test
    void refusesToRunWhileATournamentIsInProgress() {
        for (TournamentState state : List.of(TournamentState.RUNNING, TournamentState.PAUSED)) {
            tournamentState = state;

            assertTrue(dryRun().findRejection().isPresent(), "the tile is refused in " + state);
            DryRunReport report = dryRun().run();

            assertTrue(report.isRejected());
            assertFalse(report.isSuccessful());
            assertEquals(List.of(), report.getSteps(), "a refused run walks nothing");
            assertEquals(0, teamRegistry.getTeamCount(), "and registers nothing");
            assertTrue(report.describe().stream().anyMatch(line -> line.contains("A tournament is in progress")),
                    report.describe().toString());
        }
    }

    @Test
    void refusesToRunWhileAMatchStandsOnTheArena() {
        matchRunning = true;

        DryRunReport report = dryRun().run();

        assertTrue(report.isRejected());
        assertEquals(0, teamRegistry.getTeamCount());
        assertTrue(report.describe().stream().anyMatch(line -> line.contains("A match stands on the arena")),
                report.describe().toString());
    }

    @Test
    void runsAfterATournamentIsOver() {
        makeTheServerReadyForItsEvent();
        tournamentState = TournamentState.FINISHED;

        assertTrue(dryRun().findRejection().isEmpty());
        assertTrue(findStep(dryRun().run(), TournamentDryRun.MATCH_STEP).isPassed());
    }

    @Test
    void registersTwoTestTeamsAndRemovesThemAgain() {
        registerRealTeam("Alpha");

        DryRunReport report = dryRun().run();

        DryRunStep registration = findStep(report, TournamentDryRun.TEAM_STEP);
        assertTrue(registration.isPassed(), registration.details().toString());
        assertTrue(registration.details().stream().anyMatch(line -> line.startsWith(DryRunTeam.FIRST_TEAM_NAME)));
        assertTrue(registration.details().stream().anyMatch(line -> line.startsWith(DryRunTeam.SECOND_TEAM_NAME)));

        assertTrue(findStep(report, TournamentDryRun.CLEANUP_STEP).isPassed());
        assertEquals(List.of("Alpha"), teamRegistry.getTeamNames(),
                "nothing of the dry run is left in the registry");
    }

    @Test
    void describesTheRegisteredTestTeamsAndTheirPlaceholderPlayers() {
        DryRunReport report = dryRun().run();

        assertEquals(List.of("Dry-Run-Alpha with Dry-Run-Alpha-1, Dry-Run-Alpha-2",
                "Dry-Run-Bravo with Dry-Run-Bravo-1, Dry-Run-Bravo-2",
                "Both are found by name and both placeholder players are found by their unique id",
                "The members are placeholders and no account of this server, and nothing was written to teams.yml"),
                findStep(report, TournamentDryRun.TEAM_STEP).details());
    }

    @Test
    void leavesTeamsOfOneAndThreePlayersOfTheEventAsTheyAre() {
        Team soloTeam = teamRegistry.registerTeam("Solo", List.of(new TeamMember(UUID.randomUUID(), "Steve")));
        Team trioTeam = teamRegistry.registerTeam("Trio", List.of(new TeamMember(UUID.randomUUID(), "Alex"),
                new TeamMember(UUID.randomUUID(), "Notch"), new TeamMember(UUID.randomUUID(), "Jeb_")));

        DryRunReport report = dryRun().run();

        assertTrue(findStep(report, TournamentDryRun.TEAM_STEP).isPassed());
        assertTrue(findStep(report, TournamentDryRun.CLEANUP_STEP).isPassed());
        assertEquals(List.of("Solo", "Trio"), teamRegistry.getTeamNames());
        assertEquals(soloTeam.getMembers(), teamRegistry.findTeam("Solo").orElseThrow().getMembers());
        assertEquals(trioTeam.getMembers(), teamRegistry.findTeam("Trio").orElseThrow().getMembers());
    }

    @Test
    void refusesToTouchATeamThatIsAlreadyCalledLikeATestTeam() {
        Team ownTeam = registerRealTeam(DryRunTeam.FIRST_TEAM_NAME);

        DryRunReport report = dryRun().run();

        DryRunStep registration = findStep(report, TournamentDryRun.TEAM_STEP);
        assertTrue(registration.isFailed());
        assertTrue(registration.details().stream().anyMatch(line -> line.contains(DryRunTeam.FIRST_TEAM_NAME)),
                registration.details().toString());
        assertEquals(DryRunStatus.SKIPPED, findStep(report, TournamentDryRun.MATCH_STEP).status());
        assertEquals(DryRunStatus.SKIPPED, findStep(report, TournamentDryRun.CLEANUP_STEP).status(),
                "nothing was registered, so there is nothing to remove");
        assertEquals(Optional.of(ownTeam.getMemberNamesJoined()),
                teamRegistry.findTeam(DryRunTeam.FIRST_TEAM_NAME).map(Team::getMemberNamesJoined),
                "the team of the event keeps its own two players");
    }

    @Test
    void removesTheTestTeamsAlsoWhenAStepThrows() {
        makeTheServerReadyForItsEvent();
        arena.failWith(new IllegalStateException("the arena is on fire"));

        DryRunReport report = dryRun().run();

        assertFalse(report.isSuccessful());
        assertTrue(findStep(report, TournamentDryRun.FAILURE_STEP).details().stream()
                .anyMatch(line -> line.contains("the arena is on fire")));
        assertTrue(findStep(report, TournamentDryRun.CLEANUP_STEP).isPassed());
        assertEquals(0, teamRegistry.getTeamCount());
    }

    @Test
    void writesTheFailureOfAStepWithItsStackTraceIntoTheLog() {
        makeTheServerReadyForItsEvent();
        IllegalStateException failure = new IllegalStateException("the arena is on fire");
        arena.failWith(failure);
        List<LogRecord> logRecords = new ArrayList<>();

        DryRunReport report = dryRunLoggingInto(logRecords).run();

        assertTrue(findStep(report, TournamentDryRun.MATCH_STEP).isPassed(), "the run failed in its middle");
        assertFalse(findStep(report, TournamentDryRun.FAILURE_STEP).isPassed());
        assertEquals(1, logRecords.size());
        assertEquals(Level.WARNING, logRecords.get(0).getLevel());
        assertEquals("The dry run failed", logRecords.get(0).getMessage());
        assertSame(failure, logRecords.get(0).getThrown());
    }

    @Test
    void writesNothingIntoTheLogForAStepThatFailsItsCheck() {
        makeTheServerReadyForItsEvent();
        List<LogRecord> logRecords = new ArrayList<>();

        DryRunReport report = dryRunLoggingInto(logRecords).run();

        assertFalse(findStep(report, TournamentDryRun.KIT_STEP).isPassed(), "the kit is empty");
        assertTrue(report.getSteps().stream().map(DryRunStep::name)
                .noneMatch(Messages.text(TournamentDryRun.FAILURE_STEP)::equals), report.describe().toString());
        assertEquals(List.of(), logRecords);
    }

    @Test
    void walksTheBracketOfTheTwoTestTeamsToItsChampion() {
        makeTheServerReadyForItsEvent();

        DryRunStep match = findStep(dryRun().run(), TournamentDryRun.MATCH_STEP);

        assertTrue(match.isPassed(), match.details().toString());
        String details = String.join(" | ", match.details());
        assertTrue(details.contains("round1-match1"), details);
        assertTrue(details.contains("the final is the one match an admin starts"), details);
        assertTrue(details.contains("READY"), details);
        assertTrue(details.contains("the champion is " + DryRunTeam.FIRST_TEAM_NAME), details);
        assertTrue(details.contains("FINISHED"), details);
        assertTrue(details.contains("no gate was opened"), details);
    }

    @Test
    void failsTheArenaStepWithoutCalculatingAnything() {
        arena.forget();

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.ARENA_STEP);

        assertTrue(step.isFailed());
        assertTrue(step.details().stream().anyMatch(line -> line.contains(Arena.describeCalculateAction())),
                step.details().toString());
        assertTrue(step.details().stream().anyMatch(line -> line.contains("never calculates")),
                step.details().toString());
        assertFalse(arena.isReady(), "the dry run left the arena exactly as unconfigured as it found it");
        assertTrue(arena.getVolume().isEmpty());
    }

    @Test
    void failsTheArenaStepWhileATeamSpawnIsMissing() {
        makeTheServerReadyForItsEvent();
        arena.secondTeamSpawn = false;

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.ARENA_STEP);

        assertTrue(step.isFailed());
        assertTrue(step.details().stream().anyMatch(line -> line.contains("the second team spawn")),
                step.details().toString());
    }

    @Test
    void reportsTheVolumeAndTheBlockCountOfAReadyArena() {
        makeTheServerReadyForItsEvent();

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.ARENA_STEP);

        assertTrue(step.isPassed(), step.details().toString());
        assertTrue(step.details().stream().anyMatch(
                        line -> line.contains("A cleaning writes 60 blocks between y 53 and y 67")),
                step.details().toString());
    }

    @Test
    void reportsTheBlockCountOfTheFightAreaAloneWithoutABaseline() {
        makeTheServerReadyForItsEvent();
        arena.baseline = false;

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.ARENA_STEP);

        assertTrue(step.isPassed(), step.details().toString());
        assertTrue(step.details().stream().anyMatch(
                        line -> line.contains("A cleaning writes 16 blocks between y 64 and y 67")),
                step.details().toString());
    }

    @Test
    void failsTheKitStepWhileNoKitIsSaved() {
        makeTheServerReadyForItsEvent();

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.KIT_STEP);

        assertTrue(step.isFailed());
        assertTrue(step.details().stream().anyMatch(line -> line.contains("bare hands")),
                step.details().toString());
    }

    @Test
    void skipsTheGateWhileNoBlockIsMarked() {
        makeTheServerReadyForItsEvent();
        gateMarks.clear();

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.GATE_STEP);

        assertEquals(DryRunStatus.SKIPPED, step.status());
        assertFalse(step.isFailed(), "a missing gate never colours the report red");
        assertTrue(step.details().stream().anyMatch(line -> line.contains("without a gate")),
                step.details().toString());
    }

    @Test
    void walksTheWholeGateCountdownWithoutWaitingATick() {
        makeTheServerReadyForItsEvent();

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.GATE_STEP);

        assertTrue(step.isPassed(), step.details().toString());
        String details = String.join(" | ", step.details());
        assertTrue(details.contains("reads 10 down to 1"), details);
        assertTrue(details.contains("Fight!"), details);
        assertTrue(details.contains(WORLD + ": 15 blocks in 5 layers"), details);
        assertTrue(details.contains("the last one falls on tick 200 of 200"), details);
    }

    @Test
    void namesAGateThatKeepsComingDownAfterTheCountdown() {
        makeTheServerReadyForItsEvent();
        gateDuration.setSeconds(1);
        markGate(30);

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.GATE_STEP);

        assertTrue(step.isPassed(), step.details().toString());
        assertTrue(step.details().stream().anyMatch(line -> line.contains("taller than the countdown")
                        && line.contains("10 ticks after")),
                step.details().toString());
    }

    @Test
    void failsTheCleaningStepWithoutABaseline() {
        makeTheServerReadyForItsEvent();
        arena.baseline = false;

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.CLEANING_STEP);

        assertTrue(step.isFailed());
        assertTrue(step.details().stream().anyMatch(line -> line.contains(Arena.describeCalculateAction())),
                step.details().toString());
    }

    @Test
    void reportsTheRejectionTheArenaWouldAnswerACleaningWith() {
        makeTheServerReadyForItsEvent();
        arena.cleaningRejection = "The arena is already being cleaned";

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.CLEANING_STEP);

        assertTrue(step.isFailed());
        assertTrue(step.details().contains("The arena is already being cleaned"),
                step.details().toString());
    }

    @Test
    void passesTheCleaningStepWithoutCleaningAnything() {
        makeTheServerReadyForItsEvent();

        DryRunStep step = findStep(dryRun().run(), TournamentDryRun.CLEANING_STEP);

        assertTrue(step.isPassed(), step.details().toString());
        String details = String.join(" | ", step.details());
        assertTrue(details.contains("60 blocks between y 53 and y 67"), details);
        assertTrue(details.contains("it never cleans the arena"), details);
    }

    @Test
    void reportsEveryStepOfAServerThatIsReadyForItsEvent() {
        makeTheServerReadyForItsEvent();

        DryRunReport report = dryRun().run();

        assertEquals(List.of(TournamentDryRun.TEAM_STEP, TournamentDryRun.ARENA_STEP,
                        TournamentDryRun.KIT_STEP, TournamentDryRun.GATE_STEP,
                        TournamentDryRun.MATCH_STEP, TournamentDryRun.CLEANING_STEP,
                        TournamentDryRun.CLEANUP_STEP).stream().map(Messages::text).toList(),
                report.getSteps().stream().map(DryRunStep::name).toList());
        assertEquals(1L, report.countByStatus(DryRunStatus.FAILED), report.describe().toString());
        assertTrue(findStep(report, TournamentDryRun.KIT_STEP).isFailed(), "and it is the kit");
        assertTrue(report.describe().containsAll(DryRunReport.describeRealPlayerNote()),
                "the report always says what it could not check");
    }

    @Test
    void reportsEveryLineOfTeamsOfTwoPlayersExactlyAsTheDryRunWithoutATeamSizeDid() {
        makeTheServerReadyForItsEvent();
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        List<String> withoutTeamSize = dryRun().run().describe();

        List<String> withTeamSizeTwo = dryRunWith(teamSizeSetting).run().describe();

        assertEquals(withoutTeamSize, withTeamSizeTwo);
        assertTrue(withTeamSizeTwo.contains(
                "    Both are found by name and both placeholder players are found by their unique id"),
                withTeamSizeTwo.toString());
    }

    @Test
    void registersTestTeamsOfThreePlaceholderPlayersAtATeamSizeOfThree() {
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        teamSizeSetting.setTeamSize(3);
        registerRealTeam("Alpha");

        DryRunReport report = dryRunWith(teamSizeSetting).run();

        assertEquals(List.of("Dry-Run-Alpha with Dry-Run-Alpha-1, Dry-Run-Alpha-2, Dry-Run-Alpha-3",
                "Dry-Run-Bravo with Dry-Run-Bravo-1, Dry-Run-Bravo-2, Dry-Run-Bravo-3",
                "Both are found by name and every placeholder player is found by their unique id",
                "The members are placeholders and no account of this server, and nothing was written to teams.yml"),
                findStep(report, TournamentDryRun.TEAM_STEP).details());
        assertTrue(findStep(report, TournamentDryRun.CLEANUP_STEP).isPassed());
        assertEquals(List.of("Alpha"), teamRegistry.getTeamNames());
        assertEquals(3, teamSizeSetting.getTeamSize(), "the dry run never changes the team size");
    }

    @Test
    void registersTestTeamsOfOnePlaceholderPlayerAtATeamSizeOfOne() {
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        teamSizeSetting.setTeamSize(1);

        DryRunReport report = dryRunWith(teamSizeSetting).run();

        assertEquals(List.of("Dry-Run-Alpha with Dry-Run-Alpha-1", "Dry-Run-Bravo with Dry-Run-Bravo-1",
                "Both are found by name and every placeholder player is found by their unique id",
                "The members are placeholders and no account of this server, and nothing was written to teams.yml"),
                findStep(report, TournamentDryRun.TEAM_STEP).details());
        assertEquals(1, teamSizeSetting.getTeamSize());
    }

    @Test
    void walksTheSimulatedMatchWithTestTeamsOfEightPlayers() {
        makeTheServerReadyForItsEvent();
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        teamSizeSetting.setTeamSize(8);

        DryRunReport report = dryRunWith(teamSizeSetting).run();

        assertTrue(findStep(report, TournamentDryRun.TEAM_STEP).isPassed());
        assertTrue(findStep(report, TournamentDryRun.MATCH_STEP).isPassed());
        assertTrue(findStep(report, TournamentDryRun.CLEANUP_STEP).isPassed());
        assertEquals(0, teamRegistry.getTeamCount());
        assertEquals(8, teamSizeSetting.getTeamSize());
    }

    private TournamentDryRun dryRun() {
        return new TournamentDryRun(() -> tournamentState, () -> matchRunning, teamRegistry, arena,
                kitService, gateMarks, gateDuration);
    }

    private TournamentDryRun dryRunWith(TeamSizeSetting teamSizeSetting) {
        return new TournamentDryRun(() -> tournamentState, () -> matchRunning, teamRegistry, arena,
                kitService, gateMarks, gateDuration, teamSizeSetting);
    }

    private TournamentDryRun dryRunLoggingInto(List<LogRecord> logRecords) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return new TournamentDryRun(() -> tournamentState, () -> matchRunning, teamRegistry, arena,
                kitService, gateMarks, gateDuration, new TeamSizeSetting(), logger);
    }

    private void makeTheServerReadyForItsEvent() {
        arena.volume = shape();
        arena.firstTeamSpawn = true;
        arena.secondTeamSpawn = true;
        arena.baseline = true;
        arena.cleaningRejection = null;
        markGate(5);
    }

    private void markGate(int layerCount) {
        gateMarks.clear();
        for (int layer = 0; layer < layerCount; layer++) {
            for (int blockX = 0; blockX < 3; blockX++) {
                gateMarks.mark(WORLD, blockX, 64 + layer, 0, Material.BARRIER);
            }
        }
    }

    private static ArenaShape shape() {
        return new ArenaShape(WORLD, 64, 64, 67, new long[] {
                HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(0, 1),
                HorizontalCellCodec.encode(1, 0), HorizontalCellCodec.encode(1, 1)});
    }

    private Team registerRealTeam(String teamName) {
        return teamRegistry.registerTeam(teamName, List.of(
                new TeamMember(UUID.randomUUID(), teamName + "-first"),
                new TeamMember(UUID.randomUUID(), teamName + "-second")));
    }

    private static DryRunStep findStep(DryRunReport report, MessageKey stepKey) {
        String stepName = Messages.text(stepKey);
        return report.getSteps().stream()
                .filter(step -> step.name().equals(stepName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the report holds no step " + stepName + ": "
                        + report.describe()));
    }

    private static final class FakeDryRunArena implements DryRunArena {

        private ArenaShape volume;
        private boolean firstTeamSpawn;
        private boolean secondTeamSpawn;
        private boolean baseline;
        private String cleaningRejection;
        private RuntimeException failure;

        private void forget() {
            volume = null;
            firstTeamSpawn = false;
            secondTeamSpawn = false;
            baseline = false;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public boolean isReady() {
            return volume != null;
        }

        @Override
        public List<String> getMissingConfiguration() {
            return isReady() ? List.of()
                    : List.of("the calculated fight area (" + Arena.describeCalculateAction() + ")");
        }

        @Override
        public Optional<ArenaShape> getVolume() {
            return Optional.ofNullable(volume);
        }

        @Override
        public boolean hasFirstTeamSpawn() {
            return firstTeamSpawn;
        }

        @Override
        public boolean hasSecondTeamSpawn() {
            return secondTeamSpawn;
        }

        @Override
        public boolean hasBaseline() {
            return baseline;
        }

        @Override
        public Optional<ArenaShape> findCleanedVolume() {
            return getVolume().map(shape -> baseline
                    ? shape.withFloorLoweredBy(ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR)
                    : shape);
        }

        @Override
        public long countCleaningBlocks() {
            return findCleanedVolume().map(ArenaShape::getBlockCount).orElse(0L);
        }

        @Override
        public Optional<String> findCleaningRejection() {
            if (failure != null) {
                throw failure;
            }
            return Optional.ofNullable(cleaningRejection);
        }
    }
}
