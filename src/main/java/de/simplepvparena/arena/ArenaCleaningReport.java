package de.simplepvparena.arena;

public record ArenaCleaningReport(long checkedBlockCount, long changedBlockCount, int clearedBorderMarkCount,
                                  String changeDescription, String failureReason) {

    public ArenaCleaningReport(long checkedBlockCount, long changedBlockCount, int clearedBorderMarkCount,
                               String changeDescription) {
        this(checkedBlockCount, changedBlockCount, clearedBorderMarkCount, changeDescription, null);
    }

    public static ArenaCleaningReport failed(long checkedBlockCount, long changedBlockCount,
                                             int clearedBorderMarkCount, String changeDescription,
                                             String failureReason) {
        return new ArenaCleaningReport(checkedBlockCount, changedBlockCount, clearedBorderMarkCount,
                changeDescription, failureReason);
    }

    public boolean isSuccessful() {
        return failureReason == null;
    }

    public String describeOutcome() {
        if (isSuccessful()) {
            return "The arena is clean, " + describe();
        }
        return "The arena cleaning was stopped: " + failureReason + ". " + describe()
                + ". Run " + Arena.CLEAN_ACTION + " again once the reason is gone, the blocks that were "
                + "already written stay as they are";
    }

    public String describe() {
        if (changedBlockCount == 0L) {
            return "not a single block had to be changed, all " + checkedBlockCount
                    + " checked blocks already held what the cleaning writes. " + clearedBorderMarkCount
                    + " border marks were removed";
        }
        return changedBlockCount + " of " + checkedBlockCount + " checked blocks " + changeDescription + ", "
                + clearedBorderMarkCount + " border marks were removed";
    }
}
