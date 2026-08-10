package de.simplepvparena.arena;

public final class ArenaMovementRule {

    private ArenaMovementRule() {
    }

    public static boolean isMovementAllowed(boolean belongsIntoArena, boolean originInside,
                                            boolean destinationInside) {
        if (originInside == destinationInside) {
            return true;
        }
        return destinationInside == belongsIntoArena;
    }
}
