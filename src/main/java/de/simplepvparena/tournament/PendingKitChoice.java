package de.simplepvparena.tournament;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record PendingKitChoice(String matchId, String teamsDescription, String defaultKitName, Instant deadline) {

    private static final long MILLISECONDS_PER_SECOND = 1000L;

    public PendingKitChoice {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(teamsDescription, "teamsDescription");
        Objects.requireNonNull(defaultKitName, "defaultKitName");
        Objects.requireNonNull(deadline, "deadline");
    }

    public long secondsLeftAt(Instant now) {
        long millisecondsLeft = Duration.between(now, deadline).toMillis();
        if (millisecondsLeft <= 0) {
            return 0;
        }
        return (millisecondsLeft + MILLISECONDS_PER_SECOND - 1) / MILLISECONDS_PER_SECOND;
    }
}
