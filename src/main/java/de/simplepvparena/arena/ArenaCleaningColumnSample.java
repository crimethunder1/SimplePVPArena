package de.simplepvparena.arena;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

public final class ArenaCleaningColumnSample {

    private static final long[] NO_COLUMNS = new long[0];

    private ArenaCleaningColumnSample() {
    }

    public static long[] pick(ArenaShape shape, int maximumColumnCount, IntUnaryOperator randomIndex) {
        if (shape == null || maximumColumnCount < 1) {
            return NO_COLUMNS;
        }
        int cellCount = shape.getCellCount();
        long[] columns = new long[Math.min(maximumColumnCount, cellCount)];
        int columnCount = 0;
        for (int draw = 0; draw < columns.length; draw++) {
            long column = shape.getCell(boundedIndex(randomIndex, cellCount));
            if (holds(columns, columnCount, column)) {
                continue;
            }
            columns[columnCount++] = column;
        }
        return columnCount == columns.length ? columns : Arrays.copyOf(columns, columnCount);
    }

    private static int boundedIndex(IntUnaryOperator randomIndex, int cellCount) {
        return Math.floorMod(randomIndex.applyAsInt(cellCount), cellCount);
    }

    private static boolean holds(long[] columns, int columnCount, long column) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            if (columns[columnIndex] == column) {
                return true;
            }
        }
        return false;
    }
}
