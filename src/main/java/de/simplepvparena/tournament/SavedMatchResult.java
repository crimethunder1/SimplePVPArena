package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public record SavedMatchResult(String matchId, String winnerTeamName, Optional<MatchDecisionKind> decidedBy) {

    public SavedMatchResult {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(winnerTeamName, "winnerTeamName");
        Objects.requireNonNull(decidedBy, "decidedBy");
    }
}
