package de.simplepvparena.message;

import java.util.Set;

public enum AreaInventoryMessage implements MessageKey {

    KEPT_SAFE("area-inventory.kept-safe"),
    BACK("area-inventory.back"),
    NOT_GIVEN_BACK("area-inventory.not-given-back"),
    DROP_LOCKED("area-inventory.drop-locked"),
    ENDER_CHEST_LOCKED("area-inventory.ender-chest-locked"),
    NOT_WRITTEN("area-inventory.not-written", "player", "reason"),
    NOT_CLEARED("area-inventory.not-cleared", "player", "reason"),
    UNREADABLE("area-inventory.unreadable", "player", "file"),
    MISSING("area-inventory.missing", "player", "file"),
    NOT_RESTORED("area-inventory.not-restored", "player", "reason"),
    PLAYER_DATA_NOT_SAVED("area-inventory.player-data-not-saved", "player", "file", "reason"),
    FILE_NOT_DELETED("area-inventory.file-not-deleted", "player", "file", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    AreaInventoryMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}
