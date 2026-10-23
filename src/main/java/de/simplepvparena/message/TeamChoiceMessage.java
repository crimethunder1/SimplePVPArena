package de.simplepvparena.message;

import java.util.Set;

public enum TeamChoiceMessage implements MessageKey {

    REGISTER_FIRST("team-choice.register-first"),
    RANDOM_MODE("team-choice.random-mode"),
    CLOSED("team-choice.closed"),
    TEAM_FULL("team-choice.team-full"),
    ASSIGNED_BY_ADMIN("team-choice.assigned-by-admin", "team"),
    NOT_IN_A_TEAM("team-choice.not-in-a-team"),
    JOINED("team-choice.joined", "team"),
    MEMBER_JOINED("team-choice.member-joined", "player"),
    LEFT("team-choice.left", "team"),
    MENU_TITLE("team-choice.menu.title"),
    BED_TITLE("team-choice.menu.tile.bed.title", "colour"),
    BED_NO_MEMBERS("team-choice.menu.tile.bed.no-members"),
    BED_PLACES("team-choice.menu.tile.bed.places", "taken", "count"),
    BED_CLICK_TO_JOIN("team-choice.menu.tile.bed.click-to-join"),
    BED_YOUR_TEAM("team-choice.menu.tile.bed.your-team"),
    INFO_TITLE("team-choice.menu.tile.info.title"),
    INFO_REGISTERED_PLAYERS("team-choice.menu.tile.info.registered-players", "count"),
    INFO_TEAM_SIZE("team-choice.menu.tile.info.team-size", "count"),
    INFO_HINT("team-choice.menu.tile.info.hint"),
    LEAVE_TITLE("team-choice.menu.tile.leave.title"),
    LEAVE_LORE("team-choice.menu.tile.leave.lore", "team");

    private final String path;
    private final Set<String> placeholderNames;

    TeamChoiceMessage(String path, String... placeholderNames) {
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
