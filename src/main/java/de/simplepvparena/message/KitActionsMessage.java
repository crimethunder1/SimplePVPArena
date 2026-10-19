package de.simplepvparena.message;

import java.util.Set;

public enum KitActionsMessage implements MessageKey {

    TITLE("kit-actions-menu.title", "kit"),
    INFO_KIT_GONE("kit-actions-menu.tile.info.kit-gone"),
    INFO_DEFAULT_KIT("kit-actions-menu.tile.info.default-kit"),
    INFO_NOT_DEFAULT_KIT("kit-actions-menu.tile.info.not-default-kit"),
    DEFAULT_TITLE("kit-actions-menu.tile.default.title"),
    DEFAULT_ALREADY_DEFAULT_TITLE("kit-actions-menu.tile.default.already-default-title"),
    DEFAULT_LORE("kit-actions-menu.tile.default.lore"),
    REPLACE_TITLE("kit-actions-menu.tile.replace.title"),
    DELETE_TITLE("kit-actions-menu.tile.delete.title"),
    DELETE_LORE("kit-actions-menu.tile.delete.lore", "kit"),
    REPLACEMENT_INVENTORY_BECOMES_KIT("kit-actions-menu.replacement.inventory-becomes-kit", "kit"),
    REPLACEMENT_MATCH_RUNNING("kit-actions-menu.replacement.match-running"),
    REPLACE_CONFIRMATION_ITEMS_GONE("kit-actions-menu.replace-confirmation.items-gone"),
    DELETE_CONFIRMATION("kit-actions-menu.delete-confirmation", "kit"),
    DEFAULT_KIT_SET("kit-actions-menu.default-kit-set", "kit"),
    KIT_DELETED("kit-actions-menu.kit-deleted", "kit");

    private final String path;
    private final Set<String> placeholderNames;

    KitActionsMessage(String path, String... placeholderNames) {
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
