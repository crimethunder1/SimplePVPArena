package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public enum PlacementMessage implements MessageKey {

    TOURNAMENT_TITLE("placement.title.tournament"),
    AUTO_MATCH_TITLE("placement.title.auto-match"),
    NO_GAMES_YET("placement.no-games-yet"),
    NO_GAMES_HINT("placement.no-games-hint"),
    NO_TOURNAMENT_YET("placement.no-tournament-yet"),
    NO_TOURNAMENT_HINT("placement.no-tournament-hint"),
    WINS_ENTRY("placement.wins-entry", "place", "player", "count"),
    ROUND("placement.round", "round", "rounds"),
    PAUSED("placement.paused"),
    OVER("placement.over"),
    MATCH("placement.match", "firstTeam", "firstPoints", "secondPoints", "secondTeam"),
    NO_MATCH("placement.no-match"),
    STILL_IN("placement.still-in", "count"),
    MORE("placement.more", "count"),
    PLACE("placement.place", "place", "teams"),
    OUT_IN_ROUND("placement.out-in-round", "round", "teams"),
    BRACKET_TITLE("placement.bracket.title"),
    BRACKET_WAITING_HINT("placement.bracket.waiting-hint"),
    BRACKET_NOW("placement.bracket.now", "firstTeam", "secondTeam"),
    BRACKET_NOW_NONE("placement.bracket.now-none"),
    BRACKET_NEXT("placement.bracket.next", "firstTeam", "secondTeam"),
    BRACKET_NEXT_OPEN("placement.bracket.next-open"),
    BRACKET_CHAMPION("placement.bracket.champion", "team"),
    BRACKET_ROUND("placement.bracket.round", "round"),
    BRACKET_QUARTER_FINAL("placement.bracket.quarter-final"),
    BRACKET_SEMI_FINAL("placement.bracket.semi-final"),
    BRACKET_FINAL("placement.bracket.final"),
    BRACKET_WINNER_COLUMN("placement.bracket.winner-column"),
    BRACKET_OPEN_TEAM("placement.bracket.open-team"),
    BRACKET_BYE("placement.bracket.bye"),
    BRACKET_ITEM_NAME("placement.bracket.item-name"),
    BRACKET_ITEM_LORE("placement.bracket.item-lore"),
    BRACKET_PLACED("placement.bracket.placed"),
    BRACKET_REMOVED("placement.bracket.removed"),
    BRACKET_NO_ROOM("placement.bracket.no-room", "width", "height"),
    BRACKET_NOT_LOADED("placement.bracket.not-loaded", "file"),
    BRACKET_NOT_SAVED("placement.bracket.not-saved", "file");

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
