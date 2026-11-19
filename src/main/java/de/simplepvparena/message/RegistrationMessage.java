package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
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
    STATION_BUTTON_NAME("registration.station-button.name"),
    STATION_BUTTON_LORE("registration.station-button.lore"),
    MACHINE_ITEM_NAME("registration.machine.item-name"),
    MACHINE_ITEM_LORE("registration.machine.item-lore"),
    MACHINE_PLACED("registration.machine.placed"),
    MACHINE_REMOVED("registration.machine.removed"),
    MACHINE_NO_ROOM("registration.machine.no-room"),
    MACHINES_NOT_LOADED("registration.machine.not-loaded", "file"),
    MACHINES_NOT_SAVED("registration.machine.not-saved", "file"),
    STATION_ADDED("registration.station.added"),
    STATION_REMOVED("registration.station.removed"),
    CLICK_A_BUTTON("registration.station.click-a-button"),
    STATION_PROTECTED("registration.station.protected"),
    STATION_WITHOUT_BUTTON("registration.station.without-button", "world", "x", "y", "z"),
    DISPLAY_TITLE("registration.display.title"),
    DISPLAY_HINT("registration.display.hint"),
    DISPLAY_COUNT("registration.display.count", "count"),
    FORMING_TOURNAMENT_IN_PROGRESS("registration.forming.tournament-in-progress"),
    FORMING_AUTO_MATCH("registration.forming.auto-match"),
    FORMING_TOO_FEW_PLAYERS("registration.forming.too-few-players", "count", "minimum"),
    FORMING_FORMED("registration.forming.formed", "count", "players"),
    FORMING_PLAYING_WITH_MATES("registration.forming.playing-with-mates", "team", "mates"),
    FORMING_PLAYING_ALONE("registration.forming.playing-alone", "team"),
    IMPORT_USAGE("registration.import.usage"),
    IMPORT_NO_FILES("registration.import.no-files", "folder"),
    IMPORT_FILES("registration.import.files", "folder", "files"),
    IMPORT_ALREADY_RUNNING("registration.import.already-running"),
    IMPORT_FILE_NOT_FOUND("registration.import.file-not-found", "file", "folder"),
    IMPORT_FILE_NOT_READ("registration.import.file-not-read", "file", "reason"),
    IMPORT_NO_PLAYERS("registration.import.no-players", "file"),
    IMPORT_INVALID_NAME("registration.import.invalid-name", "line", "name"),
    IMPORT_NAMED_TWICE("registration.import.named-twice", "line", "name"),
    IMPORT_GROUP_TOO_LARGE("registration.import.group-too-large", "line", "players", "size"),
    IMPORT_LOOKING_UP("registration.import.looking-up", "count"),
    IMPORT_LOOKUP_FAILED("registration.import.lookup-failed", "reason"),
    IMPORT_UNKNOWN_PLAYERS("registration.import.unknown-players", "count", "players"),
    IMPORT_TOO_FEW_TEAMS("registration.import.too-few-teams", "count", "minimum"),
    IMPORT_DONE("registration.import.done", "count", "players", "file");

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
