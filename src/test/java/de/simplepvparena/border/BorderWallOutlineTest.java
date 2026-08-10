package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.BorderColumnReader;
import de.simplepvparena.arena.HorizontalCellCodec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderWallOutlineTest {

    private static final String WORLD = "event";
    private static final int FLOOR_Y = 64;

    @Test
    void collectsTheWholeRingAroundTheShape() {
        BorderWallOutline outline = BorderWallOutline.around(squareRoom(FLOOR_Y),
                rectangleOutline(0, 0, 3, 3, FLOOR_Y));

        assertEquals(12, outline.getWallBlockCount());
        assertFalse(outline.isEmpty());
        assertTrue(wallBlocksOf(outline).contains(BlockPositionCodec.encode(0, FLOOR_Y, 0)), "a corner of the ring");
        assertTrue(wallBlocksOf(outline).contains(BlockPositionCodec.encode(0, FLOOR_Y, 1)), "a side of the ring");
        assertFalse(wallBlocksOf(outline).contains(BlockPositionCodec.encode(1, FLOOR_Y, 1)),
                "a cell of the room itself never belongs to the wall");
    }

    @Test
    void leavesOutAColumnWhoseBorderBlocksAllStandAboveTheFloor() {
        WallColumns columns = rectangleOutline(0, 0, 3, 3, FLOOR_Y);
        columns.replace(0, 1, FLOOR_Y + 10);

        BorderWallOutline outline = BorderWallOutline.around(squareRoom(FLOOR_Y), columns);

        assertEquals(11, outline.getWallBlockCount());
        assertFalse(wallBlocksOf(outline).contains(BlockPositionCodec.encode(0, FLOOR_Y + 10, 1)));
    }

    @Test
    void leavesOutTheBorderBlocksAboveTheTopOfTheWall() {
        WallColumns columns = rectangleOutline(0, 0, 3, 3, FLOOR_Y);
        columns.add(0, 1, FLOOR_Y + 1);
        columns.add(0, 1, FLOOR_Y + 5);

        BorderWallOutline outline = BorderWallOutline.around(
                new ArenaShape(WORLD, FLOOR_Y, FLOOR_Y + 1, roomCells()), columns);

        assertTrue(wallBlocksOf(outline).contains(BlockPositionCodec.encode(0, FLOOR_Y + 1, 1)));
        assertFalse(wallBlocksOf(outline).contains(BlockPositionCodec.encode(0, FLOOR_Y + 5, 1)));
        assertEquals(13, outline.getWallBlockCount());
    }

    @Test
    void connectsEveryPairOfTouchingBorderBlocksExactlyOnce() {
        BorderWallOutline outline = BorderWallOutline.around(squareRoom(FLOOR_Y),
                rectangleOutline(0, 0, 3, 3, FLOOR_Y));

        assertEquals(16, outline.getConnections().size());
        assertEquals(outline.getConnections().size(), new LinkedHashSet<>(outline.getConnections()).size());
        assertTrue(outline.getConnections().contains(BorderWallConnection.between(
                BlockPositionCodec.encode(0, FLOOR_Y, 0), BlockPositionCodec.encode(0, FLOOR_Y, 1))),
                "two blocks next to each other");
        assertTrue(outline.getConnections().contains(BorderWallConnection.between(
                BlockPositionCodec.encode(0, FLOOR_Y, 0), BlockPositionCodec.encode(1, FLOOR_Y, 0))),
                "the block around the corner");
        assertFalse(outline.getConnections().contains(BorderWallConnection.between(
                BlockPositionCodec.encode(0, FLOOR_Y, 0), BlockPositionCodec.encode(0, FLOOR_Y, 2))),
                "two blocks with a block between them do not touch");
    }

    @Test
    void connectsTheDiagonalAndTheStackedNeighbour() {
        WallColumns columns = new WallColumns()
                .with(0, 0, FLOOR_Y)
                .with(1, 1, FLOOR_Y)
                .with(0, 1, FLOOR_Y, FLOOR_Y + 1);
        List<BorderWallConnection> connections = BorderWallOutline
                .around(oneCellRoom(), columns).getConnections();

        assertTrue(connections.contains(BorderWallConnection.between(
                BlockPositionCodec.encode(0, FLOOR_Y, 0), BlockPositionCodec.encode(1, FLOOR_Y, 1))),
                "the diagonal neighbour");
        assertTrue(connections.contains(BorderWallConnection.between(
                BlockPositionCodec.encode(0, FLOOR_Y, 1), BlockPositionCodec.encode(0, FLOOR_Y + 1, 1))),
                "the block stacked on top of another one");
    }

    @Test
    void stopsCollectingLinesAtTheLimit() {
        BorderWallOutline outline = BorderWallOutline.around(squareRoom(FLOOR_Y),
                rectangleOutline(0, 0, 3, 3, FLOOR_Y), 3);

        assertEquals(3, outline.getConnections().size());
        assertTrue(outline.isTruncated());
        assertTrue(outline.describe().contains("line limit"));
        assertFalse(BorderWallOutline.around(squareRoom(FLOOR_Y), rectangleOutline(0, 0, 3, 3, FLOOR_Y))
                .isTruncated());
    }

    @Test
    void reportsAnEmptyOutlineWithoutASingleBorderBlock() {
        BorderWallOutline outline = BorderWallOutline.around(squareRoom(FLOOR_Y), new WallColumns());

        assertTrue(outline.isEmpty());
        assertEquals(0, outline.getConnections().size());
        assertFalse(outline.isTruncated());
    }

    @Test
    void refusesANegativeLineLimit() {
        assertThrows(IllegalArgumentException.class, () -> BorderWallOutline.around(squareRoom(FLOOR_Y),
                rectangleOutline(0, 0, 3, 3, FLOOR_Y), -1));
    }

    private static ArenaShape squareRoom(int floorY) {
        return new ArenaShape(WORLD, floorY, floorY, roomCells());
    }

    private static long[] roomCells() {
        return new long[] {
                HorizontalCellCodec.encode(1, 1), HorizontalCellCodec.encode(1, 2),
                HorizontalCellCodec.encode(2, 1), HorizontalCellCodec.encode(2, 2)};
    }

    private static ArenaShape oneCellRoom() {
        return new ArenaShape(WORLD, FLOOR_Y, FLOOR_Y + 1, new long[] {HorizontalCellCodec.encode(1, 0)});
    }

    private static Set<Long> wallBlocksOf(BorderWallOutline outline) {
        Set<Long> wallBlocks = new LinkedHashSet<>();
        for (long wallBlock : outline.getWallBlocks()) {
            wallBlocks.add(wallBlock);
        }
        return wallBlocks;
    }

    private static WallColumns rectangleOutline(int minimumX, int minimumZ, int maximumX, int maximumZ,
                                                int borderHeight) {
        WallColumns columns = new WallColumns();
        for (int blockX = minimumX; blockX <= maximumX; blockX++) {
            columns.add(blockX, minimumZ, borderHeight);
            columns.add(blockX, maximumZ, borderHeight);
        }
        for (int blockZ = minimumZ + 1; blockZ < maximumZ; blockZ++) {
            columns.add(minimumX, blockZ, borderHeight);
            columns.add(maximumX, blockZ, borderHeight);
        }
        return columns;
    }

    private static final class WallColumns implements BorderColumnReader {

        private final Map<Long, List<Integer>> heightsByColumn = new HashMap<>();

        WallColumns with(int blockX, int blockZ, int... borderHeights) {
            for (int borderHeight : borderHeights) {
                add(blockX, blockZ, borderHeight);
            }
            return this;
        }

        void add(int blockX, int blockZ, int borderHeight) {
            heightsByColumn.computeIfAbsent(HorizontalCellCodec.encode(blockX, blockZ),
                    column -> new ArrayList<>()).add(borderHeight);
        }

        void replace(int blockX, int blockZ, int borderHeight) {
            heightsByColumn.remove(HorizontalCellCodec.encode(blockX, blockZ));
            add(blockX, blockZ, borderHeight);
        }

        @Override
        public int[] readBorderHeights(int blockX, int blockZ) {
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
    }
}
