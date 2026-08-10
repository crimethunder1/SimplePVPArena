package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.BorderColumnReader;
import de.simplepvparena.arena.HorizontalCellCodec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderColumnConnectionsTest {

    private static final int FLOOR_Y = 64;
    private static final int NO_LIMIT = 1000;

    @Test
    void connectsTwoBorderBlocksThatStandNextToEachOther() {
        WallColumns wall = new WallColumns().with(10, 20, FLOOR_Y).with(11, 20, FLOOR_Y);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20));

        assertEquals(List.of(BorderWallConnection.between(
                BlockPositionCodec.encode(10, FLOOR_Y, 20), BlockPositionCodec.encode(11, FLOOR_Y, 20))),
                connections);
    }

    @Test
    void connectsTheDiagonalNeighbour() {
        WallColumns wall = new WallColumns().with(10, 20, FLOOR_Y).with(11, 21, FLOOR_Y);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20));

        assertEquals(List.of(BorderWallConnection.between(
                BlockPositionCodec.encode(10, FLOOR_Y, 20), BlockPositionCodec.encode(11, FLOOR_Y, 21))),
                connections);
    }

    @Test
    void connectsAllEightNeighboursOfAColumn() {
        WallColumns wall = new WallColumns()
                .with(9, 19, FLOOR_Y).with(10, 19, FLOOR_Y).with(11, 19, FLOOR_Y)
                .with(9, 20, FLOOR_Y).with(10, 20, FLOOR_Y).with(11, 20, FLOOR_Y)
                .with(9, 21, FLOOR_Y).with(10, 21, FLOOR_Y).with(11, 21, FLOOR_Y);

        assertEquals(8, connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20)).size());
    }

    @Test
    void leavesOutAColumnThatIsTwoStepsAway() {
        WallColumns wall = new WallColumns().with(10, 20, FLOOR_Y).with(12, 20, FLOOR_Y);

        assertTrue(connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20)).isEmpty());
    }

    @Test
    void drawsNoLineAtAllForABorderBlockWithoutASingleNeighbour() {
        WallColumns wall = new WallColumns().with(10, 20, FLOOR_Y);

        assertTrue(connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20)).isEmpty(),
                "the missing line is the signal, an isolated block simply has none");
    }

    @Test
    void connectsAPairOfShownBlocksExactlyOnce() {
        WallColumns wall = new WallColumns()
                .with(10, 20, FLOOR_Y).with(11, 20, FLOOR_Y).with(12, 20, FLOOR_Y);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT,
                block(10, FLOOR_Y, 20), block(11, FLOOR_Y, 20), block(12, FLOOR_Y, 20));

        assertEquals(2, connections.size());
        assertEquals(connections.size(), new LinkedHashSet<>(connections).size());
        assertTrue(connections.contains(BorderWallConnection.between(
                BlockPositionCodec.encode(10, FLOOR_Y, 20), BlockPositionCodec.encode(11, FLOOR_Y, 20))));
        assertTrue(connections.contains(BorderWallConnection.between(
                BlockPositionCodec.encode(11, FLOOR_Y, 20), BlockPositionCodec.encode(12, FLOOR_Y, 20))));
    }

    @Test
    void connectsAColumnWithSeveralStackedBorderBlocksOnceOnly() {
        WallColumns wall = new WallColumns()
                .with(10, 20, FLOOR_Y, FLOOR_Y + 1, FLOOR_Y + 2)
                .with(11, 20, FLOOR_Y);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT,
                block(10, FLOOR_Y, 20), block(10, FLOOR_Y + 1, 20), block(10, FLOOR_Y + 2, 20));

        assertEquals(List.of(BorderWallConnection.between(
                BlockPositionCodec.encode(10, FLOOR_Y + 2, 20), BlockPositionCodec.encode(11, FLOOR_Y, 20))),
                connections);
    }

    @Test
    void connectsNeighbouringColumnsOfCompletelyDifferentHeights() {
        WallColumns staircase = new WallColumns()
                .with(9, 20, 12).with(10, 20, 91).with(11, 20, -60);

        List<BorderWallConnection> connections = connectionsOf(staircase, NO_LIMIT, block(10, 91, 20));

        assertEquals(2, connections.size());
        assertTrue(connections.contains(BorderWallConnection.between(
                BlockPositionCodec.encode(10, 91, 20), BlockPositionCodec.encode(9, 12, 20))));
        assertTrue(connections.contains(BorderWallConnection.between(
                BlockPositionCodec.encode(10, 91, 20), BlockPositionCodec.encode(11, -60, 20))));
    }

    @Test
    void connectsADiagonalStaircaseOfDifferentHeights() {
        WallColumns staircase = new WallColumns()
                .with(9, 19, -60).with(10, 20, 5).with(11, 21, 200);

        List<BorderWallConnection> connections = connectionsOf(staircase, NO_LIMIT, block(10, 5, 20));

        assertEquals(2, connections.size());
    }

    @Test
    void anchorsBothEndsOfALineAtTheTopOfTheirColumn() {
        WallColumns wall = new WallColumns()
                .with(10, 20, FLOOR_Y, FLOOR_Y + 3)
                .with(11, 20, FLOOR_Y, FLOOR_Y + 1, FLOOR_Y + 7);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20));

        assertEquals(List.of(BorderWallConnection.between(
                BlockPositionCodec.encode(10, FLOOR_Y + 3, 20), BlockPositionCodec.encode(11, FLOOR_Y + 7, 20))),
                connections);
    }

    @Test
    void connectsANeighbourThatIsNotShownItself() {
        WallColumns wall = new WallColumns().with(10, 20, FLOOR_Y).with(11, 20, FLOOR_Y);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20));

        assertEquals(1, connections.size(), "the line ends at the border block that is not drawn itself");
    }

    @Test
    void ignoresAShownBlockWhoseColumnHoldsNoMarkAnyMore() {
        WallColumns wall = new WallColumns().with(11, 20, FLOOR_Y);

        assertTrue(connectionsOf(wall, NO_LIMIT, block(10, FLOOR_Y, 20)).isEmpty());
    }

    @Test
    void stopsAtTheLineLimit() {
        WallColumns wall = new WallColumns()
                .with(9, 19, FLOOR_Y).with(10, 19, FLOOR_Y).with(11, 19, FLOOR_Y)
                .with(9, 20, FLOOR_Y).with(10, 20, FLOOR_Y).with(11, 20, FLOOR_Y)
                .with(9, 21, FLOOR_Y).with(10, 21, FLOOR_Y).with(11, 21, FLOOR_Y);

        assertEquals(3, connectionsOf(wall, 3, block(10, FLOOR_Y, 20)).size());
        assertTrue(connectionsOf(wall, 0, block(10, FLOOR_Y, 20)).isEmpty(),
                "zero lines switch the feature off");
    }

    @Test
    void refusesANegativeLineLimit() {
        assertThrows(IllegalArgumentException.class, () -> BorderColumnConnections.fromShownBlocks(
                new long[0], new WallColumns(), -1));
    }

    @Test
    void reportsNoConnectionWithoutASingleShownBlock() {
        assertTrue(BorderColumnConnections.fromShownBlocks(new long[0], new WallColumns(), NO_LIMIT).isEmpty());
    }

    @Test
    void connectsNoColumnBeyondTheCoordinateRange() {
        int edgeX = BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE;
        WallColumns wall = new WallColumns().with(edgeX, 20, FLOOR_Y).with(edgeX - 1, 20, FLOOR_Y);

        List<BorderWallConnection> connections = connectionsOf(wall, NO_LIMIT, block(edgeX, FLOOR_Y, 20));

        assertEquals(1, connections.size());
        assertFalse(wall.wasAskedBeyondTheCoordinateRange());
    }

    private static List<BorderWallConnection> connectionsOf(WallColumns wall, int maximumConnectionCount,
                                                            long... shownBorderBlocks) {
        return BorderColumnConnections.fromShownBlocks(shownBorderBlocks, wall, maximumConnectionCount);
    }

    private static long block(int blockX, int blockY, int blockZ) {
        return BlockPositionCodec.encode(blockX, blockY, blockZ);
    }

    private static final class WallColumns implements BorderColumnReader {

        private final Map<Long, List<Integer>> heightsByColumn = new HashMap<>();

        private boolean askedBeyondTheCoordinateRange;

        WallColumns with(int blockX, int blockZ, int... borderHeights) {
            for (int borderHeight : borderHeights) {
                heightsByColumn.computeIfAbsent(HorizontalCellCodec.encode(blockX, blockZ),
                        column -> new ArrayList<>()).add(borderHeight);
            }
            return this;
        }

        boolean wasAskedBeyondTheCoordinateRange() {
            return askedBeyondTheCoordinateRange;
        }

        @Override
        public int[] readBorderHeights(int blockX, int blockZ) {
            if (isOutsideRange(blockX) || isOutsideRange(blockZ)) {
                askedBeyondTheCoordinateRange = true;
            }
            List<Integer> heights = heightsByColumn.get(HorizontalCellCodec.encode(blockX, blockZ));
            if (heights == null) {
                return NO_BORDER_BLOCKS;
            }
            int[] borderHeights = new int[heights.size()];
            for (int heightIndex = 0; heightIndex < borderHeights.length; heightIndex++) {
                borderHeights[heightIndex] = heights.get(heightIndex);
            }
            return borderHeights;
        }

        private static boolean isOutsideRange(int horizontalCoordinate) {
            return horizontalCoordinate < BlockPositionCodec.MINIMUM_HORIZONTAL_COORDINATE
                    || horizontalCoordinate > BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE;
        }
    }
}
