package de.simplepvparena.message;

import java.util.Set;

public enum TeamMenuMessage implements MessageKey {

    TITLE("teams-menu.title", "page"),
    TEAM_MEMBERS("teams-menu.tile.team.members", "members"),
    TEAM_LORE("teams-menu.tile.team.lore"),
    INFO_TITLE("teams-menu.tile.info.title"),
    REGISTERED_TEAMS("teams-menu.tile.info.registered-teams", "count"),
    TEAM_SIZE("teams-menu.tile.info.team-size", "matchUp"),
    INFO_HINT("teams-menu.tile.info.hint"),
    SMALLER_TITLE("teams-menu.tile.smaller.title"),
    SMALLER_LORE("teams-menu.tile.smaller.lore", "count"),
    LARGER_TITLE("teams-menu.tile.larger.title"),
    LARGER_LORE("teams-menu.tile.larger.lore", "count"),
    ADD_TITLE("teams-menu.tile.add.title"),
    ADD_LORE("teams-menu.tile.add.lore", "usage"),
    REGISTRATIONS_TITLE("teams-menu.tile.registrations.title"),
    REGISTRATIONS_LORE("teams-menu.tile.registrations.lore"),
    SMALLEST_TEAM_SIZE("teams-menu.smallest-team-size", "minimum"),
    LARGEST_TEAM_SIZE("teams-menu.largest-team-size", "maximum"),
    TEAM_SIZE_CHANGED("teams-menu.team-size-changed", "matchUp"),
    TEAM_SIZE_NOT_WRITTEN("teams-menu.team-size-not-written", "matchUp", "reason"),
    ADD_INPUT_DESCRIPTION("teams-menu.add-input.description"),
    ADD_INPUT_TYPE("teams-menu.add-input.type", "usage"),
    ADD_INPUT_EXACT_SIZE("teams-menu.add-input.exact-size", "count", "rule"),
    ADD_INPUT_SIZE_RANGE("teams-menu.add-input.size-range", "minimum", "maximum", "rule"),
    ADD_INPUT_EXAMPLES("teams-menu.add-input.examples"),
    ADD_INPUT_PLAYERS_KNOWN("teams-menu.add-input.players-known"),
    TEAM_REGISTERED("teams-menu.team-registered", "team", "members"),
    REGISTRATION_FAILED("teams-menu.registration-failed", "team", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    TeamMenuMessage(String path, String... placeholderNames) {
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
