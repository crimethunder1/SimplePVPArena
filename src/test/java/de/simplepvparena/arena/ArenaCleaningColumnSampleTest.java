package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningColumnSampleTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 64;

    @Test
    void neverDrawsMoreCellsThanTheRateAllows() {
        ArenaShape shape = shapeWithCells(4096);

        long[] columns = ArenaCleaningColumnSample.pick(shape, 6, countingIndex());

        assertEquals(6, columns.length);
    }

    @Test
    void picksTheCellsTheRandomSourceNames() {
        ArenaShape shape = shapeOf(cell(10, 20), cell(11, 21), cell(12, 22));

        long[] columns = ArenaCleaningColumnSample.pick(shape, 2, indexesOf(2, 0));

        assertEquals(List.of(HorizontalCellCodec.encode(12, 22), HorizontalCellCodec.encode(10, 20)),
                asList(columns), "the cells of a shape are sorted, the picks follow that order");
    }

    @Test
    void dropsTheSecondPickOfTheSameCell() {
        ArenaShape shape = shapeOf(cell(4, 8), cell(9, 3));

        long[] columns = ArenaCleaningColumnSample.pick(shape, 2, indexesOf(0, 0));

        assertEquals(1, columns.length, "the same cell twice is one dot");
    }

    @Test
    void anAreaSmallerThanTheRateIsNeverStretched() {
        ArenaShape shape = shapeOf(cell(1, 1), cell(2, 2));

        long[] columns = ArenaCleaningColumnSample.pick(shape, 10, countingIndex());

        assertEquals(2, columns.length);
    }

    @Test
    void drawsNothingWithoutAShapeOrWithARateOfZero() {
        assertEquals(0, ArenaCleaningColumnSample.pick(null, 6, countingIndex()).length);
        assertEquals(0, ArenaCleaningColumnSample.pick(shapeWithCells(64), 0, countingIndex()).length);
        assertEquals(0, ArenaCleaningColumnSample.pick(shapeWithCells(64), -4, countingIndex()).length);
    }

    @Test
    void keepsTheCellsOfANegativeCorner() {
        ArenaShape shape = shapeOf(cell(-30, -7));

        long[] columns = ArenaCleaningColumnSample.pick(shape, 1, indexesOf(0));

        assertEquals(-30, HorizontalCellCodec.decodeX(columns[0]));
        assertEquals(-7, HorizontalCellCodec.decodeZ(columns[0]));
    }

    @Test
    void survivesARandomSourceThatAnswersOutsideOfTheArea() {
        ArenaShape shape = shapeWithCells(8);

        long[] columns = ArenaCleaningColumnSample.pick(shape, 3, indexesOf(99, -1, Integer.MIN_VALUE));

        assertEquals(3, columns.length);
        for (long column : columns) {
            assertTrue(shape.containsCell(HorizontalCellCodec.decodeX(column),
                            HorizontalCellCodec.decodeZ(column)),
                    "every drawn dot has to stand in a cell of the fight area");
        }
    }

    @Test
    void asksTheRandomSourceWithTheNumberOfCells() {
        List<Integer> bounds = new ArrayList<>();
        ArenaShape shape = shapeWithCells(37);

        ArenaCleaningColumnSample.pick(shape, 4, bound -> {
            bounds.add(bound);
            return 0;
        });

        assertEquals(List.of(37, 37, 37, 37), bounds);
    }

    @Test
    void picksTheSameCellsNoMatterHowTallTheFightAreaIs() {
        ArenaShape flat = new ArenaShape(WORLD_NAME, FLOOR_Y, FLOOR_Y, FLOOR_Y,
                new long[] {cell(0, 0), cell(1, 0)});
        ArenaShape tall = flat.withVerticalExtent(FLOOR_Y, FLOOR_Y + 50);

        assertEquals(asList(ArenaCleaningColumnSample.pick(flat, 2, indexesOf(0, 1))),
                asList(ArenaCleaningColumnSample.pick(tall, 2, indexesOf(0, 1))));
    }

    private static ArenaShape shapeWithCells(int cellCount) {
        long[] cells = new long[cellCount];
        for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            cells[cellIndex] = cell(cellIndex, cellIndex * 2);
        }
        return shapeOf(cells);
    }

    private static ArenaShape shapeOf(long... cells) {
        return new ArenaShape(WORLD_NAME, FLOOR_Y, FLOOR_Y, FLOOR_Y + 2, cells);
    }

    private static long cell(int blockX, int blockZ) {
        return HorizontalCellCodec.encode(blockX, blockZ);
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
