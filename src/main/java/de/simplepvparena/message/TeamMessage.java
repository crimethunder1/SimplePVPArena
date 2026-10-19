package de.simplepvparena.message;

import java.util.Set;

public enum TeamMessage implements MessageKey {

    NAME_RULE("team.name-rule", "maximumLength"),
    NAME_MISSING("team.name-missing", "rule"),
    NAME_NOT_ALLOWED("team.name-not-allowed", "team", "rule"),
    MEMBER_COUNT_OUT_OF_RANGE("team.member-count-out-of-range", "minimum", "maximum"),
    SAME_PLAYER_TWICE("team.same-player-twice"),
    ALREADY_EXISTS("team.already-exists", "team"),
    TEAM_FULL("team.team-full", "team", "count"),
    PLAYER_NOT_IN_TEAM("team.player-not-in-team", "uniqueId", "team"),
    LAST_MEMBER("team.last-member", "count"),
    NOT_REGISTERED("team.not-registered", "team"),
    PLAYER_ALREADY_IN_TEAM("team.player-already-in-team", "player", "team"),
    CREATION_USAGE("team.creation.usage"),
    CREATION_PLAYER_NAMED_TWICE("team.creation.player-named-twice", "minimum", "maximum", "player"),
    SIZE_OUT_OF_RANGE("team.size.out-of-range", "minimum", "maximum"),
    SIZE_TOURNAMENT_IN_PROGRESS("team.size.tournament-in-progress"),
    SIZE_MATCH_UP("team.size.match-up", "teamSize"),
    TEAMS_LARGER_THAN_ONE_PLAYER("team.size.larger-teams.than-one-player", "count", "teamSize", "teams"),
    TEAMS_LARGER_THAN_SEVERAL_PLAYERS("team.size.larger-teams.than-several-players", "count", "teamSize", "teams"),
    TEAMS_SMALLER_THAN_ONE_PLAYER("team.size.smaller-teams.than-one-player", "count", "teamSize", "teams"),
    TEAMS_SMALLER_THAN_SEVERAL_PLAYERS("team.size.smaller-teams.than-several-players", "count", "teamSize", "teams"),
    REMOVE_LARGER_TEAMS_FIRST("team.size.remove-larger-teams-first", "largerTeams"),
    TEAM_WITH_MEMBERS("team.size.team-with-members", "team", "members"),
    TEAM_WITH_MEMBER_COUNT("team.size.team-with-member-count", "team", "memberCount");

    private final String path;
    private final Set<String> placeholderNames;

    TeamMessage(String path, String... placeholderNames) {
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
