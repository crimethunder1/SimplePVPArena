package de.simplepvparena.arena;

public enum ArenaSpawnType {

    FIRST_TEAM("first team spawn"),
    SECOND_TEAM("second team spawn"),
    TRIBUNE("tribune spawn");

    private final String displayName;

    ArenaSpawnType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
