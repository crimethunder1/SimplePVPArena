package de.simplepvparena.message;

import java.util.Set;

public enum TeamActionsMessage implements MessageKey {

    TITLE("team-actions-menu.title", "team"),
    NOT_REGISTERED("team-actions-menu.not-registered"),
    INFO_MEMBERS("team-actions-menu.tile.info.members", "members"),
    INFO_PLAYER_COUNT("team-actions-menu.tile.info.player-count", "count", "teamSize"),
    INFO_NOT_REGISTERED("team-actions-menu.tile.info.not-registered"),
    RENAME_TITLE("team-actions-menu.tile.rename.title"),
    RENAME_LORE("team-actions-menu.tile.rename.lore"),
    ADD_PLAYER_TITLE("team-actions-menu.tile.add-player.title"),
    ADD_PLAYER_LORE("team-actions-menu.tile.add-player.lore"),
    ADD_PLAYER_TEAM_FULL("team-actions-menu.tile.add-player.team-full", "count", "teamSize"),
    REMOVE_PLAYER_TITLE("team-actions-menu.tile.remove-player.title"),
    REMOVE_PLAYER_LORE("team-actions-menu.tile.remove-player.lore", "team"),
    REMOVE_TITLE("team-actions-menu.tile.remove.title"),
    REMOVE_LORE("team-actions-menu.tile.remove.lore", "team"),
    REMOVE_CONFIRMATION("team-actions-menu.remove-confirmation", "team"),
    TEAM_FULL("team-actions-menu.team-full", "count", "team", "teamSize"),
    RENAME_INPUT_DESCRIPTION("team-actions-menu.rename-input.description", "team"),
    RENAME_INPUT_PROMPT("team-actions-menu.rename-input.prompt", "rule"),
    TEAM_RENAMED("team-actions-menu.team-renamed", "formerName", "newName"),
    RENAME_FAILED("team-actions-menu.rename-failed", "team", "reason"),
    ADD_PLAYER_INPUT_DESCRIPTION("team-actions-menu.add-player-input.description", "team"),
    ADD_PLAYER_INPUT_TYPE("team-actions-menu.add-player-input.type"),
    ADD_PLAYER_INPUT_PLAYER_COUNT("team-actions-menu.add-player-input.player-count", "count", "team",
            "memberCount"),
    ADD_PLAYER_INPUT_HINTS("team-actions-menu.add-player-input.hints"),
    PLAYER_JOINED("team-actions-menu.player-joined", "player", "team"),
    JOIN_FAILED("team-actions-menu.join-failed", "player", "team", "reason"),
    UNKNOWN_TEAM("team-actions-menu.unknown-team", "team"),
    TEAM_REMOVED("team-actions-menu.team-removed", "team"),
    MEMBERS_TITLE("team-members-menu.title", "team"),
    MEMBERS_INFO_PLAYER_COUNT("team-members-menu.tile.info.player-count", "count", "teamSize"),
    MEMBERS_INFO_HINT("team-members-menu.tile.info.hint"),
    MEMBERS_INFO_NOT_REGISTERED("team-members-menu.tile.info.not-registered"),
    MEMBER_LORE("team-members-menu.tile.member.lore"),
    PLAYER_LEFT("team-members-menu.player-left", "player", "team"),
    LEAVE_FAILED("team-members-menu.leave-failed", "player", "team", "reason");

    private final String path;
    private final Set<String> placeholderNames;

    TeamActionsMessage(String path, String... placeholderNames) {
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
