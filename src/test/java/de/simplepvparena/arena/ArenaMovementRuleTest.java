package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaMovementRuleTest {

    private static final boolean BELONGS_INTO_ARENA = true;
    private static final boolean BELONGS_OUTSIDE = false;
    private static final boolean OPERATOR = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;

    @Test
    void letsAFighterMoveFreelyInsideTheArena() {
        assertTrue(isAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, INSIDE));
    }

    @Test
    void stopsAFighterWhoWouldLeaveTheArena() {
        assertFalse(isAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE));
    }

    @Test
    void letsAFighterWalkBackIntoTheArena() {
        assertTrue(isAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, INSIDE));
    }

    @Test
    void neverLocksAFighterWhoStandsOutsideTheArena() {
        assertTrue(isAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, OUTSIDE));
    }

    @Test
    void keepsSpectatorsOutOfTheArena() {
        assertFalse(isAllowed(BELONGS_OUTSIDE, REGULAR_PLAYER, OUTSIDE, INSIDE));
        assertTrue(isAllowed(BELONGS_OUTSIDE, REGULAR_PLAYER, OUTSIDE, OUTSIDE));
    }

    @Test
    void neverLocksASpectatorWhoStandsInsideTheArena() {
        assertTrue(isAllowed(BELONGS_OUTSIDE, REGULAR_PLAYER, INSIDE, INSIDE));
        assertTrue(isAllowed(BELONGS_OUTSIDE, REGULAR_PLAYER, INSIDE, OUTSIDE));
    }

    @Test
    void letsAnOperatorCrossTheOutlineInBothDirections() {
        assertTrue(isAllowed(BELONGS_OUTSIDE, OPERATOR, OUTSIDE, INSIDE), "walking into the arena");
        assertTrue(isAllowed(BELONGS_OUTSIDE, OPERATOR, INSIDE, OUTSIDE), "and back out of it");
    }

    @Test
    void letsAnOperatorPassNoMatterWhichSideHeBelongsTo() {
        for (boolean belongsIntoArena : new boolean[] {BELONGS_INTO_ARENA, BELONGS_OUTSIDE}) {
            assertTrue(isAllowed(belongsIntoArena, OPERATOR, INSIDE, OUTSIDE));
            assertTrue(isAllowed(belongsIntoArena, OPERATOR, OUTSIDE, INSIDE));
            assertTrue(isAllowed(belongsIntoArena, OPERATOR, INSIDE, INSIDE));
            assertTrue(isAllowed(belongsIntoArena, OPERATOR, OUTSIDE, OUTSIDE));
        }
    }

    @Test
    void letsAFightingOperatorLeaveTheArena() {
        assertFalse(isAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE));
        assertTrue(isAllowed(BELONGS_INTO_ARENA, OPERATOR, INSIDE, OUTSIDE));
    }

    private static boolean isAllowed(boolean belongsIntoArena, boolean operator,
                                     boolean originInside, boolean destinationInside) {
        return ArenaMovementRule.isMovementAllowed(belongsIntoArena, operator, originInside, destinationInside);
    }
}
