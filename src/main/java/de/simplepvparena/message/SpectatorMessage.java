package de.simplepvparena.message;

import java.util.Set;

public enum SpectatorMessage implements MessageKey {

    ITEM_NAME("spectator.item.name"),
    SWITCH_WHILE_FIGHTING("spectator.switch-while-fighting"),
    NOT_WATCHING("spectator.not-watching"),
    TRIBUNE_SPAWN_NOT_SET("spectator.tribune-spawn-not-set");

    private final String path;
    private final Set<String> placeholderNames;

    SpectatorMessage(String path, String... placeholderNames) {
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
