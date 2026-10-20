package de.simplepvparena.message;

import java.util.Set;

public enum BorderMessage implements MessageKey {

    MARKED("border.mark.marked"),
    MARK_REMOVED("border.mark.removed"),
    MARK_REPORT("border.mark.report", "state", "blockX", "blockY", "blockZ"),
    SELECTOR_NO_LONGER_ALLOWED("border.selector.no-longer-allowed"),
    OUTLINE_BLOCKS("border.outline.blocks", "count", "lines"),
    OUTLINE_LINES("border.outline.lines", "count"),
    OUTLINE_TRUNCATED("border.outline.truncated", "outline");

    private final String path;
    private final Set<String> placeholderNames;

    BorderMessage(String path, String... placeholderNames) {
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
