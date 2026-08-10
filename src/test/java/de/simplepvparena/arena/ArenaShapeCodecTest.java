package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaShapeCodecTest {

    @Test
    void writesOneRunLengthEncodedRowPerXCoordinate() {
        long[] cells = cells(5, 1, 5, 2, 5, 3, 6, 1);

        List<String> rows = ArenaShapeCodec.encodeRows(cells);

        assertEquals(List.of("5:1..3", "6:1..1"), rows);
    }

    @Test
    void splitsARowThatHoldsAGap() {
        long[] cells = cells(0, 1, 0, 2, 0, 5);

        assertEquals(List.of("0:1..2,5..5"), ArenaShapeCodec.encodeRows(cells));
    }

    @Test
    void readsTheWrittenCellsBackUnchanged() {
        long[] cells = cells(-3, -8, -3, -7, -3, -6, -2, -8, 12, 40, 12, 41, 13, 40);

        long[] decodedCells = ArenaShapeCodec.decodeRows(ArenaShapeCodec.encodeRows(cells));

        assertArrayEquals(sorted(cells), sorted(decodedCells));
    }

    @Test
    void keepsAShapeOfSeveralThousandCellsShort() {
        long[] cells = new long[100 * 100];
        int cellIndex = 0;
        for (int cellX = 0; cellX < 100; cellX++) {
            for (int cellZ = 0; cellZ < 100; cellZ++) {
                cells[cellIndex++] = HorizontalCellCodec.encode(cellX, cellZ);
            }
        }

        List<String> rows = ArenaShapeCodec.encodeRows(cells);

        assertEquals(100, rows.size(), "one row per x coordinate instead of one entry per cell");
        assertEquals("0:0..99", rows.get(0));
        assertArrayEquals(sorted(cells), sorted(ArenaShapeCodec.decodeRows(rows)));
    }

    @Test
    void readsNegativeCoordinatesOfAHandWrittenRow() {
        long[] cells = ArenaShapeCodec.decodeRows(List.of("-10:-5..-3"));

        assertArrayEquals(cells(-10, -5, -10, -4, -10, -3), sorted(cells));
    }

    @Test
    void rejectsABrokenRow() {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> ArenaShapeCodec.decodeRows(List.of("5"))).getMessage().contains("5"));
        assertThrows(IllegalArgumentException.class, () -> ArenaShapeCodec.decodeRows(List.of("5:1")));
        assertThrows(IllegalArgumentException.class, () -> ArenaShapeCodec.decodeRows(List.of("x:1..2")));
        assertThrows(IllegalArgumentException.class, () -> ArenaShapeCodec.decodeRows(List.of("5:3..1")));
    }

    private long[] cells(int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return cells;
    }

    private long[] sorted(long[] cells) {
        long[] sortedCells = cells.clone();
        Arrays.sort(sortedCells);
        return sortedCells;
    }
}
