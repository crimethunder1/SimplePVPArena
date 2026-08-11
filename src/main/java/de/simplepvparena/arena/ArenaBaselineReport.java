package de.simplepvparena.arena;

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
                    + ". " + Arena.CLEAN_ACTION + " sets every block to air until "
                    + Arena.CALCULATE_ACTION + " takes one";
        }
        return "The baseline of the fight area was taken: " + snapshot.describe() + ". "
                + Arena.CLEAN_ACTION + " restores exactly this state instead of emptying the area";
    }
}
