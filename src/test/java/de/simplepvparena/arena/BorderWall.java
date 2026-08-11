package de.simplepvparena.arena;

import de.simplepvparena.border.BlockedColumnLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BorderWall implements BorderColumnReader {

    private final Map<Long, List<Integer>> heightsByCell = new HashMap<>();

    BorderWall add(int blockX, int blockZ, int borderHeight) {
        heightsByCell.computeIfAbsent(HorizontalCellCodec.encode(blockX, blockZ), cell -> new ArrayList<>())
                .add(borderHeight);
        return this;
    }

    BorderWall addRectangleOutline(int minimumX, int minimumZ, int maximumX, int maximumZ, int borderHeight) {
        for (int blockX = minimumX; blockX <= maximumX; blockX++) {
            add(blockX, minimumZ, borderHeight);
            add(blockX, maximumZ, borderHeight);
        }
        for (int blockZ = minimumZ; blockZ <= maximumZ; blockZ++) {
            add(minimumX, blockZ, borderHeight);
            add(maximumX, blockZ, borderHeight);
        }
        return this;
    }

    static BorderWall around(Set<Long> roomCells, int borderHeight) {
        BorderWall wall = new BorderWall();
        for (long roomCell : roomCells) {
            int cellX = HorizontalCellCodec.decodeX(roomCell);
            int cellZ = HorizontalCellCodec.decodeZ(roomCell);
            addWallCell(wall, roomCells, cellX + 1, cellZ, borderHeight);
            addWallCell(wall, roomCells, cellX - 1, cellZ, borderHeight);
            addWallCell(wall, roomCells, cellX, cellZ + 1, borderHeight);
            addWallCell(wall, roomCells, cellX, cellZ - 1, borderHeight);
        }
        return wall;
    }

    static Set<Long> cells(int... coordinatePairs) {
        Set<Long> cells = new LinkedHashSet<>();
        for (int pairIndex = 0; pairIndex < coordinatePairs.length; pairIndex += 2) {
            cells.add(HorizontalCellCodec.encode(coordinatePairs[pairIndex], coordinatePairs[pairIndex + 1]));
        }
        return cells;
    }

    BorderWall removeColumn(int blockX, int blockZ) {
        heightsByCell.remove(HorizontalCellCodec.encode(blockX, blockZ));
        return this;
    }

    BorderWall raiseColumn(int blockX, int blockZ, int borderHeight) {
        return removeColumn(blockX, blockZ).add(blockX, blockZ, borderHeight);
    }

    BlockedColumnLookup asBlockedColumns() {
        return (blockX, blockZ) -> !heightsByCell
                .getOrDefault(HorizontalCellCodec.encode(blockX, blockZ), List.of()).isEmpty();
    }

    boolean hasTwoColumnsTouchingOnlyDiagonally() {
        for (long cell : heightsByCell.keySet()) {
            int cellX = HorizontalCellCodec.decodeX(cell);
            int cellZ = HorizontalCellCodec.decodeZ(cell);
            if (touchesOnlyDiagonally(cellX, cellZ, cellX + 1, cellZ + 1)
                    || touchesOnlyDiagonally(cellX, cellZ, cellX + 1, cellZ - 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean touchesOnlyDiagonally(int firstX, int firstZ, int secondX, int secondZ) {
        BlockedColumnLookup blockedColumns = asBlockedColumns();
        return blockedColumns.isBlockedColumn(firstX, firstZ)
                && blockedColumns.isBlockedColumn(secondX, secondZ)
                && !blockedColumns.isBlockedColumn(secondX, firstZ)
                && !blockedColumns.isBlockedColumn(firstX, secondZ);
    }

    @Override
    public int[] readBorderHeights(int blockX, int blockZ) {
        List<Integer> heights = heightsByCell.get(HorizontalCellCodec.encode(blockX, blockZ));
        if (heights == null) {
            return NO_BORDER_BLOCKS;
        }
        int[] borderHeights = new int[heights.size()];
        for (int heightIndex = 0; heightIndex < borderHeights.length; heightIndex++) {
            borderHeights[heightIndex] = heights.get(heightIndex);
        }
        return borderHeights;
    }

    private static void addWallCell(BorderWall wall, Set<Long> roomCells, int blockX, int blockZ, int borderHeight) {
        if (!roomCells.contains(HorizontalCellCodec.encode(blockX, blockZ))) {
            wall.add(blockX, blockZ, borderHeight);
        }
    }
}
