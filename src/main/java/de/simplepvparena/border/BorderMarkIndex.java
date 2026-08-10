package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.BorderColumnReader;
import de.simplepvparena.arena.HorizontalCellCodec;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BorderMarkIndex {

    private final Map<String, Map<Long, long[]>> positionsByColumnByWorld = new LinkedHashMap<>();

    public boolean mark(String worldName, int blockX, int blockY, int blockZ) {
        Objects.requireNonNull(worldName, "worldName");
        long encodedPosition = BlockPositionCodec.encode(blockX, blockY, blockZ);
        Map<Long, long[]> columns = positionsByColumnByWorld.computeIfAbsent(worldName, name -> new LinkedHashMap<>());
        long columnKey = HorizontalCellCodec.encode(blockX, blockZ);
        long[] positions = columns.getOrDefault(columnKey, MarkedBlockPositions.EMPTY_POSITIONS);
        long[] extendedPositions = MarkedBlockPositions.add(positions, encodedPosition);
        if (extendedPositions == positions) {
            return false;
        }
        columns.put(columnKey, extendedPositions);
        return true;
    }

    public boolean clearMark(String worldName, int blockX, int blockY, int blockZ) {
        Map<Long, long[]> columns = positionsByColumnByWorld.get(worldName);
        if (columns == null) {
            return false;
        }
        long columnKey = HorizontalCellCodec.encode(blockX, blockZ);
        long[] positions = columns.get(columnKey);
        if (positions == null) {
            return false;
        }
        long[] reducedPositions = MarkedBlockPositions.remove(positions,
                BlockPositionCodec.encode(blockX, blockY, blockZ));
        if (reducedPositions == positions) {
            return false;
        }
        if (reducedPositions.length == 0) {
            columns.remove(columnKey);
        } else {
            columns.put(columnKey, reducedPositions);
        }
        dropEmptyWorld(worldName, columns);
        return true;
    }

    public boolean isMarkedBlock(String worldName, int blockX, int blockY, int blockZ) {
        Map<Long, long[]> columns = positionsByColumnByWorld.get(worldName);
        if (columns == null) {
            return false;
        }
        long[] positions = columns.get(HorizontalCellCodec.encode(blockX, blockZ));
        return positions != null
                && MarkedBlockPositions.contains(positions, BlockPositionCodec.encode(blockX, blockY, blockZ));
    }

    public boolean isBlockedColumn(String worldName, int blockX, int blockZ) {
        Map<Long, long[]> columns = positionsByColumnByWorld.get(worldName);
        return columns != null && columns.containsKey(HorizontalCellCodec.encode(blockX, blockZ));
    }

    public BlockedColumnLookup lookupIn(String worldName) {
        return (blockX, blockZ) -> isBlockedColumn(worldName, blockX, blockZ);
    }

    public BorderColumnReader createColumnReader(String worldName) {
        return (blockX, blockZ) -> readBorderHeights(worldName, blockX, blockZ);
    }

    public int[] readBorderHeights(String worldName, int blockX, int blockZ) {
        Map<Long, long[]> columns = positionsByColumnByWorld.get(worldName);
        if (columns == null) {
            return BorderColumnReader.NO_BORDER_BLOCKS;
        }
        long[] positions = columns.get(HorizontalCellCodec.encode(blockX, blockZ));
        if (positions == null) {
            return BorderColumnReader.NO_BORDER_BLOCKS;
        }
        int[] borderHeights = new int[positions.length];
        for (int positionIndex = 0; positionIndex < positions.length; positionIndex++) {
            borderHeights[positionIndex] = BlockPositionCodec.decodeY(positions[positionIndex]);
        }
        return borderHeights;
    }

    public int removeInsideShape(ArenaShape shape) {
        Map<Long, long[]> columns = positionsByColumnByWorld.get(shape.getWorldName());
        if (columns == null) {
            return 0;
        }
        int removedMarkCount = 0;
        Iterator<Map.Entry<Long, long[]>> columnEntries = columns.entrySet().iterator();
        while (columnEntries.hasNext()) {
            Map.Entry<Long, long[]> columnEntry = columnEntries.next();
            long[] positions = columnEntry.getValue();
            long[] keptPositions = MarkedBlockPositions.removeInsideShape(positions, shape);
            if (keptPositions == positions) {
                continue;
            }
            removedMarkCount += positions.length - keptPositions.length;
            if (keptPositions.length == 0) {
                columnEntries.remove();
            } else {
                columnEntry.setValue(keptPositions);
            }
        }
        dropEmptyWorld(shape.getWorldName(), columns);
        return removedMarkCount;
    }

    public Set<String> getWorldNames() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(positionsByColumnByWorld.keySet()));
    }

    public long[] getMarkedPositions(String worldName) {
        Map<Long, long[]> columns = positionsByColumnByWorld.get(worldName);
        if (columns == null) {
            return MarkedBlockPositions.EMPTY_POSITIONS;
        }
        long[] markedPositions = new long[countMarkedBlocks(columns)];
        int targetIndex = 0;
        for (long[] positions : columns.values()) {
            System.arraycopy(positions, 0, markedPositions, targetIndex, positions.length);
            targetIndex += positions.length;
        }
        Arrays.sort(markedPositions);
        return markedPositions;
    }

    public int getMarkedBlockCount() {
        int markedBlockCount = 0;
        for (Map<Long, long[]> columns : positionsByColumnByWorld.values()) {
            markedBlockCount += countMarkedBlocks(columns);
        }
        return markedBlockCount;
    }

    public void replaceWorld(String worldName, long[] markedPositions) {
        Objects.requireNonNull(worldName, "worldName");
        Map<Long, long[]> columns = new LinkedHashMap<>();
        for (long markedPosition : markedPositions) {
            long columnKey = HorizontalCellCodec.encode(BlockPositionCodec.decodeX(markedPosition),
                    BlockPositionCodec.decodeZ(markedPosition));
            columns.put(columnKey, MarkedBlockPositions.add(
                    columns.getOrDefault(columnKey, MarkedBlockPositions.EMPTY_POSITIONS), markedPosition));
        }
        if (columns.isEmpty()) {
            positionsByColumnByWorld.remove(worldName);
            return;
        }
        positionsByColumnByWorld.put(worldName, columns);
    }

    public void clear() {
        positionsByColumnByWorld.clear();
    }

    private void dropEmptyWorld(String worldName, Map<Long, long[]> columns) {
        if (columns.isEmpty()) {
            positionsByColumnByWorld.remove(worldName);
        }
    }

    private static int countMarkedBlocks(Map<Long, long[]> columns) {
        int markedBlockCount = 0;
        for (long[] positions : columns.values()) {
            markedBlockCount += positions.length;
        }
        return markedBlockCount;
    }
}
