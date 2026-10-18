package de.simplepvparena.message;

import java.util.Set;

public enum CommonMessage implements MessageKey {

    PREFIX("message.prefix"),
    LIST_AND("common.list.and", "items", "last"),
    LIST_MORE("common.list.more", "items", "count"),
    LANGUAGE_FILE_UNREADABLE("common.language-file-unreadable", "file", "reason"),
    NO_TOURNAMENT_IN_PROGRESS("common.no-tournament-in-progress"),
    TOURNAMENT_ALREADY_IN_PROGRESS("common.tournament-already-in-progress"),
    ASKS_FOR_CONFIRMATION("common.asks-for-confirmation"),
    MENU_CLOSES_TO_TYPE("common.menu-closes-to-type"),
    MENU_CLOSES_TO_READ("common.menu-closes-to-read"),
    ROUND_TIMER_KEEPS_TIME("common.round-timer-keeps-time"),
    COMMAND_NOT_ALLOWED("common.command-not-allowed");

    private final String path;
    private final Set<String> placeholderNames;

    CommonMessage(String path, String... placeholderNames) {
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
