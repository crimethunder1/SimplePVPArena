package de.simplepvparena.message;

import java.util.Set;

public enum PlacementMessage implements MessageKey {

    TOURNAMENT_TITLE("placement.title.tournament"),
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
