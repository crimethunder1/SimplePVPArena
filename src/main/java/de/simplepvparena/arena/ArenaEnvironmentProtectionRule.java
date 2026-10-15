package de.simplepvparena.arena;

import java.util.Optional;

public final class ArenaEnvironmentProtectionRule {

    private ArenaEnvironmentProtectionRule() {
    }

    public static boolean isProtected(Optional<ArenaShape> fightArea, int capturedFloorY, String worldName,
                                      int blockX, int blockY, int blockZ) {
        if (fightArea.isEmpty() || !fightArea.get().getWorldName().equals(worldName)) {
            return false;
        }
        return !fightArea.get().containsCell(blockX, blockZ) || blockY < capturedFloorY;
    }

    public static int findCapturedFloorY(ArenaShape fightArea, Optional<ArenaBaselineSnapshot> baseline) {
        return baseline
                .map(snapshot -> snapshot.getCapturedVolume().getFloorY())
                .orElse(fightArea.getFloorY());
    }
}
