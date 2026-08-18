package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.RoundTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalWithoutSuddenDeathTest {

    private static final RoundTimeoutSettings ONE_SECOND_ROUND = new RoundTimeoutSettings(1, 0, 2.0D, 1.0D);

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        harness.getMatchService().setRoundTimeoutSettings(ONE_SECOND_ROUND);
    }

    @Test
    void theFinalIsFoughtWithoutARoundTimerByDefault() {
        assertTrue(MatchService.DEFAULT_SUDDEN_DEATH_DISABLED_IN_FINAL);
        assertTrue(harness.getMatchService().isSuddenDeathDisabledInFinal());

        Match match = harness.startFinalMatch(fighter, mate, opponent, opponentMate);

        assertTrue(match.isFinalMatch());
        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals(Optional.empty(), harness.getMatchService().findRoundTimer(),
                "the final runs without a round timer at all");
        assertEquals(0, harness.getScheduledTaskCount(),
                "and without a task that could tick one: nothing counts the round down");
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        assertTrue(harness.getMatchService().isActiveFighter(opponent.uniqueId()));
    }

    @Test
    void theFinalKeepsRunningPastTheTimeLimitOfARegularRound() {
        Match match = harness.startFinalMatch(fighter, mate, opponent, opponentMate);

        for (int tick = 0; tick < 10; tick++) {
            harness.runTick();
        }

        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals(Optional.empty(), harness.getMatchService().findRoundTimer());
        assertEquals(0, match.getScore().getPoints(match.getFirstTeam().getTeamName()),
                "nothing was decided by a clock");
        assertEquals(0, match.getScore().getPoints(match.getSecondTeam().getTeamName()));
    }

    @Test
    void theFinalIsDecidedByTheFightAlone() {
        Match match = harness.startFinalMatch(fighter, mate, opponent, opponentMate);

        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());
        assertEquals(MatchState.RUNNING, match.getState(), "one fallen fighter is not a fallen team");

        harness.getMatchService().handlePlayerEliminated(opponentMate.uniqueId());

        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertEquals(1, match.getScore().getPoints(match.getFirstTeam().getTeamName()));
    }

    @Test
    void aRegularMatchKeepsItsTimeLimitAndItsSuddenDeath() {
        assertTrue(harness.getMatchService().isSuddenDeathDisabledInFinal());

        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);

        assertFalse(match.isFinalMatch());
        RoundTimer roundTimer = harness.getMatchService().findRoundTimer().orElseThrow();
        assertEquals(1, roundTimer.getRemainingSeconds());
        assertFalse(roundTimer.isSuddenDeathActive());

        harness.runTick();

        assertTrue(roundTimer.isSuddenDeathActive(),
                "the second the round is over, sudden death starts as it always did");
        assertEquals(MatchState.RUNNING, match.getState());
    }

    @Test
    void theFinalIsAMatchLikeEveryOtherOneWhileTheSettingIsOff() {
        harness.getMatchService().setSuddenDeathDisabledInFinal(false);

        Match match = harness.startFinalMatch(fighter, mate, opponent, opponentMate);

        assertTrue(match.isFinalMatch());
        RoundTimer roundTimer = harness.getMatchService().findRoundTimer().orElseThrow();
        assertEquals(1, roundTimer.getRemainingSeconds());

        harness.runTick();

        assertTrue(roundTimer.isSuddenDeathActive(), "the final takes the sudden death of a regular round");
    }

    @Test
    void aReloadDuringTheFinalReachesTheNextRoundAndNotTheRunningOne() {
        Match match = harness.startFinalMatch(fighter, mate, opponent, opponentMate);
        assertEquals(Optional.empty(), harness.getMatchService().findRoundTimer());

        harness.getMatchService().setSuddenDeathDisabledInFinal(false);

        assertEquals(Optional.empty(), harness.getMatchService().findRoundTimer(),
                "the round that is being fought keeps what it started with");
        assertEquals(1, match.getRoundNumber());

        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());
        harness.getMatchService().handlePlayerEliminated(opponentMate.uniqueId());
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        harness.runTick();

        assertEquals(2, match.getRoundNumber());
        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals(1, harness.getMatchService().findRoundTimer().orElseThrow().getRemainingSeconds(),
                "the next round of the final is the one the reload reaches");
    }

    @Test
    void aReloadThatTakesTheClockOffAlsoOnlyReachesTheNextRound() {
        harness.getMatchService().setSuddenDeathDisabledInFinal(false);
        Match match = harness.startFinalMatch(fighter, mate, opponent, opponentMate);
        RoundTimer roundTimer = harness.getMatchService().findRoundTimer().orElseThrow();

        harness.getMatchService().setSuddenDeathDisabledInFinal(true);

        assertEquals(roundTimer, harness.getMatchService().findRoundTimer().orElseThrow(),
                "the running round keeps the very timer it started with");

        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());
        harness.getMatchService().handlePlayerEliminated(opponentMate.uniqueId());
        harness.runTick();

        assertEquals(2, match.getRoundNumber());
        assertEquals(Optional.empty(), harness.getMatchService().findRoundTimer());
        assertEquals(0, harness.getScheduledTaskCount());
    }
}
