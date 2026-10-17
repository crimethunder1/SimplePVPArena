package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateRoundStartTest {

    private static final int GATE_X = 2;
    private static final int GATE_Z = 3;

    private static final int SECOND_GATE_X = 8;
    private static final int SECOND_GATE_Z = 7;

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
    }

    @Test
    void startsTheRoundWhenNoGateIsPlacedAtAll() {
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);

        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals(1, match.getRoundNumber());
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        assertTrue(harness.getMatchService().isActiveFighter(opponent.uniqueId()));
        assertEquals(2.5D, fighter.location().getX(), "the fighter still arrives on the team spawn");
    }

    @Test
    void putsTheWholeGateBackAndStartsTheRound() {
        markGateBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.BARRIER);
        markGateBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 2, GATE_Z, Material.BARRIER);
        markGateBlock(SECOND_GATE_X, ArenaMatchHarness.GROUND_Y + 1, SECOND_GATE_Z, Material.IRON_BARS);
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.AIR);
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 2, GATE_Z, Material.AIR);
        harness.setBlock(SECOND_GATE_X, ArenaMatchHarness.GROUND_Y + 1, SECOND_GATE_Z, Material.AIR);

        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);

        assertEquals(MatchState.RUNNING, match.getState());
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        assertEquals(Material.BARRIER, harness.findMaterial(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z));
        assertEquals(Material.BARRIER, harness.findMaterial(GATE_X, ArenaMatchHarness.GROUND_Y + 2, GATE_Z));
        assertEquals(Material.IRON_BARS,
                harness.findMaterial(SECOND_GATE_X, ArenaMatchHarness.GROUND_Y + 1, SECOND_GATE_Z),
                "the gate of the other spawn is the same registry and comes back with it");
    }

    @Test
    void putsTheGateBackWhenARoundEndsDuringTheCountdown() {
        markGateBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.BARRIER);
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.AIR);

        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());
        harness.getMatchService().handlePlayerEliminated(opponentMate.uniqueId());

        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertEquals(Material.BARRIER, harness.findMaterial(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z));
    }

    @Test
    void putsTheGateBackWhenTheMatchIsCancelled() {
        markGateBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.BARRIER);
        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.AIR);
        harness.forgetTribuneSpawn();

        harness.getMatchService().cancelActiveMatch();

        assertEquals(Material.BARRIER, harness.findMaterial(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z));
    }

    private void markGateBlock(int blockX, int blockY, int blockZ, Material material) {
        harness.setBlock(blockX, blockY, blockZ, material);
        harness.getGateMarks().mark(ArenaMatchHarness.WORLD_NAME, blockX, blockY, blockZ, material);
    }
}
