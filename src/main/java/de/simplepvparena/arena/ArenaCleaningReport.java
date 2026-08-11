package de.simplepvparena.arena;

public record ArenaCleaningReport(long checkedBlockCount, long changedBlockCount, int clearedBorderMarkCount,
                                  String changeDescription) {

    public String describe() {
        return changedBlockCount + " of " + checkedBlockCount + " checked blocks " + changeDescription + ", "
                + clearedBorderMarkCount + " border marks were removed";
    }
}
