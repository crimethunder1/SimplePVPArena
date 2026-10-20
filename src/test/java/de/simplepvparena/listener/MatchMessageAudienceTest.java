package de.simplepvparena.listener;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.gate.StartGateCountdown;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.match.RoundTimeoutSettings;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchMessageAudienceTest {

    private static final double FAR_AWAY = TournamentAudienceHarness.FAR_AWAY;
    private static final int GATE_X = 2;
    private static final int GATE_Z = 3;
    private static final int TICKS_PER_SECOND = 20;
    private static final RoundTimeoutSettings TWO_SECOND_ROUND_WITH_A_WARNING =
            new RoundTimeoutSettings(2, 1, 0.0D, 0.0D);
    private static final RoundTimeoutSettings ONE_SECOND_ROUND = new RoundTimeoutSettings(1, 0, 2.0D, 1.0D);
    private static final String SUDDEN_DEATH_WARNING = "Sudden death in 1 second";
    private static final String SUDDEN_DEATH_TITLE = "Sudden death / Both teams take damage until one of them falls";
    private static final String SUDDEN_DEATH_LINE = "The time limit of round 1 in match M1 is over, sudden death "
            + "has started";

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;
    private ArenaMatchHarness.FakePlayer bystander;
    private ArenaMatchHarness.FakePlayer outsider;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        bystander = harness.addPlayer("Bystander");
        outsider = harness.addPlayer("Outsider");
        outsider.moveTo(harness.at(FAR_AWAY, FAR_AWAY));
        harness.getArenaRegistry().getArena().setFirstTeamSpawn(new PositionSnapshot(ArenaMatchHarness.WORLD_NAME,
                FAR_AWAY, ArenaMatchHarness.GROUND_Y, FAR_AWAY, 0F, 0F));
    }

    @Test
    void theCountdownOfTheStartGateReachesThePlayersInTheAreaAndTheTeamMembersOutsideOfIt() {
        harness.useArenaAudience();
        markGateBlock();

        harness.startMatch(fighter, mate, opponent, opponentMate);
        runOneSecond();

        assertBystanderIsNoSpectatorAndFighterIsOutsideOfTheArea();
        List<String> expectedTitles = List.of(countdownTitle(StartGateDuration.DEFAULT_SECONDS),
                countdownTitle(StartGateDuration.DEFAULT_SECONDS - 1));
        assertEquals(expectedTitles, bystander.titleTexts());
        assertEquals(expectedTitles, fighter.titleTexts(), "a member far outside sees it");
        assertEquals(expectedTitles, opponent.titleTexts());
        assertEquals(List.of(), outsider.titleTexts());
    }

    @Test
    void theSuddenDeathAndItsWarningReachThePlayersInTheAreaAndTheTeamMembersOutsideOfIt() {
        harness.useArenaAudience();
        harness.getMatchService().setRoundTimeoutSettings(TWO_SECOND_ROUND_WITH_A_WARNING);

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runRepeatingTasks();
        harness.runRepeatingTasks();

        assertBystanderIsNoSpectatorAndFighterIsOutsideOfTheArea();
        for (ArenaMatchHarness.FakePlayer receiver : List.of(bystander, fighter, opponent)) {
            assertEquals(List.of(SUDDEN_DEATH_WARNING), receiver.actionBars(), receiver.name());
            assertEquals(List.of(SUDDEN_DEATH_TITLE), receiver.titleTexts(), receiver.name());
            assertEquals(1, TournamentAudienceHarness.countMessages(receiver, SUDDEN_DEATH_LINE), receiver.name());
        }
        assertEquals(List.of(), outsider.actionBars());
        assertEquals(List.of(), outsider.titleTexts());
        assertEquals(List.of(), outsider.messages());
    }

    @Test
    void theChatLinesOfTheRoundsAndTheMatchReachThePlayersInTheAreaAndTheTeamMembersOutsideOfIt() {
        harness.useArenaAudience();
        String timeLimit = "time limit " + RoundTimeoutSettings.DEFAULT_ROUND_TIMEOUT_SECONDS + " seconds";

        harness.startMatch(fighter, mate, opponent, opponentMate);
        assertBystanderIsNoSpectatorAndFighterIsOutsideOfTheArea();
        winRoundForAlpha();
        harness.runTick();
        winRoundForAlpha();

        List<String> expectedLines = List.of(
                "Round 1 of match M1 has started (Alpha 0 : 0 Beta), " + timeLimit,
                "Alpha scores a point (Alpha 1 : 0 Beta)",
                "Round 2 of match M1 has started (Alpha 1 : 0 Beta), " + timeLimit,
                "Alpha scores a point (Alpha 2 : 0 Beta)",
                "Match M1 is over, Alpha wins (Alpha 2 : 0 Beta)");
        assertEquals(expectedLines, bystander.messages());
        for (String expectedLine : expectedLines) {
            assertEquals(1, TournamentAudienceHarness.countMessages(fighter, expectedLine), expectedLine);
            assertEquals(1, TournamentAudienceHarness.countMessages(opponent, expectedLine), expectedLine);
        }
        assertEquals(List.of(), outsider.messages());
    }

    @Test
    void theLinesOfADrawReachThePlayersInTheAreaAndTheTeamMembersOutsideOfIt() {
        harness.useArenaAudience();
        harness.getMatchService().setRoundTimeoutSettings(ONE_SECOND_ROUND);
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            participant.reactToDamageWith(() -> harness.getMatchService()
                    .handlePlayerEliminated(participant.uniqueId()));
        }

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runTick();
        harness.runRepeatingTasks();

        List<String> drawLines = List.of(
                "Round 1 of match M1 ended without a surviving team, no point is awarded",
                "Both teams fell in the same sudden death second, round 1 of match M1 is repeated");
        for (ArenaMatchHarness.FakePlayer receiver : List.of(bystander, fighter, opponent)) {
            for (String drawLine : drawLines) {
                assertEquals(1, TournamentAudienceHarness.countMessages(receiver, drawLine), receiver.name());
            }
        }
        assertEquals(List.of(), outsider.messages());
    }

    @Test
    void theHaltedMatchReachesThePlayersInTheAreaWhenNoParticipantIsOnline() {
        harness.useArenaAudience();
        harness.getMatchService().setRoundTimeoutSettings(ONE_SECOND_ROUND);
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            participant.reactToDamageWith(() -> {
                harness.getMatchService().handlePlayerEliminated(participant.uniqueId());
                participant.setOnline(false);
            });
        }

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runTick();
        harness.runRepeatingTasks();

        String haltedLine = "Match M1 is halted, no participant is online\n"
                + "Use \"Force a result\" in the match menu of /tournament to resolve it";
        assertEquals(1, TournamentAudienceHarness.countMessages(bystander, haltedLine),
                bystander.messages().toString());
        assertEquals(List.of(), outsider.messages());
    }

    @Test
    void aSpectatorOutsideOfTheAreaNoLongerReceivesTheMatchMessages() {
        harness.useArenaAudience();
        harness.getMatchService().setRoundTimeoutSettings(TWO_SECOND_ROUND_WITH_A_WARNING);
        markGateBlock();
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        harness.getSpectatorService().sendToTribune(spectator.player());
        spectator.moveTo(harness.at(FAR_AWAY, FAR_AWAY));

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runRepeatingTasks();
        harness.runRepeatingTasks();

        assertTrue(harness.getSpectatorService().getSpectators().contains(spectator.uniqueId()));
        assertEquals(List.of(), spectator.messages());
        assertEquals(List.of(), spectator.actionBars());
        assertEquals(List.of(), spectator.titleTexts());
    }

    @Test
    void withoutTheArenaAudienceTheMatchMessagesGoToTheTeamsAndTheSpectatorsAsBefore() {
        harness.getMatchService().setRoundTimeoutSettings(TWO_SECOND_ROUND_WITH_A_WARNING);
        markGateBlock();
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        harness.getSpectatorService().sendToTribune(spectator.player());
        spectator.moveTo(harness.at(FAR_AWAY, FAR_AWAY));

        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.runRepeatingTasks();
        harness.runRepeatingTasks();

        String roundStartLine = "Round 1 of match M1 has started (Alpha 0 : 0 Beta), time limit 2 seconds";
        List<String> expectedTitles = List.of(countdownTitle(StartGateDuration.DEFAULT_SECONDS), SUDDEN_DEATH_TITLE);
        for (ArenaMatchHarness.FakePlayer receiver : List.of(fighter, opponent, spectator)) {
            assertEquals(1, TournamentAudienceHarness.countMessages(receiver, roundStartLine), receiver.name());
            assertEquals(1, TournamentAudienceHarness.countMessages(receiver, SUDDEN_DEATH_LINE), receiver.name());
            assertEquals(List.of(SUDDEN_DEATH_WARNING), receiver.actionBars(), receiver.name());
            assertEquals(expectedTitles, receiver.titleTexts(), receiver.name());
        }
        assertEquals(List.of(), bystander.messages());
        assertEquals(List.of(), bystander.actionBars());
        assertEquals(List.of(), bystander.titleTexts());
    }

    private void assertBystanderIsNoSpectatorAndFighterIsOutsideOfTheArea() {
        assertFalse(harness.getSpectatorService().getSpectators().contains(bystander.uniqueId()));
        List<?> playersInTheArea = harness.getArenaAudience().findPlayersInTheArea();
        assertTrue(playersInTheArea.contains(bystander.player()));
        assertFalse(playersInTheArea.contains(fighter.player()));
        assertFalse(playersInTheArea.contains(outsider.player()));
    }

    private void winRoundForAlpha() {
        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());
        harness.getMatchService().handlePlayerEliminated(opponentMate.uniqueId());
    }

    private void runOneSecond() {
        for (int tick = 0; tick < TICKS_PER_SECOND; tick++) {
            harness.runRepeatingTasks();
        }
    }

    private void markGateBlock() {
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.BARRIER);
        harness.getGateMarks().mark(ArenaMatchHarness.WORLD_NAME, GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z,
                Material.BARRIER);
    }

    private static String countdownTitle(int remainingSeconds) {
        return StartGateCountdown.describeTitle(remainingSeconds) + " / "
                + StartGateCountdown.describeSubtitle(remainingSeconds);
    }
}
