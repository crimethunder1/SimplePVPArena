package de.simplepvparena.border;

import java.util.Objects;

public record OperatorBorderPass(String playerName, int passCount, long firstPassMillis, long lastPassMillis) {

    public OperatorBorderPass {
        Objects.requireNonNull(playerName, "playerName");
        if (passCount < 1) {
            throw new IllegalArgumentException("passCount must be at least one");
        }
    }

    public String describe() {
        return playerName + " (" + passCount + (passCount == 1 ? " time)" : " times)");
    }
}
