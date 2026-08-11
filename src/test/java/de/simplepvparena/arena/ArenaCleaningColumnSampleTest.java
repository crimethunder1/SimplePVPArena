package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningColumnSampleTest {

    private static final int FLOOR_Y = 64;

    @Test
    void neverDrawsMoreColumnsThanTheRateAllows() {
        long[] batch = layerOfBlocks(4096);

        long[] columns = ArenaCleaningColumnSample.pick(batch, 6, countingIndex());

        assertEquals(6, columns.length);
    }

    @Test
    void picksTheColumnsOfTheBlocksTheRandomSourceNames() {
        long[] batch = {
                BlockPositionCodec.encode(10, FLOOR_Y, 20),
                BlockPositionCodec.encode(11, FLOOR_Y, 21),
                BlockPositionCodec.encode(12, FLOOR_Y, 22)};

        long[] columns = ArenaCleaningColumnSample.pick(batch, 2, indexesOf(2, 0));

        assertEquals(List.of(HorizontalCellCodec.encode(12, 22), HorizontalCellCodec.encode(10, 20)),
                asList(columns));
    }

    @Test
    void dropsTheSecondPickOfTheSameColumn() {
        long[] batch = {
                BlockPositionCodec.encode(4, FLOOR_Y, 8),
                BlockPositionCodec.encode(4, FLOOR_Y + 5, 8),
                BlockPositionCodec.encode(9, FLOOR_Y, 3)};

        long[] columns = ArenaCleaningColumnSample.pick(batch, 3, indexesOf(0, 1, 2));

        assertEquals(List.of(HorizontalCellCodec.encode(4, 8), HorizontalCellCodec.encode(9, 3)),
                asList(columns), "the same cell at two heights is one column");
    }

    @Test
    void aBatchSmallerThanTheRateIsNeverStretched() {
        long[] batch = {BlockPositionCodec.encode(1, FLOOR_Y, 1), BlockPositionCodec.encode(2, FLOOR_Y, 2)};

        long[] columns = ArenaCleaningColumnSample.pick(batch, 10, countingIndex());

        assertEquals(2, columns.length);
    }

    @Test
    void drawsNothingForAnEmptyBatchOrARateOfZero() {
        assertEquals(0, ArenaCleaningColumnSample.pick(new long[0], 6, countingIndex()).length);
        assertEquals(0, ArenaCleaningColumnSample.pick(null, 6, countingIndex()).length);
        assertEquals(0, ArenaCleaningColumnSample.pick(layerOfBlocks(64), 0, countingIndex()).length);
        assertEquals(0, ArenaCleaningColumnSample.pick(layerOfBlocks(64), -4, countingIndex()).length);
    }

    @Test
    void keepsTheCellsOfANegativeCorner() {
        long[] batch = {BlockPositionCodec.encode(-30, FLOOR_Y, -7)};

        long[] columns = ArenaCleaningColumnSample.pick(batch, 1, indexesOf(0));

        assertEquals(-30, HorizontalCellCodec.decodeX(columns[0]));
        assertEquals(-7, HorizontalCellCodec.decodeZ(columns[0]));
    }

    @Test
    void survivesARandomSourceThatAnswersOutsideOfTheBatch() {
        long[] batch = layerOfBlocks(8);

        long[] columns = ArenaCleaningColumnSample.pick(batch, 3, indexesOf(99, -1, Integer.MIN_VALUE));

        assertEquals(3, columns.length);
        for (long column : columns) {
            assertTrue(asList(batch).stream().anyMatch(blockPosition ->
                            HorizontalCellCodec.encode(BlockPositionCodec.decodeX(blockPosition),
                                    BlockPositionCodec.decodeZ(blockPosition)) == column),
                    "every drawn column has to belong to a block of the batch");
        }
    }

    @Test
    void asksTheRandomSourceWithTheLengthOfTheBatch() {
        List<Integer> bounds = new ArrayList<>();
        long[] batch = layerOfBlocks(37);

        ArenaCleaningColumnSample.pick(batch, 4, bound -> {
            bounds.add(bound);
            return 0;
        });

        assertEquals(List.of(37, 37, 37, 37), bounds);
    }

    private static long[] layerOfBlocks(int blockCount) {
        long[] batch = new long[blockCount];
        for (int blockIndex = 0; blockIndex < blockCount; blockIndex++) {
            batch[blockIndex] = BlockPositionCodec.encode(blockIndex, FLOOR_Y, blockIndex * 2);
        }
        return batch;
    }

    private static IntUnaryOperator indexesOf(int... indexes) {
        int[] nextIndex = {0};
        return bound -> indexes[nextIndex[0]++ % indexes.length];
    }

    private static IntUnaryOperator countingIndex() {
        int[] next = {0};
        return bound -> next[0]++;
    }

    private static List<Long> asList(long[] values) {
        List<Long> list = new ArrayList<>(values.length);
        for (long value : values) {
            list.add(value);
        }
        return list;
    }
}
