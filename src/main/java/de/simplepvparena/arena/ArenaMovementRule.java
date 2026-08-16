package de.simplepvparena.arena;

public final class ArenaMovementRule {

    private ArenaMovementRule() {
    }

    public static boolean isMovementAllowed(boolean activeFighter, boolean belongsIntoArena, boolean operator,
                                            boolean originInside, boolean destinationInside) {
        if (activeFighter || operator) {
            return true;
        }
        if (originInside == destinationInside) {
            return true;
        }
        return destinationInside == belongsIntoArena;
    }
}
