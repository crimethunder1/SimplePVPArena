package de.simplepvparena.message;

import java.util.Set;

public enum TournamentStatusMessage implements MessageKey {

    STATE_IDLE("tournament-status.state.idle"),
    STATE_RUNNING("tournament-status.state.running"),
    STATE_PAUSED("tournament-status.state.paused"),
    STATE_CANCELLED("tournament-status.state.cancelled"),
    STATE_FINISHED("tournament-status.state.finished"),
    STAGE_PENDING("tournament-status.final-stage.pending"),
    STAGE_READY("tournament-status.final-stage.ready"),
    STAGE_RUNNING("tournament-status.final-stage.running"),
    STAGE_FINISHED("tournament-status.final-stage.finished"),
    STATUS("tournament-status.status.without-bracket", "state"),
    STATUS_WITH_RUNNING_MATCH("tournament-status.status.running-match", "state", "rounds", "match", "stage"),
    STATUS_WITHOUT_RUNNING_MATCH("tournament-status.status.no-running-match", "state", "rounds", "stage"),
    STATUS_WITH_RUNNING_MATCH_AND_CHAMPION("tournament-status.status.running-match-and-champion", "state", "rounds",
            "match", "stage", "champion"),
    STATUS_WITHOUT_RUNNING_MATCH_AND_CHAMPION("tournament-status.status.no-running-match-and-champion", "state",
            "rounds", "stage", "champion"),
    BRACKET_ROUND("tournament-status.bracket.round", "round"),
    BRACKET_MATCH("tournament-status.bracket.match", "match", "firstTeam", "secondTeam"),
    BRACKET_DECIDED_MATCH("tournament-status.bracket.decided-match", "match", "firstTeam", "secondTeam", "winner"),
    BRACKET_OPEN_SLOT("tournament-status.bracket.open-slot");

    private final String path;
    private final Set<String> placeholderNames;

    TournamentStatusMessage(String path, String... placeholderNames) {
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
