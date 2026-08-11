package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaFlightExitRuleTest {

    private static final boolean ARENA_FLIGHT = true;
    private static final boolean NOT_FLYING = false;
    private static final boolean BELONGS_INTO_ARENA = true;
    private static final boolean BELONGS_OUTSIDE = false;
    private static final boolean OPERATOR = true;
    private static final boolean REGULAR_PLAYER = false;
    private static final boolean INSIDE = true;
    private static final boolean OUTSIDE = false;

    @Test
    void endsTheFlightOfASpectatorWhoWouldLeaveTheArena() {
        assertTrue(leavesTheArena(ARENA_FLIGHT, INSIDE, OUTSIDE));
    }

    @Test
    void neverStopsTheMovementThatEndsAFlight() {
        assertFalse(ArenaMovementRule.isMovementAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, INSIDE, OUTSIDE),
                "the movement rule on its own stops that movement");
        assertTrue(isMovementAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, ARENA_FLIGHT, INSIDE, OUTSIDE),
                "and the flying spectator is let through it anyway");
        assertTrue(isMovementAllowed(BELONGS_INTO_ARENA, OPERATOR, ARENA_FLIGHT, INSIDE, OUTSIDE),
                "exactly like the operator always was");
    }

    @Test
    void keepsStoppingEveryOtherMovementOfAFlyingSpectator() {
        assertFalse(isMovementAllowed(BELONGS_OUTSIDE, REGULAR_PLAYER, ARENA_FLIGHT, OUTSIDE, INSIDE),
                "flying back in is not an exit and stays stopped");
    }

    @Test
    void keepsStoppingEverybodyWhoIsNotFlying() {
        assertFalse(isMovementAllowed(BELONGS_INTO_ARENA, REGULAR_PLAYER, NOT_FLYING, INSIDE, OUTSIDE),
                "the fighter who runs against the outline");
        assertFalse(isMovementAllowed(BELONGS_OUTSIDE, REGULAR_PLAYER, NOT_FLYING, OUTSIDE, INSIDE),
                "and the spectator on the tribune who walks into the arena");
    }

    @Test
    void letsASpectatorFlyInsideTheArena() {
        assertFalse(leavesTheArena(ARENA_FLIGHT, INSIDE, INSIDE));
    }

    @Test
    void neverEndsAFlightThatDoesNotLeaveTheArena() {
        assertFalse(leavesTheArena(ARENA_FLIGHT, OUTSIDE, INSIDE));
        assertFalse(leavesTheArena(ARENA_FLIGHT, OUTSIDE, OUTSIDE));
    }

    @Test
    void neverSendsAFighterToTheTribune() {
        assertFalse(leavesTheArena(NOT_FLYING, INSIDE, OUTSIDE));
    }

    @Test
    void neverTouchesASpectatorOnTheTribune() {
        assertFalse(leavesTheArena(NOT_FLYING, OUTSIDE, INSIDE));
        assertFalse(leavesTheArena(NOT_FLYING, OUTSIDE, OUTSIDE));
        assertFalse(leavesTheArena(NOT_FLYING, INSIDE, INSIDE));
    }

    @Test
    void endsTheFlightOfAnOperatorWhoLeavesTheArena() {
        assertTrue(ArenaMovementRule.isMovementAllowed(true, OPERATOR, INSIDE, OUTSIDE),
                "the movement rule lets him pass");
        assertTrue(leavesTheArena(ARENA_FLIGHT, INSIDE, OUTSIDE),
                "and the flight ends anyway");
    }

    @Test
    void neverSendsAnOperatorWhoIsNotFlyingToTheTribune() {
        assertTrue(ArenaMovementRule.isMovementAllowed(false, OPERATOR, INSIDE, OUTSIDE),
                "the movement rule lets him pass");
        assertFalse(leavesTheArena(NOT_FLYING, INSIDE, OUTSIDE));
    }

    private static boolean leavesTheArena(boolean arenaFlight, boolean originInside, boolean destinationInside) {
        return ArenaFlightExitRule.leavesTheArena(arenaFlight, originInside, destinationInside);
    }

    private static boolean isMovementAllowed(boolean belongsIntoArena, boolean operator, boolean arenaFlight,
                                             boolean originInside, boolean destinationInside) {
        return ArenaFlightExitRule.isMovementAllowed(belongsIntoArena, operator, arenaFlight,
                originInside, destinationInside);
    }
}
