package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public record SavedFirstRoundMatch(String matchId, Optional<String> firstTeamName, Optional<String> secondTeamName) {

    public SavedFirstRoundMatch {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(firstTeamName, "firstTeamName");
        Objects.requireNonNull(secondTeamName, "secondTeamName");
    }
}
