package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ArenaWorldBuildRule {

    private ArenaWorldBuildRule() {
    }

    public static boolean isChangeAllowed(boolean arenaAdmin, boolean insideFightArea, boolean insideEffectArea) {
        return !insideEffectArea || isChangeAllowed(arenaAdmin, insideFightArea);
    }

    public static boolean isChangeAllowed(boolean arenaAdmin, boolean insideFightArea) {
        return insideFightArea || arenaAdmin;
    }
}
