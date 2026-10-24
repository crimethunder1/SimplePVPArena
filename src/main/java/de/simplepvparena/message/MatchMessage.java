package de.simplepvparena.message;

import java.util.Set;

public enum MatchMessage implements MessageKey {

    MENU_REFERENCE("match.menu-reference", "tile"),
    ROUND_STARTED_WITH_TIME_LIMIT("match.round.started-with-time-limit", "round", "match", "score", "count"),
    ROUND_STARTED_WITHOUT_TIME_LIMIT("match.round.started-without-time-limit", "round", "match", "score"),
    ROUND_DRAW("match.round.draw", "round", "match"),
    SUDDEN_DEATH_DRAW("match.round.sudden-death-draw", "round", "match"),
    POINT_SCORED("match.point-scored", "team", "score"),
    HALTED("match.halted", "match", "action"),
    OVER("match.over", "match", "team", "score"),
    ROUND_CLEANING_WITHOUT_BASELINE("match.round-cleaning.without-baseline"),
    ROUND_CLEANING_NOT_STARTED("match.round-cleaning.not-started", "reason"),
    RECONNECT_CONNECTION_LOST("match.reconnect.connection-lost", "player", "count"),
    RECONNECT_RETURNED("match.reconnect.returned", "player"),
    RECONNECT_NOT_RETURNED("match.reconnect.not-returned", "player"),
    SUDDEN_DEATH_WARNING("match.sudden-death.warning", "count"),
    SUDDEN_DEATH_TITLE("match.sudden-death.title"),
    SUDDEN_DEATH_SUBTITLE("match.sudden-death.subtitle"),
    SUDDEN_DEATH_STARTED("match.sudden-death.started", "round", "match"),
    ROUND_DRAW_TITLE("match.title.round-draw", "round"),
    ROUND_WON_TITLE("match.title.round-won", "team", "round"),
    MATCH_WON_TITLE("match.title.match-won", "team"),
    FINAL_WON_TITLE("match.title.final-won", "team"),
    RESULT_SUBTITLE("match.title.result", "match", "score"),
    DECIDED_BY_AN_ADMIN_SUBTITLE("match.title.decided-by-an-admin", "match");

    private final String path;
    private final Set<String> placeholderNames;

    MatchMessage(String path, String... placeholderNames) {
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
