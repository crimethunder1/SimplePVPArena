package de.simplepvparena.arena;

public final class ArenaMovementRule {

    private ArenaMovementRule() {
    }

    public static boolean isMovementAllowed(boolean belongsIntoArena, boolean operator,
                                            boolean originInside, boolean destinationInside) {
        if (operator) {
            return true;
        }
        if (originInside == destinationInside) {
            return true;
        }
        return destinationInside == belongsIntoArena;
    }
}
