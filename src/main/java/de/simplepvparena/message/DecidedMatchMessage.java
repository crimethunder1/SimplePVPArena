package de.simplepvparena.message;

import java.util.Set;

public enum DecidedMatchMessage implements MessageKey {

    TITLE("decided-matches-menu.title", "page"),
    CONFIRMATION("decided-matches-menu.confirmation", "team", "match"),
    INFO_TITLE("decided-matches-menu.tile.info.title"),
    NO_RESULT_REVOCABLE("decided-matches-menu.tile.info.no-result-revocable"),
    REVOCABLE_MATCH_COUNT("decided-matches-menu.tile.info.count", "count"),
    TEAMS("decided-matches-menu.tile.match.teams", "firstTeam", "secondTeam"),
    WINNER("decided-matches-menu.tile.match.winner", "team"),
    PLAYED("decided-matches-menu.tile.match.played"),
    WALKOVER("decided-matches-menu.tile.match.walkover"),
    FORCED("decided-matches-menu.tile.match.forced"),
    HINT("decided-matches-menu.tile.match.hint");

    private final String path;
    private final Set<String> placeholderNames;

    DecidedMatchMessage(String path, String... placeholderNames) {
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
