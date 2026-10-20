package de.simplepvparena.message;

import java.util.Set;

public enum BracketViewMessage implements MessageKey {

    TITLE("bracket-menu.title", "round"),
    ROUND_OF_ROUNDS("bracket-menu.round-of-rounds", "round", "roundCount"),
    INFO_TITLE("bracket-menu.tile.info.title"),
    INFO_STATE("bracket-menu.tile.info.state", "state"),
    INFO_CHAMPION("bracket-menu.tile.info.champion", "team"),
    INFO_MATCHES_CUT_OFF("bracket-menu.tile.info.matches-cut-off", "shown", "total"),
    MATCH_TEAMS("bracket-menu.tile.match.teams", "firstTeam", "secondTeam"),
    MATCH_OPEN_TEAM("bracket-menu.tile.match.open-team"),
    MATCH_WAITING_FOR_TEAMS("bracket-menu.tile.match.waiting-for-teams"),
    MATCH_READY("bracket-menu.tile.match.ready"),
    MATCH_RUNNING("bracket-menu.tile.match.running"),
    MATCH_WON_BY("bracket-menu.tile.match.won-by", "team"),
    MATCH_BYE("bracket-menu.tile.match.bye", "team"),
    PREVIOUS_ROUND_TITLE("bracket-menu.tile.previous-round.title"),
    NEXT_ROUND_TITLE("bracket-menu.tile.next-round.title");

    private final String path;
    private final Set<String> placeholderNames;

    BracketViewMessage(String path, String... placeholderNames) {
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
