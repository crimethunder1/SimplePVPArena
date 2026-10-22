package de.simplepvparena.message;

import java.util.Set;

public enum RegistrationMessage implements MessageKey {

    FILE_SET_ASIDE("registration.file.set-aside", "file", "reason", "brokenFile"),
    FILE_NOT_SET_ASIDE("registration.file.not-set-aside", "file", "reason"),
    REGISTERED("registration.registered"),
    ALREADY_REGISTERED("registration.already-registered"),
    UNREGISTERED("registration.unregistered"),
    NOT_REGISTERED("registration.not-registered"),
    CLOSED("registration.closed"),
    TOOL_NAME("registration.tool.name"),
    TOOL_LORE("registration.tool.lore"),
    STATION_ADDED("registration.station.added"),
    STATION_REMOVED("registration.station.removed"),
    CLICK_A_BUTTON("registration.station.click-a-button"),
    STATION_PROTECTED("registration.station.protected"),
    FORMING_TOURNAMENT_IN_PROGRESS("registration.forming.tournament-in-progress"),
    FORMING_AUTO_MATCH("registration.forming.auto-match"),
    FORMING_TOO_FEW_PLAYERS("registration.forming.too-few-players", "count", "minimum"),
    FORMING_FORMED("registration.forming.formed", "count", "players"),
    FORMING_PLAYING_WITH_MATES("registration.forming.playing-with-mates", "team", "mates"),
    FORMING_PLAYING_ALONE("registration.forming.playing-alone", "team");

    private final String path;
    private final Set<String> placeholderNames;

    RegistrationMessage(String path, String... placeholderNames) {
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
