package de.simplepvparena.message;

import java.util.Set;

public enum RegistrationMenuMessage implements MessageKey {

    TITLE("registrations-menu.title", "page"),
    REGISTERED_PLAYERS("registrations-menu.registered-players", "count"),
    NOBODY_REGISTERED("registrations-menu.nobody-registered"),
    TOOL_HANDED_OUT("registrations-menu.tool-handed-out"),
    TOOL_INVENTORY_FULL("registrations-menu.tool-inventory-full"),
    REGISTRATION_REMOVED("registrations-menu.registration-removed", "player"),
    REGISTRATIONS_CLEARED("registrations-menu.registrations-cleared"),
    COLOURED_TEAMS_REMOVED("registrations-menu.coloured-teams-removed", "count"),
    FORM_CONFIRMATION_REPLACES("registrations-menu.form-confirmation.replaces"),
    FORM_CONFIRMATION_TEAMS("registrations-menu.form-confirmation.teams", "count"),
    CLEAR_CONFIRMATION_REGISTRATIONS("registrations-menu.clear-confirmation.registrations", "count"),
    CLEAR_CONFIRMATION_TEAMS_STAY("registrations-menu.clear-confirmation.teams-stay"),
    CLEAR_CONFIRMATION_COLOURED_TEAMS("registrations-menu.clear-confirmation.coloured-teams"),
    PLAYER_LORE("registrations-menu.tile.player.lore"),
    INFO_TITLE("registrations-menu.tile.info.title"),
    TEAM_COUNT("registrations-menu.tile.info.team-count", "count", "teamSize"),
    TOOL_TITLE("registrations-menu.tile.tool.title"),
    TOOL_LORE("registrations-menu.tile.tool.lore"),
    FORM_TITLE("registrations-menu.tile.form.title"),
    FORM_LORE("registrations-menu.tile.form.lore"),
    CLEAR_TITLE("registrations-menu.tile.clear.title"),
    CLEAR_LORE("registrations-menu.tile.clear.lore"),
    CLEAR_LORE_FREE_CHOICE("registrations-menu.tile.clear.lore-free-choice"),
    TEAM_MODE_TITLE("registrations-menu.tile.team-mode.title"),
    TEAM_MODE_FREE_CHOICE("registrations-menu.tile.team-mode.free-choice"),
    TEAM_MODE_RANDOM("registrations-menu.tile.team-mode.random"),
    TEAM_MODE_SWITCH("registrations-menu.tile.team-mode.switch"),
    TEAM_MODE_NOT_WRITTEN("registrations-menu.team-mode-not-written", "mode", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    RegistrationMenuMessage(String path, String... placeholderNames) {
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
