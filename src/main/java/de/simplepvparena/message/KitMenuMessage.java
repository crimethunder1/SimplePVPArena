package de.simplepvparena.message;

import java.util.Set;

public enum KitMenuMessage implements MessageKey {

    TITLE("kit-menu.title", "page"),
    INFO_TITLE("kit-menu.tile.info.title"),
    KIT_COUNT("kit-menu.tile.info.kit-count", "count"),
    DEFAULT_KIT("kit-menu.tile.info.default-kit", "kit"),
    NO_DEFAULT_KIT("kit-menu.tile.info.no-default-kit"),
    INFO_HINT("kit-menu.tile.info.hint"),
    KIT_DEFAULT_KIT("kit-menu.tile.kit.default-kit"),
    KIT_LORE("kit-menu.tile.kit.lore"),
    CREATE_TITLE("kit-menu.tile.create.title"),
    CREATE_ASKS_FOR_NAME("kit-menu.tile.create.asks-for-name"),
    CREATE_FIRST_KIT("kit-menu.tile.create.first-kit"),
    WORN_ITEM_COUNT("kit-menu.item-counts.worn", "count", "slotCount"),
    INVENTORY_ITEM_COUNT("kit-menu.item-counts.inventory", "count", "slotCount"),
    INVENTORY_CAPTURE("kit-menu.inventory-capture"),
    NAME_INPUT_DESCRIPTION("kit-menu.name-input.description"),
    NAME_INPUT_PROMPT("kit-menu.name-input.prompt", "rule");

    private final String path;
    private final Set<String> placeholderNames;

    KitMenuMessage(String path, String... placeholderNames) {
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
