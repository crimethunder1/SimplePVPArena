package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaDebugCellLimitTest {

    @Test
    void acceptsACellCountInsideTheAllowedRange() {
        ArenaDebugCellLimit limit = ArenaDebugCellLimit.parse("2000000");

        assertTrue(limit.isAccepted());
        assertEquals(2_000_000, limit.getCellCount());
        assertTrue(limit.getRejectionMessage().isEmpty());
    }

    @Test
    void acceptsTheEdgesOfTheAllowedRange() {
        assertEquals(ArenaDebugCellLimit.MINIMUM_CELL_COUNT,
                ArenaDebugCellLimit.parse(String.valueOf(ArenaDebugCellLimit.MINIMUM_CELL_COUNT)).getCellCount());
        assertEquals(ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT,
                ArenaDebugCellLimit.parse(String.valueOf(ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT))
                        .getCellCount());
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertEquals(500, ArenaDebugCellLimit.parse("  500 ").getCellCount());
    }

    @Test
    void rejectsSomethingThatIsNotANumber() {
        ArenaDebugCellLimit limit = ArenaDebugCellLimit.parse("many");

        assertFalse(limit.isAccepted());
        assertEquals("The cell limit many is not a number", limit.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsACellCountOutsideTheAllowedRange() {
        String expectedRejection = "The cell limit has to be between " + ArenaDebugCellLimit.MINIMUM_CELL_COUNT
                + " and " + ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT;

        assertEquals(expectedRejection, ArenaDebugCellLimit.parse("0").getRejectionMessage().orElseThrow());
        assertEquals(expectedRejection, ArenaDebugCellLimit.parse("-5").getRejectionMessage().orElseThrow());
        assertEquals(expectedRejection,
                ArenaDebugCellLimit.parse(String.valueOf(ArenaShapeFloodFill.MAXIMUM_DEBUG_CELL_COUNT + 1))
                        .getRejectionMessage().orElseThrow());
    }

    @Test
    void readsATypedLineAsTheSingleCellCount() {
        assertEquals(500, ArenaDebugCellLimit.parseLine("  500 ").getCellCount());
    }

    @Test
    void rejectsALineThatHoldsMoreThanTheCellCount() {
        assertEquals("Expected 1 values separated by spaces (<cells>), but got 2: 500 cells",
                ArenaDebugCellLimit.parseLine("500 cells").getRejectionMessage().orElseThrow());
        assertEquals("Nothing was typed, expected <cells>",
                ArenaDebugCellLimit.parseLine("  ").getRejectionMessage().orElseThrow());
    }
}
