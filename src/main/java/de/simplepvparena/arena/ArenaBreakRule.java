package de.simplepvparena.arena;

import org.bukkit.Material;

import java.util.Optional;

public final class ArenaBreakRule {

    private ArenaBreakRule() {
    }

    public static boolean isBreakingAllowed(boolean activeFighter, boolean arenaAdmin, boolean insideFightArea,
                                            boolean partOfTheOriginalArena) {
        if (!insideFightArea || arenaAdmin) {
            return true;
        }
        return activeFighter && !partOfTheOriginalArena;
    }

    public static boolean isPartOfTheOriginalArena(Optional<ArenaBaselineSnapshot> baseline, int blockX, int blockY,
                                                   int blockZ, Material currentMaterial) {
        return baseline
                .map(snapshot -> snapshot.holdsCapturedMaterial(blockX, blockY, blockZ, currentMaterial))
                .orElse(true);
    }
}
