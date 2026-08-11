package de.simplepvparena.arena;

import de.simplepvparena.message.TournamentMessages;

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
            return "The baseline of the fight area could not be taken: " + failureReason
                    + TournamentMessages.LINE_BREAK + Arena.CLEAN_ACTION + " sets every block to air until "
                    + Arena.CALCULATE_ACTION + " takes one";
        }
        return "The baseline of the fight area was taken"
                + TournamentMessages.LINE_BREAK + snapshot.describe()
                + TournamentMessages.LINE_BREAK + Arena.CLEAN_ACTION
                + " restores exactly this state instead of emptying the area";
    }
}
