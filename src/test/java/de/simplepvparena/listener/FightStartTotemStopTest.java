package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FightStartTotemStopTest {

    private static final long ROUND_RESET_DELAY_TICKS = 100L;

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private RecordingRoundStartEffects effects;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        matchService = harness.getMatchService();
        matchService.setCountdownSeconds(3);
        matchService.setRoundResetDelayTicks(ROUND_RESET_DELAY_TICKS);
        matchService.setReconnectSeconds(60);
        effects = harness.getRoundStartEffects();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void theFightStartsWithTheTotemOfEveryFighterAndNothingIsStoppedByIt() {
        startAndReachTheFight();

        assertEquals(List.of(), effects.stops());
        assertTrue(effects.events().contains("totem for Fighter"), effects.events().toString());
    }

    @Test
    void anEliminatedFighterGetsTheOffHandBackAtOnce() {
        startAndReachTheFight();

        matchService.handlePlayerEliminated(fighter.uniqueId());

        assertEquals(List.of("stop the totem of Fighter"), effects.stops());
    }

    @Test
    void aFighterWhoLeavesGetsTheOffHandBackBeforeTheReconnectWindowOpens() {
        startAndReachTheFight();

        matchService.handlePlayerDisconnected(fighter.player());

        assertEquals(List.of("stop the totem of Fighter"), effects.stops());
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
    }

    @Test
    void aFighterWhoLeavesWithoutAReconnectWindowGetsTheOffHandBackToo() {
        matchService.setReconnectSeconds(0);
        startAndReachTheFight();

        matchService.handlePlayerDisconnected(fighter.player());

        assertEquals("stop the totem of Fighter", effects.stops().getFirst());
    }

    @Test
    void theEndOfARoundGivesEveryOffHandBack() {
        startAndReachTheFight();

        matchService.handlePlayerEliminated(opponent.uniqueId());
        effects.stops().clear();
        matchService.handlePlayerEliminated(opponentMate.uniqueId());

        assertEquals(List.of("stop the totem of OpponentMate", "stop every totem"), effects.stops());
    }

    @Test
    void theNextRoundGivesEveryOffHandBackBeforeItStarts() {
        startAndReachTheFight();
        matchService.handlePlayerEliminated(opponent.uniqueId());
        matchService.handlePlayerEliminated(opponentMate.uniqueId());
        effects.stops().clear();

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(List.of("stop every totem"), effects.stops());
    }

    @Test
    void theEndOfTheMatchGivesEveryOffHandBack() {
        Match match = startAndReachTheFight();

        matchService.forceResult(match.getMatchId(), "Alpha");

        assertEquals(List.of("stop every totem"), effects.stops());
    }

    @Test
    void aCancelledMatchGivesEveryOffHandBack() {
        startAndReachTheFight();

        matchService.cancelActiveMatch();

        assertEquals(List.of("stop every totem"), effects.stops());
    }

    @Test
    void anEliminationOutsideOfAMatchStopsNothing() {
        matchService.handlePlayerEliminated(fighter.uniqueId());

        assertEquals(List.of(), effects.stops());
    }

    private Match startAndReachTheFight() {
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.advanceSeconds(3);
        effects.stops().clear();
        return match;
    }
}
