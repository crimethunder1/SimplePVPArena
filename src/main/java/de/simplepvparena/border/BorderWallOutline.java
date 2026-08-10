package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.BorderColumnReader;
import de.simplepvparena.arena.HorizontalCellCodec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BorderWallOutline {

    public static final int DEFAULT_MAXIMUM_CONNECTION_COUNT = 1200;

    private static final int[] COLUMN_OFFSETS_X = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] COLUMN_OFFSETS_Z = {0, 0, 1, -1, 1, -1, 1, -1};

    private static final int[] NEIGHBOUR_OFFSETS_X = {1, -1, 0, 0, 1, 1, -1, -1, 0, 0};
    private static final int[] NEIGHBOUR_OFFSETS_Y = {0, 0, 0, 0, 0, 0, 0, 0, 1, -1};
    private static final int[] NEIGHBOUR_OFFSETS_Z = {0, 0, 1, -1, 1, -1, 1, -1, 0, 0};

    private final long[] wallBlocks;
    private final List<BorderWallConnection> connections;
    private final boolean truncated;

    private BorderWallOutline(long[] wallBlocks, List<BorderWallConnection> connections, boolean truncated) {
        this.wallBlocks = wallBlocks;
        this.connections = List.copyOf(connections);
        this.truncated = truncated;
    }

    public static BorderWallOutline around(ArenaShape shape, BorderColumnReader borderColumns) {
        return around(shape, borderColumns, DEFAULT_MAXIMUM_CONNECTION_COUNT);
    }

    public static BorderWallOutline around(ArenaShape shape, BorderColumnReader borderColumns,
                                           int maximumConnectionCount) {
        if (maximumConnectionCount < 0) {
            throw new IllegalArgumentException("maximumConnectionCount must not be negative");
        }
        long[] wallBlocks = findWallBlocks(shape, borderColumns);
        return findConnections(wallBlocks, maximumConnectionCount);
    }

    static long[] findWallBlocks(ArenaShape shape, BorderColumnReader borderColumns) {
        Set<Long> wallColumns = new LinkedHashSet<>();
        for (long cell : shape.getCells()) {
            int cellX = HorizontalCellCodec.decodeX(cell);
            int cellZ = HorizontalCellCodec.decodeZ(cell);
            for (int offsetIndex = 0; offsetIndex < COLUMN_OFFSETS_X.length; offsetIndex++) {
                int columnX = cellX + COLUMN_OFFSETS_X[offsetIndex];
                int columnZ = cellZ + COLUMN_OFFSETS_Z[offsetIndex];
                if (!shape.containsCell(columnX, columnZ)) {
                    wallColumns.add(HorizontalCellCodec.encode(columnX, columnZ));
                }
            }
        }
        List<Long> wallBlocks = new ArrayList<>();
        for (long wallColumn : wallColumns) {
            int columnX = HorizontalCellCodec.decodeX(wallColumn);
            int columnZ = HorizontalCellCodec.decodeZ(wallColumn);
            int[] borderHeights = borderColumns.readBorderHeights(columnX, columnZ);
            if (!enclosesTheShape(borderHeights, shape.getReferenceY())) {
                continue;
            }
            for (int borderHeight : borderHeights) {
                if (borderHeight <= shape.getWallTopY()) {
                    wallBlocks.add(BlockPositionCodec.encode(columnX, borderHeight, columnZ));
                }
            }
        }
        return toSortedArray(wallBlocks);
    }

    static BorderWallOutline findConnections(long[] wallBlocks, int maximumConnectionCount) {
        Set<Long> wallBlockLookup = new HashSet<>(Math.max(16, wallBlocks.length * 2));
        for (long wallBlock : wallBlocks) {
            wallBlockLookup.add(wallBlock);
        }
        List<BorderWallConnection> connections = new ArrayList<>();
        for (long wallBlock : wallBlocks) {
            int blockX = BlockPositionCodec.decodeX(wallBlock);
            int blockY = BlockPositionCodec.decodeY(wallBlock);
            int blockZ = BlockPositionCodec.decodeZ(wallBlock);
            for (int offsetIndex = 0; offsetIndex < NEIGHBOUR_OFFSETS_X.length; offsetIndex++) {
                long neighbour = encodeNeighbour(blockX + NEIGHBOUR_OFFSETS_X[offsetIndex],
                        blockY + NEIGHBOUR_OFFSETS_Y[offsetIndex],
                        blockZ + NEIGHBOUR_OFFSETS_Z[offsetIndex]);
                if (wallBlock >= neighbour || !wallBlockLookup.contains(neighbour)) {
                    continue;
                }
                if (connections.size() >= maximumConnectionCount) {
                    return new BorderWallOutline(wallBlocks, connections, true);
                }
                connections.add(BorderWallConnection.between(wallBlock, neighbour));
            }
        }
        return new BorderWallOutline(wallBlocks, connections, false);
    }

    public long[] getWallBlocks() {
        return wallBlocks.clone();
    }

    public int getWallBlockCount() {
        return wallBlocks.length;
    }

    public List<BorderWallConnection> getConnections() {
        return connections;
    }

    public boolean isEmpty() {
        return wallBlocks.length == 0;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public String describe() {
        String description = wallBlocks.length + " border blocks connected by " + connections.size() + " lines";
        return truncated ? description + ", the line limit was reached and only a part of the wall is shown"
                : description;
    }

    private static boolean enclosesTheShape(int[] borderHeights, int referenceY) {
        for (int borderHeight : borderHeights) {
            if (borderHeight <= referenceY) {
                return true;
            }
        }
        return false;
    }

    private static long encodeNeighbour(int blockX, int blockY, int blockZ) {
        if (blockY < BlockPositionCodec.MINIMUM_VERTICAL_COORDINATE
                || blockY > BlockPositionCodec.MAXIMUM_VERTICAL_COORDINATE
                || blockX < BlockPositionCodec.MINIMUM_HORIZONTAL_COORDINATE
                || blockX > BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE
                || blockZ < BlockPositionCodec.MINIMUM_HORIZONTAL_COORDINATE
                || blockZ > BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE) {
            return Long.MIN_VALUE;
        }
        return BlockPositionCodec.encode(blockX, blockY, blockZ);
    }

    private static long[] toSortedArray(List<Long> positions) {
        long[] sortedPositions = new long[positions.size()];
        int positionIndex = 0;
        for (long position : positions) {
            sortedPositions[positionIndex++] = position;
        }
        Arrays.sort(sortedPositions);
        return sortedPositions;
    }
}
