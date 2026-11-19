package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public enum PlacementMessage implements MessageKey {

    TOURNAMENT_TITLE("placement.title.tournament"),
    AUTO_MATCH_TITLE("placement.title.auto-match"),
    NO_GAMES_YET("placement.no-games-yet"),
    NO_GAMES_HINT("placement.no-games-hint"),
    WINS_ENTRY("placement.wins-entry", "place", "player", "count"),
    ROUND("placement.round", "round", "rounds"),
    PAUSED("placement.paused"),
    OVER("placement.over"),
    MATCH("placement.match", "firstTeam", "firstPoints", "secondPoints", "secondTeam"),
    NO_MATCH("placement.no-match"),
    STILL_IN("placement.still-in", "count"),
    MORE("placement.more", "count"),
    PLACE("placement.place", "place", "teams"),
    OUT_IN_ROUND("placement.out-in-round", "round", "teams");

    private final String path;
    private final Set<String> placeholderNames;

    PlacementMessage(String path, String... placeholderNames) {
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
