package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.BorderColumnReader;
import de.simplepvparena.arena.HorizontalCellCodec;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BorderColumnConnections {

    private static final int[] COLUMN_OFFSETS_X = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] COLUMN_OFFSETS_Z = {0, 0, 1, -1, 1, -1, 1, -1};

    private static final long NO_BORDER_BLOCK = Long.MIN_VALUE;

    private BorderColumnConnections() {
    }

    public static List<BorderWallConnection> fromShownBlocks(long[] shownBorderBlocks,
                                                             BorderColumnReader borderColumns,
                                                             int maximumConnectionCount) {
        if (maximumConnectionCount < 0) {
            throw new IllegalArgumentException("maximumConnectionCount must not be negative");
        }
        Set<BorderWallConnection> connections = new LinkedHashSet<>();
        Set<Long> visitedColumns = new LinkedHashSet<>();
        Map<Long, Long> columnTops = new HashMap<>();
        for (long shownBorderBlock : shownBorderBlocks) {
            int blockX = BlockPositionCodec.decodeX(shownBorderBlock);
            int blockZ = BlockPositionCodec.decodeZ(shownBorderBlock);
            if (!visitedColumns.add(HorizontalCellCodec.encode(blockX, blockZ))) {
                continue;
            }
            long columnTop = readColumnTop(columnTops, borderColumns, blockX, blockZ);
            if (columnTop == NO_BORDER_BLOCK) {
                continue;
            }
            for (int offsetIndex = 0; offsetIndex < COLUMN_OFFSETS_X.length; offsetIndex++) {
                long neighbourTop = readColumnTop(columnTops, borderColumns,
                        blockX + COLUMN_OFFSETS_X[offsetIndex], blockZ + COLUMN_OFFSETS_Z[offsetIndex]);
                if (neighbourTop == NO_BORDER_BLOCK) {
                    continue;
                }
                if (connections.size() >= maximumConnectionCount) {
                    return List.copyOf(connections);
                }
                connections.add(BorderWallConnection.between(columnTop, neighbourTop));
            }
        }
        return List.copyOf(connections);
    }

    private static long readColumnTop(Map<Long, Long> columnTops, BorderColumnReader borderColumns,
                                      int blockX, int blockZ) {
        if (isOutsideCoordinateRange(blockX) || isOutsideCoordinateRange(blockZ)) {
            return NO_BORDER_BLOCK;
        }
        return columnTops.computeIfAbsent(HorizontalCellCodec.encode(blockX, blockZ),
                column -> encodeHighestBorderBlock(borderColumns, blockX, blockZ));
    }

    private static long encodeHighestBorderBlock(BorderColumnReader borderColumns, int blockX, int blockZ) {
        int[] borderHeights = borderColumns.readBorderHeights(blockX, blockZ);
        if (borderHeights.length == 0) {
            return NO_BORDER_BLOCK;
        }
        int highestHeight = borderHeights[0];
        for (int borderHeight : borderHeights) {
            highestHeight = Math.max(highestHeight, borderHeight);
        }
        return BlockPositionCodec.encode(blockX, highestHeight, blockZ);
    }

    private static boolean isOutsideCoordinateRange(int horizontalCoordinate) {
        return horizontalCoordinate < BlockPositionCodec.MINIMUM_HORIZONTAL_COORDINATE
                || horizontalCoordinate > BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE;
    }
}
