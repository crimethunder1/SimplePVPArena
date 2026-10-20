package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaFillDiagnosticsTest {

    @Test
    void boundingBoxCoversEveryFilledCell() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 1, 0, 2, 0, 2, 1, 2, 2);

        assertEquals(0, diagnostics.getMinimumX());
        assertEquals(2, diagnostics.getMaximumX());
        assertEquals(0, diagnostics.getMinimumZ());
        assertEquals(2, diagnostics.getMaximumZ());
        assertEquals(5, diagnostics.getCellCount());
    }

    @Test
    void boundingBoxCoversNegativeCoordinates() {
        ArenaFillDiagnostics diagnostics = fromCells(-40, 15, -40, 15, -41, 15, -40, 14, -100, -100);

        assertEquals(-100, diagnostics.getMinimumX());
        assertEquals(-40, diagnostics.getMaximumX());
        assertEquals(-100, diagnostics.getMinimumZ());
        assertEquals(15, diagnostics.getMaximumZ());
    }

    @Test
    void boundingBoxAlwaysHoldsTheSeed() {
        ArenaFillDiagnostics diagnostics = fromCells(7, -3, 7, -3);

        assertEquals(7, diagnostics.getMinimumX());
        assertEquals(7, diagnostics.getMaximumX());
        assertEquals(-3, diagnostics.getMinimumZ());
        assertEquals(-3, diagnostics.getMaximumZ());
        assertFalse(diagnostics.hasSpread(), "a fill of a single cell never left its seed column");
    }

    @Test
    void frontHoldsTheLastFilledCellsInFillOrder() {
        Set<Long> filledCells = new LinkedHashSet<>();
        for (int cellX = 0; cellX < 20; cellX++) {
            filledCells.add(HorizontalCellCodec.encode(cellX, 5));
        }

        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(0, 5, filledCells);

        long[] frontCells = diagnostics.getFrontCells();
        assertEquals(ArenaFillDiagnostics.FRONT_SAMPLE_SIZE, frontCells.length);
        assertEquals(HorizontalCellCodec.encode(12, 5), frontCells[0]);
        assertEquals(HorizontalCellCodec.encode(19, 5), frontCells[frontCells.length - 1]);
    }

    @Test
    void frontHoldsEveryCellOfAFillShorterThanTheSample() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 1, 0, 1, 1);

        assertEquals(3, diagnostics.getFrontCells().length);
        assertEquals("0/0, 1/0 and 1/1", diagnostics.describeFront());
    }

    @Test
    void namesTheDirectionTheFillSpreadFurthestIn() {
        assertEquals("east", directionOf(0, 0, 10, 1));
        assertEquals("west", directionOf(0, 0, -10, 1));
        assertEquals("south", directionOf(0, 0, 1, 10));
        assertEquals("north", directionOf(0, 0, 1, -10));
    }

    @Test
    void namesBothAxesWhenTheFillRanOutOfACorner() {
        assertEquals("north-east", directionOf(0, 0, 10, -10));
        assertEquals("south-west", directionOf(0, 0, -6, 8));
        assertEquals("south-east", directionOf(100, 100, 130, 160));
        assertEquals("north-west", directionOf(-20, -20, -50, -55));
    }

    @Test
    void namesOnlyTheAxisTheFillReallyRanAlong() {
        assertEquals("east", directionOf(0, 0, 40, 3));
        assertEquals("south", directionOf(0, 0, 3, 40));
    }

    @Test
    void namesNoDirectionForAFillThatNeverLeftItsSeed() {
        ArenaFillDiagnostics diagnostics = fromCells(5, 5, 5, 5);

        assertEquals("nowhere", diagnostics.getEscapeDirectionName());
        assertTrue(diagnostics.describeEscapeDirection().contains("never left the column you are standing in"));
    }

    @Test
    void escapeDescriptionNamesTheCompassTheDistancesAndTheCorner() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 3, 0, 3, -4);

        assertEquals("north-east, 3 columns east and 4 columns north of you, up to x/z 3/-4",
                diagnostics.describeEscapeDirection());
        assertEquals(3, diagnostics.getFarthestX());
        assertEquals(-4, diagnostics.getFarthestZ());
    }

    @Test
    void boundingBoxDescriptionNamesTheEdgesAndTheSize() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 3, 0, 3, -4);

        assertEquals("x 0..3, z -4..0, 4 by 5 columns", diagnostics.describeBoundingBox());
    }

    @Test
    void nearestFrontCellIsTheFrontColumnClosestToTheSeed() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 40, 0, 10, 0, 60, 0);

        assertEquals(HorizontalCellCodec.encode(10, 0), diagnostics.getNearestFrontCell());
        assertEquals(10, diagnostics.getNearestFrontX());
        assertEquals(0, diagnostics.getNearestFrontZ());
    }

    @Test
    void nearestFrontCellMeasuresTheRealDistanceOverBothAxes() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 12, 0, 7, 7);

        assertEquals(HorizontalCellCodec.encode(7, 7), diagnostics.getNearestFrontCell());
    }

    @Test
    void nearestFrontCellIsFoundAroundNegativeCoordinates() {
        ArenaFillDiagnostics diagnostics = fromCells(-100, -100, -100, -100, -140, -100, -103, -104);

        assertEquals(HorizontalCellCodec.encode(-103, -104), diagnostics.getNearestFrontCell());
    }

    @Test
    void nearestFrontCellNeverLooksBeyondTheFrontSample() {
        Set<Long> filledCells = new LinkedHashSet<>();
        for (int cellX = 0; cellX < 20; cellX++) {
            filledCells.add(HorizontalCellCodec.encode(cellX, 5));
        }

        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(0, 5, filledCells);

        assertEquals(HorizontalCellCodec.encode(12, 5), diagnostics.getNearestFrontCell());
    }

    @Test
    void nearestFrontCellPrefersTheEarlierOfTwoEquallyFarColumns() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 5, 0, 0, 5);

        assertEquals(HorizontalCellCodec.encode(5, 0), diagnostics.getNearestFrontCell());
    }

    @Test
    void nearestFrontCellSkipsTheSeedItself() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 1, 0, 1, 1);

        assertEquals(HorizontalCellCodec.encode(1, 0), diagnostics.getNearestFrontCell());
    }

    @Test
    void nearestFrontCellOfAFillThatNeverSpreadIsTheSeed() {
        ArenaFillDiagnostics diagnostics = fromCells(7, -3, 7, -3);

        assertFalse(diagnostics.hasSpread());
        assertEquals(HorizontalCellCodec.encode(7, -3), diagnostics.getNearestFrontCell());
    }

    @Test
    void rejectsAFillWithoutASingleCell() {
        assertThrows(IllegalArgumentException.class,
                () -> ArenaFillDiagnostics.fromFillOrder(0, 0, List.of()));
    }

    @Test
    void aFillThatMetNoWallAboveItsHeightHasNothingToReportAboutOne() {
        ArenaFillDiagnostics diagnostics = fromCells(0, 0, 0, 0, 5, 5);

        assertFalse(diagnostics.hasHeightMismatch());
        assertEquals(0, diagnostics.getHeightMismatchColumnCount());
        assertTrue(diagnostics.getHeightMismatchColumns().isEmpty());
        assertTrue(diagnostics.getNearestHeightMismatchColumn().isEmpty());
        assertEquals("", diagnostics.describeHeightMismatchColumns());
    }

    @Test
    void namesTheColumnsWithAWallAboveTheHeightNearestFirst() {
        ArenaFillDiagnostics diagnostics = withHeightMismatches(
                new ArenaHeightMismatchColumn(40, 0, 70, 64),
                new ArenaHeightMismatchColumn(0, 6, 66, 64),
                new ArenaHeightMismatchColumn(3, 4, 80, 64),
                new ArenaHeightMismatchColumn(-20, 20, 65, 64));

        List<ArenaHeightMismatchColumn> nearestColumns = diagnostics.findNearestHeightMismatchColumns(3);

        assertEquals(3, nearestColumns.size());
        assertEquals(3, nearestColumns.get(0).blockX());
        assertEquals(4, nearestColumns.get(0).blockZ());
        assertEquals(0, nearestColumns.get(1).blockX());
        assertEquals(6, nearestColumns.get(1).blockZ());
        assertEquals(-20, nearestColumns.get(2).blockX());
        assertEquals(20, nearestColumns.get(2).blockZ());
        assertEquals(new ArenaHeightMismatchColumn(3, 4, 80, 64),
                diagnostics.getNearestHeightMismatchColumn().orElseThrow());
    }

    @Test
    void prefersTheEarlierOfTwoEquallyFarColumnsWithAWallAboveTheHeight() {
        ArenaFillDiagnostics diagnostics = withHeightMismatches(
                new ArenaHeightMismatchColumn(0, -5, 70, 64),
                new ArenaHeightMismatchColumn(5, 0, 70, 64));

        assertEquals(new ArenaHeightMismatchColumn(0, -5, 70, 64),
                diagnostics.getNearestHeightMismatchColumn().orElseThrow());
    }

    @Test
    void describesTheNearestColumnsWithTheirHeightAndTheDifference() {
        ArenaFillDiagnostics diagnostics = withHeightMismatches(
                new ArenaHeightMismatchColumn(10, 5, 70, 64),
                new ArenaHeightMismatchColumn(10, 6, 71, 64),
                new ArenaHeightMismatchColumn(10, 7, 66, 64),
                new ArenaHeightMismatchColumn(10, 30, 90, 64));

        assertEquals("x/z 10/5 (lowest border block at y 70, 6 above you), "
                        + "x/z 10/6 (lowest border block at y 71, 7 above you) and "
                        + "x/z 10/7 (lowest border block at y 66, 2 above you)",
                diagnostics.describeHeightMismatchColumns());
    }

    @Test
    void countOfColumnsWithAWallAboveTheHeightIsNeverSmallerThanTheSample() {
        ArenaFillDiagnostics diagnostics = ArenaFillDiagnostics.fromFillOrder(0, 0, BorderWall.cells(0, 0),
                List.of(new ArenaHeightMismatchColumn(1, 0, 70, 64),
                        new ArenaHeightMismatchColumn(2, 0, 70, 64)),
                0);

        assertEquals(2, diagnostics.getHeightMismatchColumnCount());
    }

    private ArenaFillDiagnostics withHeightMismatches(ArenaHeightMismatchColumn... heightMismatchColumns) {
        List<ArenaHeightMismatchColumn> sample = List.of(heightMismatchColumns);
        return ArenaFillDiagnostics.fromFillOrder(0, 0, BorderWall.cells(0, 0), sample, sample.size());
    }

    private String directionOf(int seedX, int seedZ, int farthestX, int farthestZ) {
        return fromCells(seedX, seedZ, seedX, seedZ, farthestX, farthestZ).getEscapeDirectionName();
    }

    private ArenaFillDiagnostics fromCells(int seedX, int seedZ, int... cellCoordinatePairs) {
        return ArenaFillDiagnostics.fromFillOrder(seedX, seedZ, BorderWall.cells(cellCoordinatePairs));
    }
}
