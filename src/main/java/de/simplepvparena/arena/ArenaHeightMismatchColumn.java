package de.simplepvparena.arena;

public record ArenaHeightMismatchColumn(int blockX, int blockZ, int lowestBorderY, int seedY) {

    public ArenaHeightMismatchColumn {
        if (lowestBorderY <= seedY) {
            throw new IllegalArgumentException(
                    "a column whose lowest border block is at or below the seed height bars the fill");
        }
    }

    public int heightAboveSeed() {
        return lowestBorderY - seedY;
    }

    public String describe() {
        return "x/z " + blockX + "/" + blockZ + " (lowest border block at y " + lowestBorderY + ", "
                + heightAboveSeed() + " above you)";
    }
}
