package de.simplepvparena.arena;

import java.util.Objects;

public record ArenaGapTarget(String worldName, int blockX, int blockY, int blockZ) {

    public ArenaGapTarget {
        Objects.requireNonNull(worldName, "worldName");
    }

    public static ArenaGapTarget fromDiagnostics(String worldName, int seedY, ArenaFillDiagnostics diagnostics) {
        return new ArenaGapTarget(worldName, diagnostics.getNearestFrontX(), seedY,
                diagnostics.getNearestFrontZ());
    }

    public String describe() {
        return "x/y/z " + blockX + "/" + blockY + "/" + blockZ + " in " + worldName;
    }
}
