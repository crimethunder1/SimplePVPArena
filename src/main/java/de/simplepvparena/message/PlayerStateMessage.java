package de.simplepvparena.message;

import java.util.Set;

public enum PlayerStateMessage implements MessageKey {

    NOT_FOUND("player-state.not-found", "player"),
    UNREADABLE_CLEARED("player-state.unreadable-cleared", "player", "file"),
    UNREADABLE_KEPT("player-state.unreadable-kept", "player", "file"),
    NOT_RESTORED("player-state.not-restored", "player", "reason"),
    NOT_WRITTEN("player-state.not-written", "player", "reason"),
    FILE_NOT_DELETED("player-state.file-not-deleted", "player", "reason"),
    SET_ASIDE("player-state.set-aside", "player", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    PlayerStateMessage(String path, String... placeholderNames) {
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
