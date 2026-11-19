package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Optional;

@NullMarked
public enum MatchDecisionKind {
    PLAYED("played"),
    WALKOVER("walkover"),
    FORCED("forced");

    private final String storedValue;

    MatchDecisionKind(String storedValue) {
        this.storedValue = storedValue;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public static Optional<MatchDecisionKind> fromStoredValue(String storedValue) {
        return Arrays.stream(values())
                .filter(decisionKind -> decisionKind.storedValue.equals(storedValue))
                .findFirst();
    }
}
