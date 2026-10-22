package de.simplepvparena.message;

import java.util.Set;

public enum VisualMessage implements MessageKey {

    ITEMS_ADDER_USED("visual.items-adder-used", "section"),
    ITEMS_ADDER_SWITCHED_OFF("visual.items-adder-switched-off", "path"),
    UNKNOWN_FONT_IMAGE("visual.unknown-font-image", "identifier", "path"),
    UNKNOWN_ITEM("visual.unknown-item", "identifier", "path"),
    LOOKUP_FAILED("visual.lookup-failed", "identifier", "path", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    VisualMessage(String path, String... placeholderNames) {
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
