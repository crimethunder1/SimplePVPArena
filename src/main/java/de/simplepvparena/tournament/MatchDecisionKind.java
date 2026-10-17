package de.simplepvparena.tournament;

import java.util.Arrays;
import java.util.Optional;

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
