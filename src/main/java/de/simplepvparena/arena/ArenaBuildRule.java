package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ArenaBuildRule {

    private ArenaBuildRule() {
    }

    public static boolean isPlacementAllowed(boolean activeFighter, boolean arenaAdmin, boolean insideFightArea) {
        if (!insideFightArea) {
            return true;
        }
        return activeFighter || arenaAdmin;
    }
}
