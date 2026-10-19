package de.simplepvparena.message;

import java.util.Set;

public enum KitMessage implements MessageKey {

    NAME_RULE("kit.name-rule", "maximumLength"),
    NAME_MISSING("kit.name-missing", "rule"),
    NAME_NOT_ALLOWED("kit.name-not-allowed", "kit", "rule"),
    ALREADY_EXISTS("kit.already-exists", "kit"),
    ALREADY_DEFAULT_KIT("kit.already-default-kit", "kit"),
    LAST_KIT("kit.last-kit", "kit"),
    DEFAULT_KIT_NOT_DELETABLE("kit.default-kit-not-deletable", "kit"),
    NAME_USED_TWICE("kit.name-used-twice", "kit"),
    NO_DEFAULT_KIT_NAMED("kit.no-default-kit-named"),
    DEFAULT_KIT_UNKNOWN("kit.default-kit-unknown", "kit"),
    UNKNOWN_KIT("kit.unknown-kit", "kit"),
    WRITE_FAILED("kit.write-failed", "file", "reason"),
    DEFINITION_EMPTY("kit.definition.empty"),
    DEFINITION_WORN_ITEMS("kit.definition.worn-items", "count", "slotCount"),
    DEFINITION_INVENTORY_ITEMS("kit.definition.inventory-items", "count", "slotCount"),
    SAVED("kit.save.saved", "kit", "count", "wornCount", "inventoryCount"),
    SAVED_DEFAULT_KIT_WRITTEN("kit.save.default-kit-written", "file"),
    SAVED_OTHER_KIT_WRITTEN("kit.save.other-kit-written", "file"),
    UNREADABLE_ITEMS("kit.save.unreadable-items", "count"),
    EMPTY_INVENTORY_CREATION("kit.save.empty-inventory-creation"),
    EMPTY_INVENTORY_REPLACEMENT("kit.save.empty-inventory-replacement"),
    LOADER_UNKNOWN_REASON("kit.loader.unknown-reason"),
    LOADER_FILE_UNREADABLE("kit.loader.file-unreadable", "file", "reason", "keptFile"),
    LOADER_FILE_SET_ASIDE("kit.loader.file-set-aside", "file", "keptFile"),
    LOADER_LEGACY_KIT_UNREADABLE("kit.loader.legacy-kit-unreadable", "file"),
    LOADER_LEGACY_KIT_TAKEN_OVER("kit.loader.legacy-kit-taken-over", "file", "kit");

    private final String path;
    private final Set<String> placeholderNames;

    KitMessage(String path, String... placeholderNames) {
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
