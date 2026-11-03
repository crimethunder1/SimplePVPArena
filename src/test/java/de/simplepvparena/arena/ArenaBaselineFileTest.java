package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ArenaBaselineFileTest {

    private static final long[] CELLS = {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)};

    @Test
    void theKeyBelongsToExactlyOneFightAreaAndDepth() {
        ArenaShape shape = new ArenaShape("arena", 64, 64, 70, CELLS);

        assertEquals(ArenaBaselineFile.describeKey(shape, 11),
                ArenaBaselineFile.describeKey(new ArenaShape("arena", 64, 64, 70, CELLS.clone()), 11));
        assertNotEquals(ArenaBaselineFile.describeKey(shape, 11), ArenaBaselineFile.describeKey(shape, 5));
        assertNotEquals(ArenaBaselineFile.describeKey(shape, 11),
                ArenaBaselineFile.describeKey(new ArenaShape("other", 64, 64, 70, CELLS), 11));
        assertNotEquals(ArenaBaselineFile.describeKey(shape, 11),
                ArenaBaselineFile.describeKey(new ArenaShape("arena", 64, 64, 72, CELLS), 11));
        assertNotEquals(ArenaBaselineFile.describeKey(shape, 11), ArenaBaselineFile.describeKey(
                new ArenaShape("arena", 64, 64, 70, new long[] {HorizontalCellCodec.encode(0, 0)}), 11));
    }
}
