package de.simplepvparena.message;

import java.util.Set;

public enum PluginMessage implements MessageKey {

    KITS_NOT_LOADED("plugin.kits-not-loaded", "reason");

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
