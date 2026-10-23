package de.simplepvparena.message;

import java.util.Set;

public enum ListenerMessage implements MessageKey {

    BREAK_OUTSIDE_THE_ARENA("listener.break.outside-the-arena"),
    BREAK_ORIGINAL_ARENA("listener.break.original-arena"),
    BREAK_NOT_A_FIGHTER("listener.break.not-a-fighter"),
    BUILD_OUTSIDE_THE_ARENA("listener.build.outside-the-arena"),
    BUILD_NOT_A_FIGHTER("listener.build.not-a-fighter"),
    REJOINED_DURING_MATCH("listener.rejoined-during-match", "match"),
    REJOINED_NOT_CHOSEN("listener.rejoined-not-chosen", "match"),
    NOT_SENT_TO_TRIBUNE("listener.not-sent-to-tribune", "player"),
    SPECTATOR_MODE_NOT_SWITCHED("listener.spectator-mode-not-switched", "player");

    private final String path;
    private final Set<String> placeholderNames;

    ListenerMessage(String path, String... placeholderNames) {
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
