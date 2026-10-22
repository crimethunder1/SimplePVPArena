package de.simplepvparena.message;

import java.util.Set;

public enum UnequalTeamVoteMessage implements MessageKey {

    MENU_TITLE("unequal-team-vote-menu.title", "match"),
    VOTE_OVER("unequal-team-vote-menu.vote-over", "match"),
    PLAYER_VOTES("unequal-team-vote-menu.tile.player.votes", "count"),
    PLAYER_LORE("unequal-team-vote-menu.tile.player.lore"),
    INFO_TITLE("unequal-team-vote-menu.tile.info.title"),
    INFO_PLAYERS("unequal-team-vote-menu.tile.info.players", "online", "count"),
    INFO_TIME_LEFT("unequal-team-vote-menu.tile.info.time-left", "count"),
    INFO_HINT("unequal-team-vote-menu.tile.info.hint"),
    BEGIN("unequal-team-vote.begin", "count", "opponent"),
    RESULT("unequal-team-vote.result", "team", "players"),
    NOT_VOTING("unequal-team-vote.not-voting");

    private final String path;
    private final Set<String> placeholderNames;

    UnequalTeamVoteMessage(String path, String... placeholderNames) {
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
