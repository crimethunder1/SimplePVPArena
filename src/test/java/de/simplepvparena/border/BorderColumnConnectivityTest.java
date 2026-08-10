package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.HorizontalCellCodec;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BorderColumnConnectivityTest {

    @Test
    void reportsABorderBlockWithoutASingleNeighbourAsIsolated() {
        BlockedColumnLookup wall = wall(10, 20);

        assertEquals(0, BorderColumnConnectivity.countConnectedNeighbours(wall, 10, 20));
        assertEquals(BorderConnectivity.ISOLATED, BorderColumnConnectivity.of(wall, 10, 20));
    }

    @Test
    void reportsAColumnInAWorldWithoutAnyMarkAsIsolated() {
        assertEquals(BorderConnectivity.ISOLATED,
                BorderColumnConnectivity.of(BlockedColumnLookup.NOTHING_BLOCKED, 10, 20));
    }

    @Test
    void reportsTheEndOfAWallAsNormal() {
        BlockedColumnLookup wall = wall(10, 20, 11, 20);

        assertEquals(1, BorderColumnConnectivity.countConnectedNeighbours(wall, 10, 20));
        assertEquals(BorderConnectivity.NORMAL, BorderColumnConnectivity.of(wall, 10, 20));
        assertEquals(BorderConnectivity.NORMAL, BorderColumnConnectivity.of(wall, 11, 20),
                "both ends of a wall of two blocks");
    }

    @Test
    void countsASingleDiagonalNeighbourLikeAStraightOne() {
        BlockedColumnLookup diagonalPair = wall(10, 20, 11, 21);

        assertEquals(1, BorderColumnConnectivity.countConnectedNeighbours(diagonalPair, 10, 20));
        assertEquals(BorderConnectivity.NORMAL, BorderColumnConnectivity.of(diagonalPair, 10, 20));
        assertEquals(BorderConnectivity.NORMAL, BorderColumnConnectivity.of(diagonalPair, 11, 21));
    }

    @Test
    void reportsTheMiddleOfAStraightWallAsWellConnected() {
        BlockedColumnLookup wall = wall(9, 20, 10, 20, 11, 20);

        assertEquals(2, BorderColumnConnectivity.countConnectedNeighbours(wall, 10, 20));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderColumnConnectivity.of(wall, 10, 20));
    }

    @Test
    void reportsAColumnWithTwoDiagonalNeighboursAsWellConnected() {
        BlockedColumnLookup wall = wall(9, 19, 10, 20, 11, 21);

        assertEquals(2, BorderColumnConnectivity.countConnectedNeighbours(wall, 10, 20));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderColumnConnectivity.of(wall, 10, 20));
    }

    @Test
    void reportsAColumnWithOneStraightAndOneDiagonalNeighbourAsWellConnected() {
        BlockedColumnLookup wall = wall(10, 20, 11, 20, 9, 21);

        assertEquals(2, BorderColumnConnectivity.countConnectedNeighbours(wall, 10, 20));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderColumnConnectivity.of(wall, 10, 20));
    }

    @Test
    void countsAllEightNeighbours() {
        BlockedColumnLookup blockOfColumns = wall(
                9, 19, 10, 19, 11, 19,
                9, 20, 10, 20, 11, 20,
                9, 21, 10, 21, 11, 21);

        assertEquals(8, BorderColumnConnectivity.countConnectedNeighbours(blockOfColumns, 10, 20));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderColumnConnectivity.of(blockOfColumns, 10, 20));
    }

    @Test
    void ignoresColumnsThatAreTwoStepsAway() {
        BlockedColumnLookup wall = wall(10, 20, 12, 20, 10, 22, 8, 18);

        assertEquals(0, BorderColumnConnectivity.countConnectedNeighbours(wall, 10, 20));
        assertEquals(BorderConnectivity.ISOLATED, BorderColumnConnectivity.of(wall, 10, 20));
    }

    @Test
    void treatsNeighbouringBorderBlocksOfCompletelyDifferentHeightsAsConnected() {
        BlockedColumnLookup staircase = wallOfBlocks(
                9, 64, 20,
                10, 91, 20,
                11, 12, 20);

        assertEquals(2, BorderColumnConnectivity.countConnectedNeighbours(staircase, 10, 20));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderColumnConnectivity.of(staircase, 10, 20));
    }

    @Test
    void treatsADiagonalStaircaseOfDifferentHeightsAsConnected() {
        BlockedColumnLookup staircase = wallOfBlocks(
                9, -60, 19,
                10, 5, 20,
                11, 200, 21);

        assertEquals(2, BorderColumnConnectivity.countConnectedNeighbours(staircase, 10, 20));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderColumnConnectivity.of(staircase, 10, 20));
    }

    @Test
    void countsAColumnWithSeveralStackedBorderBlocksOnceOnly() {
        BlockedColumnLookup tower = wallOfBlocks(
                10, 64, 20,
                11, 64, 20,
                11, 65, 20,
                11, 66, 20);

        assertEquals(1, BorderColumnConnectivity.countConnectedNeighbours(tower, 10, 20));
        assertEquals(BorderConnectivity.NORMAL, BorderColumnConnectivity.of(tower, 10, 20));
    }

    @Test
    void neverCountsTheColumnItself() {
        assertEquals(0, BorderColumnConnectivity.countConnectedNeighbours(wall(10, 20), 10, 20));
    }

    @Test
    void countsNeighboursBeyondTheCoordinateRangeAsFree() {
        int edgeX = BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE;
        BlockedColumnLookup wall = wall(edgeX, 20, edgeX - 1, 20);

        assertEquals(1, BorderColumnConnectivity.countConnectedNeighbours(wall, edgeX, 20));
        assertEquals(BorderConnectivity.NORMAL, BorderColumnConnectivity.of(wall, edgeX, 20));
    }

    @Test
    void mapsEveryNeighbourCountToItsState() {
        assertEquals(BorderConnectivity.ISOLATED, BorderConnectivity.ofConnectedNeighbourCount(0));
        assertEquals(BorderConnectivity.NORMAL, BorderConnectivity.ofConnectedNeighbourCount(1));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderConnectivity.ofConnectedNeighbourCount(2));
        assertEquals(BorderConnectivity.WELL_CONNECTED, BorderConnectivity.ofConnectedNeighbourCount(8));
    }

    @Test
    void rejectsANegativeNeighbourCount() {
        assertThrows(IllegalArgumentException.class, () -> BorderConnectivity.ofConnectedNeighbourCount(-1));
    }

    private static BlockedColumnLookup wall(int... coordinatePairs) {
        Set<Long> barredColumns = new LinkedHashSet<>();
        for (int pairIndex = 0; pairIndex < coordinatePairs.length; pairIndex += 2) {
            barredColumns.add(HorizontalCellCodec.encode(coordinatePairs[pairIndex], coordinatePairs[pairIndex + 1]));
        }
        return (blockX, blockZ) -> barredColumns.contains(HorizontalCellCodec.encode(blockX, blockZ));
    }

    private static BlockedColumnLookup wallOfBlocks(int... coordinateTriples) {
        BorderMarkIndex borderMarks = new BorderMarkIndex();
        for (int tripleIndex = 0; tripleIndex < coordinateTriples.length; tripleIndex += 3) {
            borderMarks.mark("world", coordinateTriples[tripleIndex], coordinateTriples[tripleIndex + 1],
                    coordinateTriples[tripleIndex + 2]);
        }
        return borderMarks.lookupIn("world");
    }
}
