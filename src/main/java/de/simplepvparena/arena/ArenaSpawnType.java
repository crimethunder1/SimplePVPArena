package de.simplepvparena.arena;

public enum ArenaSpawnType {

    FIRST_TEAM("first team spawn", true, "inside the border wall, where the first team starts"),
    SECOND_TEAM("second team spawn", true, "inside the border wall, where the second team starts"),
    TRIBUNE("tribune spawn", false, "outside the border wall, where the spectators wait");

    private final String displayName;
    private final boolean insideFightArea;
    private final String placementHint;

    ArenaSpawnType(String displayName, boolean insideFightArea, String placementHint) {
        this.displayName = displayName;
        this.insideFightArea = insideFightArea;
        this.placementHint = placementHint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean belongsIntoFightArea() {
        return insideFightArea;
    }

    public String getPlacementHint() {
        return placementHint;
    }
}
