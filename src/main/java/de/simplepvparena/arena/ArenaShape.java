package de.simplepvparena.arena;

import org.bukkit.Location;

import java.util.Arrays;
import java.util.Objects;

public final class ArenaShape {

    private final String worldName;
    private final int referenceY;
    private final int floorY;
    private final int wallTopY;
    private final long[] cells;

    public ArenaShape(String worldName, int referenceY, int wallTopY, long[] cells) {
        this(worldName, referenceY, referenceY, wallTopY, cells);
    }

    public ArenaShape(String worldName, int referenceY, int floorY, int wallTopY, long[] cells) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(cells, "cells");
        if (cells.length == 0) {
            throw new IllegalArgumentException("an arena shape needs at least one cell");
        }
        this.referenceY = referenceY;
        this.floorY = floorY;
        this.wallTopY = Math.max(floorY, wallTopY);
        this.cells = sortedDistinct(cells);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getReferenceY() {
        return referenceY;
    }

    public int getFloorY() {
        return floorY;
    }

    public int getWallTopY() {
        return wallTopY;
    }

    public int getHeight() {
        return wallTopY - floorY + 1;
    }

    public int getCellCount() {
        return cells.length;
    }

    public long[] getCells() {
        return cells.clone();
    }

    public long getBlockCount() {
        return (long) cells.length * getHeight();
    }

    public boolean containsCell(int blockX, int blockZ) {
        return Arrays.binarySearch(cells, HorizontalCellCodec.encode(blockX, blockZ)) >= 0;
    }

    public long encodeBlockAtIndex(long blockIndex) {
        if (blockIndex < 0L || blockIndex >= getBlockCount()) {
            throw new IndexOutOfBoundsException("the block index " + blockIndex + " is outside of the "
                    + getBlockCount() + " blocks of the shape");
        }
        long cell = cells[(int) (blockIndex % cells.length)];
        int blockY = floorY + (int) (blockIndex / cells.length);
        return BlockPositionCodec.encode(HorizontalCellCodec.decodeX(cell), blockY,
                HorizontalCellCodec.decodeZ(cell));
    }

    public long findBlockIndex(int blockX, int blockY, int blockZ) {
        if (blockY < floorY || blockY > wallTopY) {
            return -1L;
        }
        int cellIndex = Arrays.binarySearch(cells, HorizontalCellCodec.encode(blockX, blockZ));
        if (cellIndex < 0) {
            return -1L;
        }
        return (long) (blockY - floorY) * cells.length + cellIndex;
    }

    public boolean hasSameVolume(ArenaShape otherShape) {
        return otherShape != null
                && worldName.equals(otherShape.worldName)
                && floorY == otherShape.floorY
                && wallTopY == otherShape.wallTopY
                && Arrays.equals(cells, otherShape.cells);
    }

    public boolean contains(String otherWorldName, double x, double z) {
        if (!worldName.equals(otherWorldName)) {
            return false;
        }
        return containsCell((int) Math.floor(x), (int) Math.floor(z));
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return contains(location.getWorld().getName(), location.getX(), location.getZ());
    }

    public boolean containsBlock(int blockX, int blockY, int blockZ) {
        return blockY >= floorY && blockY <= wallTopY && containsCell(blockX, blockZ);
    }

    public RegionBoundary getBoundingBox() {
        int minimumX = Integer.MAX_VALUE;
        int minimumZ = Integer.MAX_VALUE;
        int maximumX = Integer.MIN_VALUE;
        int maximumZ = Integer.MIN_VALUE;
        for (long cell : cells) {
            int cellX = HorizontalCellCodec.decodeX(cell);
            int cellZ = HorizontalCellCodec.decodeZ(cell);
            minimumX = Math.min(minimumX, cellX);
            minimumZ = Math.min(minimumZ, cellZ);
            maximumX = Math.max(maximumX, cellX);
            maximumZ = Math.max(maximumZ, cellZ);
        }
        return new RegionBoundary(worldName, minimumX, floorY, minimumZ, maximumX, wallTopY, maximumZ);
    }

    public PositionSnapshot getCenterPosition() {
        double averageX = 0.0D;
        double averageZ = 0.0D;
        for (long cell : cells) {
            averageX += HorizontalCellCodec.decodeX(cell);
            averageZ += HorizontalCellCodec.decodeZ(cell);
        }
        averageX /= cells.length;
        averageZ /= cells.length;
        long closestCell = cells[0];
        double closestDistance = Double.MAX_VALUE;
        for (long cell : cells) {
            double distanceX = HorizontalCellCodec.decodeX(cell) - averageX;
            double distanceZ = HorizontalCellCodec.decodeZ(cell) - averageZ;
            double distance = distanceX * distanceX + distanceZ * distanceZ;
            if (distance < closestDistance) {
                closestDistance = distance;
                closestCell = cell;
            }
        }
        return new PositionSnapshot(worldName,
                HorizontalCellCodec.decodeX(closestCell) + 0.5D, referenceY,
                HorizontalCellCodec.decodeZ(closestCell) + 0.5D, 0.0F, 0.0F);
    }

    public String describe() {
        RegionBoundary boundingBox = getBoundingBox();
        return worldName + " with " + cells.length + " cells between "
                + boundingBox.getMinimumX() + "/" + boundingBox.getMinimumZ() + " and "
                + boundingBox.getMaximumX() + "/" + boundingBox.getMaximumZ()
                + ", cleaned from y " + floorY + " to y " + wallTopY
                + ", calculated at y " + referenceY;
    }

    private static long[] sortedDistinct(long[] cells) {
        long[] sortedCells = cells.clone();
        Arrays.sort(sortedCells);
        int distinctCount = 0;
        for (int cellIndex = 0; cellIndex < sortedCells.length; cellIndex++) {
            if (cellIndex == 0 || sortedCells[cellIndex] != sortedCells[cellIndex - 1]) {
                sortedCells[distinctCount++] = sortedCells[cellIndex];
            }
        }
        return distinctCount == sortedCells.length ? sortedCells : Arrays.copyOf(sortedCells, distinctCount);
    }
}
