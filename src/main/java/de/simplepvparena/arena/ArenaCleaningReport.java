package de.simplepvparena.arena;

public record ArenaCleaningReport(long checkedBlockCount, long clearedBlockCount, int clearedBorderMarkCount) {

    public String describe() {
        return clearedBlockCount + " of " + checkedBlockCount + " checked blocks were set to air, "
                + clearedBorderMarkCount + " border marks were removed";
    }
}
