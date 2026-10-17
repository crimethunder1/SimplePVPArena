package de.simplepvparena.arena;

public final class ArenaWorldBuildRule {

    public static final String REFUSAL = "Only admins change blocks outside the arena";

    private ArenaWorldBuildRule() {
    }

    public static boolean isChangeAllowed(boolean arenaAdmin, boolean insideFightArea, boolean insideEffectArea) {
        return !insideEffectArea || isChangeAllowed(arenaAdmin, insideFightArea);
    }

    public static boolean isChangeAllowed(boolean arenaAdmin, boolean insideFightArea) {
        return insideFightArea || arenaAdmin;
    }
}
