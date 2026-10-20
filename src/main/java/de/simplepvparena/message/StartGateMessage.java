package de.simplepvparena.message;

import java.util.Set;

public enum StartGateMessage implements MessageKey {

    RELEASE_TITLE("start-gate.countdown.release-title"),
    COUNTING_SUBTITLE("start-gate.countdown.counting"),
    RELEASE_SUBTITLE("start-gate.countdown.open"),
    DURATION("start-gate.duration", "count"),
    ITEM_NAME("start-gate.item.name"),
    ITEM_LORE("start-gate.item.lore", "duration"),
    TILE_NO_BLOCK("start-gate.tile.no-block"),
    TILE_BLOCKS("start-gate.tile.blocks", "count"),
    TILE_COMES_DOWN_OVER("start-gate.tile.comes-down-over", "duration");

    private final String path;
    private final Set<String> placeholderNames;

    StartGateMessage(String path, String... placeholderNames) {
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
