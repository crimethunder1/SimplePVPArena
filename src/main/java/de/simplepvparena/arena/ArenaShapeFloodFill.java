package de.simplepvparena.arena;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ArenaShapeFloodFill {

    public static final int DEFAULT_MAXIMUM_CELL_COUNT = 50_000;

    public static final int MAXIMUM_DEBUG_CELL_COUNT = 5_000_000;

    public static final String MAXIMUM_CELL_COUNT_CONFIGURATION_PATH = "arena.shape-maximum-cells";

    private static final int[] NEIGHBOUR_OFFSETS_X = {1, -1, 0, 0};
    private static final int[] NEIGHBOUR_OFFSETS_Z = {0, 0, 1, -1};

    private ArenaShapeFloodFill() {
    }

    public static ArenaShapeCalculation calculate(String worldName, int seedX, int seedY, int seedZ,
                                                  int maximumCellCount, BorderColumnReader borderColumns) {
        if (maximumCellCount < 1) {
            throw new IllegalArgumentException("maximumCellCount must be at least one");
        }
        if (barsColumn(borderColumns.readBorderHeights(seedX, seedZ), seedY)) {
            return ArenaShapeCalculation.seedInsideBorder();
        }
        Set<Long> filledCells = new LinkedHashSet<>();
        Deque<Long> pendingCells = new ArrayDeque<>();
        long seedCell = HorizontalCellCodec.encode(seedX, seedZ);
        filledCells.add(seedCell);
        pendingCells.add(seedCell);
        int wallTopY = seedY;
        while (!pendingCells.isEmpty()) {
            long cell = pendingCells.poll();
            int cellX = HorizontalCellCodec.decodeX(cell);
            int cellZ = HorizontalCellCodec.decodeZ(cell);
            for (int direction = 0; direction < NEIGHBOUR_OFFSETS_X.length; direction++) {
                int neighbourX = cellX + NEIGHBOUR_OFFSETS_X[direction];
                int neighbourZ = cellZ + NEIGHBOUR_OFFSETS_Z[direction];
                long neighbourCell = HorizontalCellCodec.encode(neighbourX, neighbourZ);
                if (filledCells.contains(neighbourCell)) {
                    continue;
                }
                int[] borderHeights = borderColumns.readBorderHeights(neighbourX, neighbourZ);
                if (barsColumn(borderHeights, seedY)) {
                    wallTopY = Math.max(wallTopY, highestHeight(borderHeights));
                    continue;
                }
                if (filledCells.size() >= maximumCellCount) {
                    return ArenaShapeCalculation.stoppedAtCellLimit(maximumCellCount,
                            ArenaFillDiagnostics.fromFillOrder(seedX, seedZ, filledCells));
                }
                filledCells.add(neighbourCell);
                pendingCells.add(neighbourCell);
            }
        }
        return ArenaShapeCalculation.filled(new ArenaShape(worldName, seedY, wallTopY, toArray(filledCells)));
    }

    private static boolean barsColumn(int[] borderHeights, int fillHeight) {
        for (int borderHeight : borderHeights) {
            if (borderHeight <= fillHeight) {
                return true;
            }
        }
        return false;
    }

    private static int highestHeight(int[] borderHeights) {
        int highestHeight = Integer.MIN_VALUE;
        for (int borderHeight : borderHeights) {
            highestHeight = Math.max(highestHeight, borderHeight);
        }
        return highestHeight;
    }

    private static long[] toArray(Set<Long> cells) {
        long[] encodedCells = new long[cells.size()];
        int cellIndex = 0;
        for (long cell : cells) {
            encodedCells[cellIndex++] = cell;
        }
        return encodedCells;
    }
}
