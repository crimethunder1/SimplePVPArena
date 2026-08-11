package de.simplepvparena.arena;

public final class ArenaFlightExitRule {

    private ArenaFlightExitRule() {
    }

    public static boolean leavesTheArena(boolean arenaFlight, boolean originInside, boolean destinationInside) {
        if (!arenaFlight) {
            return false;
        }
        return originInside && !destinationInside;
    }

    public static boolean isMovementAllowed(boolean belongsIntoArena, boolean operator, boolean arenaFlight,
                                            boolean originInside, boolean destinationInside) {
        if (leavesTheArena(arenaFlight, originInside, destinationInside)) {
            return true;
        }
        return ArenaMovementRule.isMovementAllowed(belongsIntoArena, operator, originInside, destinationInside);
    }
}
