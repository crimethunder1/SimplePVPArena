package de.simplepvparena.arena;

public enum ArenaHeightMarkerMode {

    LOWEST("lowest point", "the floor of the cleaned fight area"),
    HIGHEST("highest point", "the ceiling of the cleaned fight area");

    private final String displayName;
    private final String meaning;

    ArenaHeightMarkerMode(String displayName, String meaning) {
        this.displayName = displayName;
        this.meaning = meaning;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMeaning() {
        return meaning;
    }

    public ArenaHeightMarkerMode toggle() {
        return this == LOWEST ? HIGHEST : LOWEST;
    }
}
