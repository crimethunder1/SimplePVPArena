package de.simplepvparena.message;

import java.util.Set;

public enum ArenaHeightMessage implements MessageKey {

    LOWEST_POINT("arena-height.end.lowest"),
    HIGHEST_POINT("arena-height.end.highest"),
    NOTHING_MARKED("arena-height.description.nothing-marked"),
    BOTH_MARKED("arena-height.description.both-marked", "lowestY", "highestY"),
    ONLY_LOWEST_MARKED("arena-height.description.only-lowest-marked", "lowestY"),
    ONLY_HIGHEST_MARKED("arena-height.description.only-highest-marked", "highestY"),
    INCOMPLETE_ONLY_LOWEST("arena-height.incomplete.only-lowest", "markedY"),
    INCOMPLETE_ONLY_HIGHEST("arena-height.incomplete.only-highest", "markedY"),
    REFUSAL_LOWEST("arena-height.refusal.lowest", "otherY", "blockY"),
    REFUSAL_HIGHEST("arena-height.refusal.highest", "otherY", "blockY"),
    MARKED_LOWEST("arena-height.marked.lowest", "blockY", "markers"),
    MARKED_HIGHEST("arena-height.marked.highest", "blockY", "markers"),
    MOVED_ON_TO_LOWEST("arena-height.moved-on.lowest"),
    MOVED_ON_TO_HIGHEST("arena-height.moved-on.highest"),
    COMPLETE_PAIR("arena-height.complete-pair", "action", "lowestY", "highestY"),
    SWITCHED_TO_LOWEST("arena-height.switched.lowest", "markers"),
    SWITCHED_TO_HIGHEST("arena-height.switched.highest", "markers"),
    TILE_NEXT_BLOCK_LOWEST("arena-height.tile.next-block-lowest"),
    TILE_NEXT_BLOCK_HIGHEST("arena-height.tile.next-block-highest"),
    TILE_COMPLETE("arena-height.tile.complete"),
    TILE_INCOMPLETE("arena-height.tile.incomplete"),
    ITEM_NAME("arena-height.item.name", "end"),
    ITEM_LORE_LOWEST("arena-height.item.lore-lowest"),
    ITEM_LORE_HIGHEST("arena-height.item.lore-highest");

    private final String path;
    private final Set<String> placeholderNames;

    ArenaHeightMessage(String path, String... placeholderNames) {
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
