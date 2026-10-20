package de.simplepvparena.message;

import java.util.Set;

public enum CommandMessage implements MessageKey {

    MENU_ONLY_FOR_PLAYERS("command.menu-only-for-players"),
    ONLY_FOR_PLAYERS("command.only-for-players"),
    SPECTATE_WHILE_FIGHTING("command.spectate.while-fighting"),
    SPECTATE_NO_TRIBUNE_POSITION("command.spectate.no-tribune-position"),
    SPECTATE_WATCHING("command.spectate.watching");

    private final String path;
    private final Set<String> placeholderNames;

    CommandMessage(String path, String... placeholderNames) {
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
