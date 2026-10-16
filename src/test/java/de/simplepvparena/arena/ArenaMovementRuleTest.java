package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaMovementRuleTest {

    private static final boolean ACTIVE_FIGHTER = true;
    private static final boolean NOT_FIGHTING = false;
    private static final boolean BELONGS_INTO_ARENA = true;
    private static final boolean BELONGS_OUTSIDE = false;
    private static final boolean ADMIN = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;

    @Test
    void letsAFighterMoveFreelyInsideTheArena() {
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, INSIDE));
    }

    @Test
    void letsAFighterCrossTheOutlineTheBorderWallStandsIn() {
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE),
                "stepping onto the wall");
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, INSIDE),
                "and back off it");
    }

    @Test
    void neverStopsAnActiveFighter() {
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, INSIDE));
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE));
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, INSIDE));
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, OUTSIDE));
    }

    @Test
    void keepsStoppingAFlyingSpectatorAtTheOutline() {
        assertFalse(isAllowed(NOT_FIGHTING, BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE));
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, INSIDE));
    }

    @Test
    void neverLocksAFlyingSpectatorWhoStandsOutsideTheArena() {
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_INTO_ARENA, REGULAR_PLAYER, OUTSIDE, OUTSIDE));
    }

    @Test
    void keepsSpectatorsOutOfTheArena() {
        assertFalse(isAllowed(NOT_FIGHTING, BELONGS_OUTSIDE, REGULAR_PLAYER, OUTSIDE, INSIDE));
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_OUTSIDE, REGULAR_PLAYER, OUTSIDE, OUTSIDE));
    }

    @Test
    void neverLocksASpectatorWhoStandsInsideTheArena() {
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_OUTSIDE, REGULAR_PLAYER, INSIDE, INSIDE));
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_OUTSIDE, REGULAR_PLAYER, INSIDE, OUTSIDE));
    }

    @Test
    void letsAnAdminCrossTheOutlineInBothDirections() {
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_OUTSIDE, ADMIN, OUTSIDE, INSIDE), "walking into the arena");
        assertTrue(isAllowed(NOT_FIGHTING, BELONGS_OUTSIDE, ADMIN, INSIDE, OUTSIDE), "and back out of it");
    }

    @Test
    void letsAnAdminPassNoMatterWhichSideHeBelongsTo() {
        for (boolean belongsIntoArena : new boolean[] {BELONGS_INTO_ARENA, BELONGS_OUTSIDE}) {
            assertTrue(isAllowed(NOT_FIGHTING, belongsIntoArena, ADMIN, INSIDE, OUTSIDE));
            assertTrue(isAllowed(NOT_FIGHTING, belongsIntoArena, ADMIN, OUTSIDE, INSIDE));
            assertTrue(isAllowed(NOT_FIGHTING, belongsIntoArena, ADMIN, INSIDE, INSIDE));
            assertTrue(isAllowed(NOT_FIGHTING, belongsIntoArena, ADMIN, OUTSIDE, OUTSIDE));
        }
    }

    @Test
    void letsAFightingAdminLeaveTheArena() {
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, ADMIN, INSIDE, OUTSIDE));
        assertTrue(isAllowed(ACTIVE_FIGHTER, BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE));
    }

    private static boolean isAllowed(boolean activeFighter, boolean belongsIntoArena, boolean admin,
                                     boolean originInside, boolean destinationInside) {
        return ArenaMovementRule.isMovementAllowed(activeFighter, belongsIntoArena, admin,
                originInside, destinationInside);
    }
}
