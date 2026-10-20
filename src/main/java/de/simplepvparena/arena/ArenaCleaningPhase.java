package de.simplepvparena.arena;

public enum ArenaCleaningPhase {

    FLOOD(255, 48, 48, false),

    CLEAR(64, 255, 64, true);

    private final int red;
    private final int green;
    private final int blue;
    private final boolean writesBlocks;

    ArenaCleaningPhase(int red, int green, int blue, boolean writesBlocks) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.writesBlocks = writesBlocks;
    }

    public int red() {
        return red;
    }

    public int green() {
        return green;
    }

    public int blue() {
        return blue;
    }

    public boolean writesBlocks() {
        return writesBlocks;
    }
}
