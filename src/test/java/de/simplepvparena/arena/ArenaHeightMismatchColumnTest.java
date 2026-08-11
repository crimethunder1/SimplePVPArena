package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArenaHeightMismatchColumnTest {

    @Test
    void namesHowFarTheWallStandsAboveTheCalculation() {
        ArenaHeightMismatchColumn column = new ArenaHeightMismatchColumn(12, -30, 71, 64);

        assertEquals(7, column.heightAboveSeed());
        assertEquals("x/z 12/-30 (lowest border block at y 71, 7 above you)", column.describe());
    }

    @Test
    void countsTheDifferenceAroundNegativeHeights() {
        assertEquals(3, new ArenaHeightMismatchColumn(0, 0, -58, -61).heightAboveSeed());
    }

    @Test
    void rejectsAColumnThatBarsTheFillAnyway() {
        assertThrows(IllegalArgumentException.class,
                () -> new ArenaHeightMismatchColumn(0, 0, 64, 64));
        assertThrows(IllegalArgumentException.class,
                () -> new ArenaHeightMismatchColumn(0, 0, 60, 64));
    }
}
