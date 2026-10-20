package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

public final class ArenaBaselineReport {

    private final ArenaBaselineSnapshot snapshot;
    private final String failureReason;

    private ArenaBaselineReport(ArenaBaselineSnapshot snapshot, String failureReason) {
        this.snapshot = snapshot;
        this.failureReason = failureReason;
    }

    public static ArenaBaselineReport captured(ArenaBaselineSnapshot snapshot) {
        return new ArenaBaselineReport(snapshot, null);
    }

    public static ArenaBaselineReport failed(String failureReason) {
        return new ArenaBaselineReport(null, failureReason);
    }

    public boolean isCaptured() {
        return snapshot != null;
    }

    public String describe() {
        if (!isCaptured()) {
            return Messages.text(ArenaCleaningMessage.BASELINE_NOT_TAKEN, Placeholder.of("reason", failureReason),
                    Placeholder.of("cleanAction", Arena.describeCleanAction()),
                    Placeholder.of("calculateAction", Arena.describeCalculateAction()));
        }
        return Messages.text(ArenaCleaningMessage.BASELINE_TAKEN, Placeholder.of("baseline", snapshot.describe()),
                Placeholder.of("cleanAction", Arena.describeCleanAction()));
    }
}
