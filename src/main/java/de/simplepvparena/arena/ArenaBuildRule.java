package de.simplepvparena.arena;

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
