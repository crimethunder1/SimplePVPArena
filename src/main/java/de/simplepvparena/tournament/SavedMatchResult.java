package de.simplepvparena.tournament;

import java.util.Objects;
import java.util.Optional;

public record SavedMatchResult(String matchId, String winnerTeamName, Optional<MatchDecisionKind> decidedBy) {

    public SavedMatchResult {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(winnerTeamName, "winnerTeamName");
        Objects.requireNonNull(decidedBy, "decidedBy");
    }
}
