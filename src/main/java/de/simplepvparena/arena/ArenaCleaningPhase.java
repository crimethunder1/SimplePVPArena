package de.simplepvparena.arena;

public enum ArenaCleaningPhase {

    FLOOD(255, 48, 48, false, "the red flood"),

    CLEAR(64, 255, 64, true, "the green clearing");

    private final int red;
    private final int green;
    private final int blue;
    private final boolean writesBlocks;
    private final String description;

    ArenaCleaningPhase(int red, int green, int blue, boolean writesBlocks, String description) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.writesBlocks = writesBlocks;
        this.description = description;
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

    public String describe() {
        return description;
    }
}
