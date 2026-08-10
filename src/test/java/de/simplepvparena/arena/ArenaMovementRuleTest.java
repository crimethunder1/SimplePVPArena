package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaMovementRuleTest {

    @Test
    void letsAFighterMoveFreelyInsideTheArena() {
        assertTrue(ArenaMovementRule.isMovementAllowed(true, true, true));
    }

    @Test
    void stopsAFighterWhoWouldLeaveTheArena() {
        assertFalse(ArenaMovementRule.isMovementAllowed(true, true, false));
    }

    @Test
    void letsAFighterWalkBackIntoTheArena() {
        assertTrue(ArenaMovementRule.isMovementAllowed(true, false, true));
    }

    @Test
    void neverLocksAFighterWhoStandsOutsideTheArena() {
        assertTrue(ArenaMovementRule.isMovementAllowed(true, false, false));
    }

    @Test
    void keepsSpectatorsOutOfTheArena() {
        assertFalse(ArenaMovementRule.isMovementAllowed(false, false, true));
        assertTrue(ArenaMovementRule.isMovementAllowed(false, false, false));
    }

    @Test
    void neverLocksASpectatorWhoStandsInsideTheArena() {
        assertTrue(ArenaMovementRule.isMovementAllowed(false, true, true));
        assertTrue(ArenaMovementRule.isMovementAllowed(false, true, false));
    }
}
