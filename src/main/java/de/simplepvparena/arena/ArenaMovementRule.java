package de.simplepvparena.arena;

public final class ArenaMovementRule {

    private ArenaMovementRule() {
    }

    public static boolean isMovementAllowed(boolean activeFighter, boolean belongsIntoArena, boolean admin,
                                            boolean originInside, boolean destinationInside) {
        if (activeFighter || admin) {
            return true;
        }
        if (originInside == destinationInside) {
            return true;
        }
        return destinationInside == belongsIntoArena;
    }
}
