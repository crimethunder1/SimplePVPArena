package de.simplepvparena.border;

import de.simplepvparena.arena.HorizontalCellCodec;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorBorderMovementRuleTest {

    private static final boolean FIGHTER = true;
    private static final boolean SPECTATOR = false;
    private static final boolean ARENA_FLIGHT = true;
    private static final boolean ON_THE_TRIBUNE = false;
    private static final boolean ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;

    @Test
    void barsTheWholeColumnOfABorderBlock() {
        BlockedColumnLookup wall = wall(10, 20);

        assertTrue(isBlocked(wall, 9, 20, 10, 20), "walking into the column from the side");
        assertTrue(isBlocked(wall, 11, 20, 10, 20));
        assertTrue(isBlocked(wall, 10, 19, 10, 20));
        assertTrue(isBlocked(wall, 10, 21, 10, 20));
    }

    @Test
    void stopsTheMovementNoMatterHowDeepTheTunnelUnderTheWallIs() {
        BlockedColumnLookup wall = wall(10, 20);

        assertFalse(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, REGULAR_PLAYER,
                wall, 9, 20, 10, 20));
    }

    @Test
    void keepsTheNeighbouringColumnsOpen() {
        BlockedColumnLookup wall = wall(10, 20);

        assertFalse(isBlocked(wall, 12, 20, 11, 20));
        assertFalse(isBlocked(wall, 10, 22, 10, 21));
    }

    @Test
    void letsEverybodyPassWhereNoBorderBlockIsMarked() {
        assertFalse(isBlocked(BlockedColumnLookup.NOTHING_BLOCKED, 9, 20, 10, 20));
    }

    @Test
    void neverLocksAPlayerInsideABorderColumn() {
        BlockedColumnLookup wall = wall(10, 20);

        assertFalse(isBlocked(wall, 10, 20, 10, 20), "a jump inside the border column");
        assertFalse(isBlocked(wall, 10, 20, 11, 20), "walking out of the border column");
    }

    @Test
    void stopsAWalkAlongTheWallFromInsideTheWall() {
        BlockedColumnLookup wall = wall(10, 20, 10, 21);

        assertTrue(isBlocked(wall, 10, 20, 10, 21));
    }

    @Test
    void stopsADiagonalStepBetweenTwoDiagonallyPlacedBorderBlocks() {
        BlockedColumnLookup wall = wall(10, 20, 11, 21);

        assertFalse(wall.isBlockedColumn(11, 20), "the destination column itself is free");
        assertTrue(isBlocked(wall, 10, 21, 11, 20));
        assertTrue(isBlocked(wall, 11, 20, 10, 21), "and the same step back through the gap");
    }

    @Test
    void letsADiagonalStepPassWhenOnlyOneOfTheTwoColumnsIsBarred() {
        BlockedColumnLookup wall = wall(10, 20);

        assertFalse(isBlocked(wall, 10, 21, 11, 20));
        assertFalse(isBlocked(wall, 11, 20, 10, 21));
    }

    @Test
    void letsADiagonalStepPassWhenNothingIsBarred() {
        assertFalse(isBlocked(BlockedColumnLookup.NOTHING_BLOCKED, 10, 21, 11, 20));
    }

    @Test
    void stopsADiagonalStepStraightIntoABorderColumn() {
        assertTrue(isBlocked(wall(11, 20), 10, 21, 11, 20));
    }

    @Test
    void letsActiveFightersPass() {
        BlockedColumnLookup wall = wall(10, 20, 11, 21);

        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(FIGHTER, ON_THE_TRIBUNE, REGULAR_PLAYER,
                wall, 9, 20, 10, 20));
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(FIGHTER, ON_THE_TRIBUNE, REGULAR_PLAYER,
                wall, 10, 21, 11, 20), "including the diagonal gap");
    }

    @Test
    void letsSpectatorsFlyingThroughTheArenaPass() {
        BlockedColumnLookup wall = wall(10, 20, 10, 21, 11, 21);

        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ARENA_FLIGHT, REGULAR_PLAYER,
                wall, 9, 20, 10, 20), "the step into the wall");
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ARENA_FLIGHT, REGULAR_PLAYER,
                wall, 10, 20, 10, 21), "and the step along inside it");
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ARENA_FLIGHT, REGULAR_PLAYER,
                wall, 10, 21, 11, 20), "and the diagonal gap between two border blocks");
    }

    @Test
    void keepsStoppingASpectatorOnTheTribune() {
        BlockedColumnLookup wall = wall(10, 20, 10, 21, 11, 21);

        assertFalse(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, REGULAR_PLAYER,
                wall, 9, 20, 10, 20), "the step into the wall");
        assertFalse(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, REGULAR_PLAYER,
                wall, 10, 20, 10, 21), "and the step along inside it");
        assertFalse(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, REGULAR_PLAYER,
                wall, 10, 21, 11, 20), "and the diagonal gap between two border blocks");
    }

    @Test
    void letsAFlyingSpectatorMoveWhereNoBorderIsInTheWayAtAll() {
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ARENA_FLIGHT, REGULAR_PLAYER,
                wall(10, 20), 12, 20, 11, 20));
    }

    @Test
    void letsAdminsPassSilently() {
        BlockedColumnLookup wall = wall(10, 20, 11, 21);

        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, ADMIN,
                wall, 9, 20, 10, 20), "the step into the wall");
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, ADMIN,
                wall, 10, 20, 10, 21), "and the step along inside it");
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, ADMIN,
                wall, 10, 21, 11, 20), "and the diagonal gap between two border blocks");
    }

    @Test
    void letsAnAdminMoveWhereNoBorderIsInTheWayAtAll() {
        assertTrue(SpectatorBorderMovementRule.isMovementAllowed(SPECTATOR, ON_THE_TRIBUNE, ADMIN,
                wall(10, 20), 12, 20, 11, 20));
    }

    private static boolean isBlocked(BlockedColumnLookup blockedColumns, int originBlockX, int originBlockZ,
                                     int destinationBlockX, int destinationBlockZ) {
        return SpectatorBorderMovementRule.isMovementBlocked(blockedColumns, originBlockX, originBlockZ,
                destinationBlockX, destinationBlockZ);
    }

    private static BlockedColumnLookup wall(int... coordinatePairs) {
        Set<Long> barredColumns = new LinkedHashSet<>();
        for (int pairIndex = 0; pairIndex < coordinatePairs.length; pairIndex += 2) {
            barredColumns.add(HorizontalCellCodec.encode(coordinatePairs[pairIndex], coordinatePairs[pairIndex + 1]));
        }
        return (blockX, blockZ) -> barredColumns.contains(HorizontalCellCodec.encode(blockX, blockZ));
    }
}
