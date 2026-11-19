package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ArenaBaselineReport {

    private final @Nullable ArenaBaselineSnapshot snapshot;
    private final @Nullable String failureReason;

    private ArenaBaselineReport(@Nullable ArenaBaselineSnapshot snapshot, @Nullable String failureReason) {
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
        ArenaBaselineSnapshot baseline = snapshot;
        if (baseline == null) {
            return Messages.text(ArenaCleaningMessage.BASELINE_NOT_TAKEN, Placeholder.of("reason", failureReason),
                    Placeholder.of("cleanAction", Arena.describeCleanAction()),
                    Placeholder.of("calculateAction", Arena.describeCalculateAction()));
        }
        return Messages.text(ArenaCleaningMessage.BASELINE_TAKEN, Placeholder.of("baseline", baseline.describe()),
                Placeholder.of("cleanAction", Arena.describeCleanAction()));
    }
}
