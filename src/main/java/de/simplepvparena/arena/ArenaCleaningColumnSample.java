package de.simplepvparena.arena;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

public final class ArenaCleaningColumnSample {

    private static final long[] NO_COLUMNS = new long[0];

    private ArenaCleaningColumnSample() {
    }

    public static long[] pick(long[] blockBatch, int maximumColumnCount, IntUnaryOperator randomIndex) {
        if (blockBatch == null || blockBatch.length == 0 || maximumColumnCount < 1) {
            return NO_COLUMNS;
        }
        long[] columns = new long[Math.min(maximumColumnCount, blockBatch.length)];
        int columnCount = 0;
        for (int draw = 0; draw < columns.length; draw++) {
            long blockPosition = blockBatch[boundedIndex(randomIndex, blockBatch.length)];
            long column = HorizontalCellCodec.encode(BlockPositionCodec.decodeX(blockPosition),
                    BlockPositionCodec.decodeZ(blockPosition));
            if (holds(columns, columnCount, column)) {
                continue;
            }
            columns[columnCount++] = column;
        }
        return columnCount == columns.length ? columns : Arrays.copyOf(columns, columnCount);
    }

    private static int boundedIndex(IntUnaryOperator randomIndex, int batchLength) {
        return Math.floorMod(randomIndex.applyAsInt(batchLength), batchLength);
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
