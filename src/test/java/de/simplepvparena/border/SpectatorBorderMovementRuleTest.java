package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorBorderMovementRuleTest {

    private static final long BORDER_BLOCK = BlockPositionCodec.encode(10, 64, 20);
    private static final long[] BORDER_POSITIONS = new long[] {BORDER_BLOCK};

    @Test
    void stopsAMovementIntoTheBorderBlock() {
        assertTrue(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 10, 64, 20));
    }

    @Test
    void stopsAMovementOntoTheBorderBlock() {
        assertTrue(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 10, 65, 20));
    }

    @Test
    void stopsAJumpOverTheBorderBlock() {
        assertTrue(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 10, 66, 20));
        assertTrue(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 10, 200, 20));
    }

    @Test
    void keepsTheSpaceBelowTheBorderBlockOpen() {
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 10, 63, 20));
    }

    @Test
    void keepsTheNeighbouringColumnsOpen() {
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 11, 64, 20));
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(false, BORDER_POSITIONS, 10, 64, 21));
    }

    @Test
    void letsActiveFightersPass() {
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(true, BORDER_POSITIONS, 10, 64, 20));
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(true, BORDER_POSITIONS, 10, 66, 20));
    }

    @Test
    void letsEverybodyPassWhereNoBorderBlockIsMarked() {
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(false, MarkedBlockPositions.EMPTY_POSITIONS,
                10, 64, 20));
    }

    @Test
    void stopsTheMovementAtTheLowestBorderBlockOfTheColumn() {
        long[] borderWall = new long[] {
                BlockPositionCodec.encode(10, 70, 20),
                BlockPositionCodec.encode(10, 64, 20)};

        assertTrue(SpectatorBorderMovementRule.isMovementBlocked(false, borderWall, 10, 64, 20));
        assertFalse(SpectatorBorderMovementRule.isMovementBlocked(false, borderWall, 10, 63, 20));
    }
}
