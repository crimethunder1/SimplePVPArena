package de.simplepvparena.arena;

import java.util.Arrays;

public final class ArenaOutline {

    private static final int[] NEIGHBOUR_X = {1, -1, 0, 0};
    private static final int[] NEIGHBOUR_Z = {0, 0, 1, -1};

    private final int[] rowX;
    private final int[] rowStart;
    private final int[] cellZ;
    private final int minimumX;
    private final int maximumX;
    private final int minimumZ;
    private final int maximumZ;

    public static ArenaOutline of(ArenaShape shape) {
        long[] cells = shape.getCells();
        long[] contourKeys = new long[cells.length];
        int contourCount = 0;
        for (long cell : cells) {
            int columnX = HorizontalCellCodec.decodeX(cell);
            int columnZ = HorizontalCellCodec.decodeZ(cell);
            if (isContourCell(shape, columnX, columnZ)) {
                contourKeys[contourCount++] = sortKey(columnX, columnZ);
            }
        }
        return new ArenaOutline(Arrays.copyOf(contourKeys, contourCount));
    }

    private ArenaOutline(long[] contourKeys) {
        if (contourKeys.length == 0) {
            throw new IllegalArgumentException("an arena shape always has a contour");
        }
        Arrays.sort(contourKeys);
        int rowCount = countRows(contourKeys);
        this.rowX = new int[rowCount];
        this.rowStart = new int[rowCount + 1];
        this.cellZ = new int[contourKeys.length];
        int rowIndex = -1;
        int lowestZ = Integer.MAX_VALUE;
        int highestZ = Integer.MIN_VALUE;
        for (int index = 0; index < contourKeys.length; index++) {
            int columnX = keyX(contourKeys[index]);
            if (rowIndex < 0 || rowX[rowIndex] != columnX) {
                rowIndex++;
                rowX[rowIndex] = columnX;
                rowStart[rowIndex] = index;
            }
            int columnZ = keyZ(contourKeys[index]);
            cellZ[index] = columnZ;
            lowestZ = Math.min(lowestZ, columnZ);
            highestZ = Math.max(highestZ, columnZ);
        }
        this.rowStart[rowCount] = contourKeys.length;
        this.minimumX = rowX[0];
        this.maximumX = rowX[rowCount - 1];
        this.minimumZ = lowestZ;
        this.maximumZ = highestZ;
    }

    public int getCellCount() {
        return cellZ.length;
    }

    public boolean containsCell(int blockX, int blockZ) {
        int rowIndex = Arrays.binarySearch(rowX, blockX);
        if (rowIndex < 0) {
            return false;
        }
        return Arrays.binarySearch(cellZ, rowStart[rowIndex], rowStart[rowIndex + 1], blockZ) >= 0;
    }

    public double distanceTo(int blockX, int blockZ) {
        return Math.sqrt(nearestSquaredDistance(blockX, blockZ));
    }

    public boolean isWithin(int blockX, int blockZ, int radius) {
        if (radius < 0) {
            return false;
        }
        long radiusSquared = (long) radius * radius;
        if (squaredDistanceToBoundingBox(blockX, blockZ) > radiusSquared) {
            return false;
        }
        return nearestSquaredDistance(blockX, blockZ) <= radiusSquared;
    }

    private long nearestSquaredDistance(int blockX, int blockZ) {
        int higherRow = firstRowAtOrAfter(blockX);
        int lowerRow = higherRow - 1;
        long best = Long.MAX_VALUE;
        while (higherRow < rowX.length || lowerRow >= 0) {
            long higherGap = higherRow < rowX.length ? squared(rowX[higherRow], blockX) : Long.MAX_VALUE;
            long lowerGap = lowerRow >= 0 ? squared(rowX[lowerRow], blockX) : Long.MAX_VALUE;
            if (Math.min(higherGap, lowerGap) >= best) {
                break;
            }
            int rowIndex = higherGap <= lowerGap ? higherRow++ : lowerRow--;
            best = Math.min(best, nearestSquaredDistanceInRow(rowIndex, blockX, blockZ));
        }
        return best;
    }

    private long nearestSquaredDistanceInRow(int rowIndex, int blockX, int blockZ) {
        long distanceX = squared(rowX[rowIndex], blockX);
        int from = rowStart[rowIndex];
        int to = rowStart[rowIndex + 1];
        int index = Arrays.binarySearch(cellZ, from, to, blockZ);
        if (index >= 0) {
            return distanceX;
        }
        int insertion = -index - 1;
        long best = Long.MAX_VALUE;
        if (insertion < to) {
            best = distanceX + squared(cellZ[insertion], blockZ);
        }
        if (insertion > from) {
            best = Math.min(best, distanceX + squared(cellZ[insertion - 1], blockZ));
        }
        return best;
    }

    private int firstRowAtOrAfter(int blockX) {
        int rowIndex = Arrays.binarySearch(rowX, blockX);
        return rowIndex >= 0 ? rowIndex : -rowIndex - 1;
    }

    private long squaredDistanceToBoundingBox(int blockX, int blockZ) {
        long gapX = Math.max(Math.max((long) minimumX - blockX, (long) blockX - maximumX), 0L);
        long gapZ = Math.max(Math.max((long) minimumZ - blockZ, (long) blockZ - maximumZ), 0L);
        return gapX * gapX + gapZ * gapZ;
    }

    private static long squared(int firstCoordinate, int secondCoordinate) {
        long difference = (long) firstCoordinate - secondCoordinate;
        return difference * difference;
    }

    private static boolean isContourCell(ArenaShape shape, int columnX, int columnZ) {
        for (int neighbour = 0; neighbour < NEIGHBOUR_X.length; neighbour++) {
            if (!shape.containsCell(columnX + NEIGHBOUR_X[neighbour], columnZ + NEIGHBOUR_Z[neighbour])) {
                return true;
            }
        }
        return false;
    }

    private static int countRows(long[] sortedKeys) {
        int rowCount = 0;
        for (int index = 0; index < sortedKeys.length; index++) {
            if (index == 0 || keyX(sortedKeys[index]) != keyX(sortedKeys[index - 1])) {
                rowCount++;
            }
        }
        return rowCount;
    }

    private static long sortKey(int columnX, int columnZ) {
        return ((long) columnX << 32) | ((columnZ ^ Integer.MIN_VALUE) & 0xFFFFFFFFL);
    }

    private static int keyX(long sortKey) {
        return (int) (sortKey >> 32);
    }

    private static int keyZ(long sortKey) {
        return ((int) sortKey) ^ Integer.MIN_VALUE;
    }
}
