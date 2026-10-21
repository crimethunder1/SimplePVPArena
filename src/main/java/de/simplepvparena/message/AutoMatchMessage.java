package de.simplepvparena.message;

import java.util.Set;

public enum AutoMatchMessage implements MessageKey {

    ANNOUNCEMENT("auto-match.announcement", "firstTeam", "secondTeam", "count"),
    CANCELLED_TEAM_OFFLINE("auto-match.cancelled.team-offline"),
    CANCELLED_MODE_SWITCHED_OFF("auto-match.cancelled.mode-switched-off"),
    CANCELLED_TOURNAMENT_STARTING("auto-match.cancelled.tournament-starting"),
    CANCELLED_ARENA_NOT_READY("auto-match.cancelled.arena-not-ready");

    private final String path;
    private final Set<String> placeholderNames;

    AutoMatchMessage(String path, String... placeholderNames) {
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
