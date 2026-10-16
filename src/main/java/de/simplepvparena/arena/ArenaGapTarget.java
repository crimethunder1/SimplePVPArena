package de.simplepvparena.arena;

import java.util.Objects;

public record ArenaGapTarget(String worldName, int blockX, int blockY, int blockZ, Reason reason) {

    public enum Reason {

        FILL_FRONT("The flood fill got that far before it was stopped",
                "That is the column the fill reached first of the ones it was stopped on, not a hole "
                        + "anybody found. Start looking around there",
                "This is where the flood fill was when it was stopped, look for a missing green connection "
                        + "line between two border blocks around you"),

        BORDER_TOO_HIGH("A border block stands there, above your calculation height",
                "A border block stands in that column, but above the height you ran the calculation at, "
                        + "so it never barred the fill. This is not a guess, it is a piece of wall that "
                        + "does not close",
                "This is a column where you built the wall, above the height you calculated at, so the "
                        + "fill walked underneath it. Build it down to your own height here, or run the "
                        + "calculation again from a position at least as high as these blocks");

        private final String loreLine;
        private final String handOutHint;
        private final String arrivalHint;

        Reason(String loreLine, String handOutHint, String arrivalHint) {
            this.loreLine = loreLine;
            this.handOutHint = handOutHint;
            this.arrivalHint = arrivalHint;
        }

        public String getLoreLine() {
            return loreLine;
        }

        public String getHandOutHint() {
            return handOutHint;
        }

        public String getArrivalHint() {
            return arrivalHint;
        }
    }

    public ArenaGapTarget {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(reason, "reason");
    }

    public ArenaGapTarget(String worldName, int blockX, int blockY, int blockZ) {
        this(worldName, blockX, blockY, blockZ, Reason.FILL_FRONT);
    }

    public static ArenaGapTarget fromDiagnostics(String worldName, int seedY, ArenaFillDiagnostics diagnostics) {
        return diagnostics.getNearestHeightMismatchColumn()
                .map(column -> new ArenaGapTarget(worldName, column.blockX(), seedY, column.blockZ(),
                        Reason.BORDER_TOO_HIGH))
                .orElseGet(() -> new ArenaGapTarget(worldName, diagnostics.getNearestFrontX(), seedY,
                        diagnostics.getNearestFrontZ(), Reason.FILL_FRONT));
    }

    public String describe() {
        return "x/y/z " + blockX + "/" + blockY + "/" + blockZ + " in " + worldName;
    }
}
