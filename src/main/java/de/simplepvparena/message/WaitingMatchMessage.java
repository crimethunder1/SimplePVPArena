package de.simplepvparena.message;

import java.util.Set;

public enum WaitingMatchMessage implements MessageKey {

    TITLE("waiting-matches-menu.title", "page"),
    INFO_TITLE("waiting-matches-menu.tile.info.title"),
    NO_MATCH_WAITING("waiting-matches-menu.tile.info.no-match-waiting"),
    WAITING_MATCH_COUNT("waiting-matches-menu.tile.info.count", "count"),
    INFO_HINT("waiting-matches-menu.tile.info.hint"),
    TEAMS("waiting-matches-menu.tile.match.teams", "firstTeam", "secondTeam"),
    ROUND("waiting-matches-menu.tile.match.round", "round"),
    TEAM_NOT_REGISTERED("waiting-matches-menu.tile.match.team-not-registered", "team"),
    RESULT_MENU_TITLE("waiting-match-result-menu.title", "match"),
    RESULT_MATCH_NOT_WAITING("waiting-match-result-menu.match-not-waiting", "match"),
    RESULT_INFO_TITLE("waiting-match-result-menu.tile.info.title"),
    RESULT_INFO_MATCH("waiting-match-result-menu.tile.info.match", "match", "firstTeam", "secondTeam"),
    RESULT_INFO_HINT("waiting-match-result-menu.tile.info.hint"),
    RESULT_TEAM_NOT_REGISTERED("waiting-match-result-menu.tile.team.not-registered"),
    RESULT_TEAM_LORE("waiting-match-result-menu.tile.team.lore");

    private final String path;
    private final Set<String> placeholderNames;

    WaitingMatchMessage(String path, String... placeholderNames) {
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
