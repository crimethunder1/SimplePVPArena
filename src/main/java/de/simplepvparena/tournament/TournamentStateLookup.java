package de.simplepvparena.tournament;

import java.util.Objects;
import java.util.Optional;

public record TournamentStateLookup(Outcome outcome, Optional<SavedTournament> savedTournament, String fileName,
                                    Optional<String> failureReason) {

    public enum Outcome {
        FOUND,
        MISSING,
        BROKEN
    }

    public TournamentStateLookup {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(savedTournament, "savedTournament");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(failureReason, "failureReason");
    }

    public static TournamentStateLookup found(SavedTournament savedTournament, String fileName) {
        return new TournamentStateLookup(Outcome.FOUND, Optional.of(savedTournament), fileName, Optional.empty());
    }

    public static TournamentStateLookup missing(String fileName) {
        return new TournamentStateLookup(Outcome.MISSING, Optional.empty(), fileName, Optional.empty());
    }

    public static TournamentStateLookup broken(String fileName, String failureReason) {
        return new TournamentStateLookup(Outcome.BROKEN, Optional.empty(), fileName, Optional.of(failureReason));
    }
}
