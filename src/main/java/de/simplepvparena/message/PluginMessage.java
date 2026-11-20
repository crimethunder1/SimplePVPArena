package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public enum PluginMessage implements MessageKey {

    KITS_NOT_LOADED("plugin.kits-not-loaded", "reason"),
    AUTO_MATCH_WINS_NOT_LOADED("plugin.auto-match-wins-not-loaded", "file"),
    AUTO_MATCH_WINS_NOT_SAVED("plugin.auto-match-wins-not-saved", "file"),
    IMPORT_FOLDER_NOT_CREATED("plugin.import-folder-not-created", "folder"),
    ARENA_BASELINE_LOADED("plugin.arena-baseline-loaded", "file"),
    ARENA_BASELINE_NOT_LOADED("plugin.arena-baseline-not-loaded", "file"),
    ARENA_BASELINE_NOT_SAVED("plugin.arena-baseline-not-saved", "file"),
    ITEMS_ADDER_CONTENT_INSTALLED("plugin.items-adder-content-installed"),
    ITEMS_ADDER_CONTENT_NOT_INSTALLED("plugin.items-adder-content-not-installed"),
    ITEMS_ADDER_PACK_REBUILT("plugin.items-adder-pack-rebuilt"),
    ITEMS_ADDER_CONTENT_UP_TO_DATE("plugin.items-adder-content-up-to-date"),
    ITEMS_ADDER_CONTENT_NO_FOLDER("plugin.items-adder-content-no-folder"),
    ITEMS_ADDER_NOT_USED("plugin.items-adder-not-used"),
    ITEMS_ADDER_NOT_READY("plugin.items-adder-not-ready");

    private final String path;
    private final Set<String> placeholderNames;

    PluginMessage(String path, String... placeholderNames) {
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
