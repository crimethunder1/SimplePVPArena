package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.RoundTimeoutSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectWindowLimitTest {

    private static final long RECONNECT_TICKS = 60L * ArenaMatchHarness.TICKS_PER_SECOND;
    private static final long ROUND_RESET_DELAY_TICKS = 100L;
    private static final String FIGHTER_LOST = "Fighter lost the connection, 60 seconds to come back";
    private static final String FIGHTER_NOT_BACK = "Fighter did not come back and is eliminated";
    private static final String CONNECTION_LOST = "lost the connection";
    private static final String SUDDEN_DEATH_STARTED = "The time limit of round 1 in match M1 is over, sudden death "
            + "has started";

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        matchService = harness.getMatchService();
        matchService.setRoundResetDelayTicks(ROUND_RESET_DELAY_TICKS);
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void aSecondConnectionLossInTheSameRoundEliminatesAtOnce() {
        Match match = start();
        disconnect(fighter);
        assertTrue(reconnect(fighter));

        disconnect(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(1, count(opponent, FIGHTER_LOST), "no second window is announced");
        assertEquals(MatchState.RUNNING, match.getState(), "the mate still fights");
    }

    @Test
    void aSecondConnectionLossOfTheLastFighterOfATeamLosesTheRoundAtOnce() {
        Match match = start();
        eliminate(mate);
        disconnect(fighter);
        assertTrue(reconnect(fighter));

        disconnect(fighter);

        assertEquals("Alpha 0 : 1 Beta", match.getScore().describe());
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertEquals(0, count(opponent, FIGHTER_NOT_BACK), "the elimination of a quit is not announced");
    }

    @Test
    void theWindowOfAnotherFighterIsNotUsedUp() {
        start();
        disconnect(fighter);
        assertTrue(reconnect(fighter));

        disconnect(mate);

        assertTrue(matchService.isWaitingForReconnect(mate.uniqueId()));
        assertTrue(matchService.isActiveFighter(mate.uniqueId()));
    }

    @Test
    void theNextRoundGivesEveryFighterAWindowAgain() {
        Match match = start();
        disconnect(fighter);
        assertTrue(reconnect(fighter));
        eliminate(opponent);
        eliminate(opponentMate);
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        assertEquals(2, match.getRoundNumber());

        disconnect(fighter);

        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertEquals(2, count(opponent, FIGHTER_LOST));

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals(1, count(opponent, FIGHTER_NOT_BACK));
        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
    }

    @Test
    void aConnectionLossInSuddenDeathEliminatesAtOnce() {
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        Match match = start();
        harness.runRepeatingTasks();
        assertEquals(1, count(opponent, SUDDEN_DEATH_STARTED));

        disconnect(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(0, countContaining(opponent, CONNECTION_LOST));
        assertEquals(MatchState.RUNNING, match.getState(), "the mate still fights");

        disconnect(mate);

        assertEquals("Alpha 0 : 1 Beta", match.getScore().describe());
    }

    @Test
    void aWindowOpenedBeforeSuddenDeathStaysOpenAndTheAbsentFighterTakesNoDamage() {
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        List<String> damagedPlayers = new ArrayList<>();
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            participant.reactToDamageWith(() -> damagedPlayers.add(participant.name()));
        }
        start();
        disconnect(fighter);

        harness.runRepeatingTasks();
        harness.runRepeatingTasks();

        assertEquals(1, count(opponent, SUDDEN_DEATH_STARTED));
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(damagedPlayers.contains("Fighter"), damagedPlayers.toString());
        assertTrue(damagedPlayers.containsAll(List.of("Mate", "Opponent", "OpponentMate")),
                damagedPlayers.toString());

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals(1, count(opponent, FIGHTER_NOT_BACK), "the deadline still decides");
        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
    }

    @Test
    void aFighterWhoComesBackInSuddenDeathAndLeavesAgainIsEliminatedAtOnce() {
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        start();
        disconnect(fighter);
        harness.runRepeatingTasks();
        assertTrue(reconnect(fighter));

        disconnect(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(1, count(opponent, FIGHTER_LOST));
    }

    private Match start() {
        return harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    private void disconnect(ArenaMatchHarness.FakePlayer player) {
        matchService.handlePlayerDisconnected(player.player());
        player.setOnline(false);
    }

    private boolean reconnect(ArenaMatchHarness.FakePlayer player) {
        player.setOnline(true);
        return matchService.handlePlayerReturned(player.player());
    }

    private void eliminate(ArenaMatchHarness.FakePlayer player) {
        matchService.handlePlayerEliminated(player.uniqueId());
    }

    private static int count(ArenaMatchHarness.FakePlayer receiver, String line) {
        return Collections.frequency(receiver.messages(), line);
    }

    private static int countContaining(ArenaMatchHarness.FakePlayer receiver, String part) {
        return (int) receiver.messages().stream().filter(message -> message.contains(part)).count();
    }
}
