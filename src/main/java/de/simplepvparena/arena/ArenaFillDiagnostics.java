package de.simplepvparena.arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ArenaFillDiagnostics {

    public static final int FRONT_SAMPLE_SIZE = 8;

    public static final int HEIGHT_MISMATCH_SAMPLE_SIZE = 8;

    public static final int REPORTED_HEIGHT_MISMATCH_COUNT = 3;

    private static final String NO_DIRECTION_NAME = "nowhere";

    private final int seedX;
    private final int seedZ;
    private final int cellCount;
    private final int minimumX;
    private final int minimumZ;
    private final int maximumX;
    private final int maximumZ;
    private final long[] frontCells;
    private final List<ArenaHeightMismatchColumn> heightMismatchColumns;
    private final int heightMismatchColumnCount;

    private ArenaFillDiagnostics(int seedX, int seedZ, int cellCount, int minimumX, int minimumZ,
                                 int maximumX, int maximumZ, long[] frontCells,
                                 List<ArenaHeightMismatchColumn> heightMismatchColumns,
                                 int heightMismatchColumnCount) {
        this.seedX = seedX;
        this.seedZ = seedZ;
        this.cellCount = cellCount;
        this.minimumX = minimumX;
        this.minimumZ = minimumZ;
        this.maximumX = maximumX;
        this.maximumZ = maximumZ;
        this.frontCells = frontCells;
        this.heightMismatchColumns = heightMismatchColumns;
        this.heightMismatchColumnCount = heightMismatchColumnCount;
    }

    public static ArenaFillDiagnostics fromFillOrder(int seedX, int seedZ,
                                                     Collection<Long> filledCellsInFillOrder) {
        return fromFillOrder(seedX, seedZ, filledCellsInFillOrder, List.of(), 0);
    }

    public static ArenaFillDiagnostics fromFillOrder(int seedX, int seedZ,
                                                     Collection<Long> filledCellsInFillOrder,
                                                     List<ArenaHeightMismatchColumn> heightMismatchSample,
                                                     int heightMismatchColumnCount) {
        if (filledCellsInFillOrder.isEmpty()) {
            throw new IllegalArgumentException("a fill always holds at least its seed cell");
        }
        int minimumX = seedX;
        int minimumZ = seedZ;
        int maximumX = seedX;
        int maximumZ = seedZ;
        int cellCount = filledCellsInFillOrder.size();
        int firstFrontIndex = Math.max(0, cellCount - FRONT_SAMPLE_SIZE);
        long[] frontCells = new long[cellCount - firstFrontIndex];
        int cellIndex = 0;
        for (long cell : filledCellsInFillOrder) {
            int cellX = HorizontalCellCodec.decodeX(cell);
            int cellZ = HorizontalCellCodec.decodeZ(cell);
            minimumX = Math.min(minimumX, cellX);
            minimumZ = Math.min(minimumZ, cellZ);
            maximumX = Math.max(maximumX, cellX);
            maximumZ = Math.max(maximumZ, cellZ);
            if (cellIndex >= firstFrontIndex) {
                frontCells[cellIndex - firstFrontIndex] = cell;
            }
            cellIndex++;
        }
        return new ArenaFillDiagnostics(seedX, seedZ, cellCount, minimumX, minimumZ, maximumX, maximumZ,
                frontCells, List.copyOf(heightMismatchSample),
                Math.max(heightMismatchColumnCount, heightMismatchSample.size()));
    }

    public int getCellCount() {
        return cellCount;
    }

    public int getMinimumX() {
        return minimumX;
    }

    public int getMinimumZ() {
        return minimumZ;
    }

    public int getMaximumX() {
        return maximumX;
    }

    public int getMaximumZ() {
        return maximumZ;
    }

    public long[] getFrontCells() {
        return frontCells.clone();
    }

    public List<ArenaHeightMismatchColumn> getHeightMismatchColumns() {
        return heightMismatchColumns;
    }

    public int getHeightMismatchColumnCount() {
        return heightMismatchColumnCount;
    }

    public boolean hasHeightMismatch() {
        return !heightMismatchColumns.isEmpty();
    }

    public Optional<ArenaHeightMismatchColumn> getNearestHeightMismatchColumn() {
        return findNearestHeightMismatchColumns(1).stream().findFirst();
    }

    public List<ArenaHeightMismatchColumn> findNearestHeightMismatchColumns(int wantedCount) {
        List<ArenaHeightMismatchColumn> nearestColumns = new ArrayList<>(heightMismatchColumns);
        nearestColumns.sort(Comparator.comparingLong(
                column -> squaredSeedDistanceOf(column.blockX(), column.blockZ())));
        return List.copyOf(nearestColumns.subList(0, Math.min(Math.max(wantedCount, 0), nearestColumns.size())));
    }

    public String describeHeightMismatchColumns() {
        StringBuilder description = new StringBuilder();
        for (ArenaHeightMismatchColumn column : findNearestHeightMismatchColumns(REPORTED_HEIGHT_MISMATCH_COUNT)) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(column.describe());
        }
        return description.toString();
    }

    public boolean hasSpread() {
        return minimumX != maximumX || minimumZ != maximumZ;
    }

    public String getEscapeDirectionName() {
        int eastwardReach = maximumX - seedX;
        int westwardReach = seedX - minimumX;
        int southwardReach = maximumZ - seedZ;
        int northwardReach = seedZ - minimumZ;
        int horizontalReach = Math.max(eastwardReach, westwardReach);
        int verticalReach = Math.max(northwardReach, southwardReach);
        if (horizontalReach == 0 && verticalReach == 0) {
            return NO_DIRECTION_NAME;
        }
        String horizontalName = eastwardReach >= westwardReach ? "east" : "west";
        String verticalName = southwardReach >= northwardReach ? "south" : "north";
        int strongerReach = Math.max(horizontalReach, verticalReach);
        int weakerReach = Math.min(horizontalReach, verticalReach);
        if (weakerReach > 0 && weakerReach * 2 >= strongerReach) {
            return verticalName + "-" + horizontalName;
        }
        return horizontalReach >= verticalReach ? horizontalName : verticalName;
    }

    public int getFarthestX() {
        return maximumX - seedX >= seedX - minimumX ? maximumX : minimumX;
    }

    public int getFarthestZ() {
        return maximumZ - seedZ >= seedZ - minimumZ ? maximumZ : minimumZ;
    }

    public String describeEscapeDirection() {
        if (!hasSpread()) {
            return NO_DIRECTION_NAME + ", it never left the column you are standing in";
        }
        return getEscapeDirectionName() + ", " + Math.abs(getFarthestX() - seedX) + " columns "
                + (getFarthestX() >= seedX ? "east" : "west") + " and " + Math.abs(getFarthestZ() - seedZ)
                + " columns " + (getFarthestZ() >= seedZ ? "south" : "north") + " of you, up to x/z "
                + getFarthestX() + "/" + getFarthestZ();
    }

    public String describeBoundingBox() {
        return "x " + minimumX + ".." + maximumX + ", z " + minimumZ + ".." + maximumZ + ", "
                + (maximumX - minimumX + 1) + " by " + (maximumZ - minimumZ + 1) + " columns";
    }

    public long getNearestFrontCell() {
        long seedCell = HorizontalCellCodec.encode(seedX, seedZ);
        long nearestCell = seedCell;
        long nearestDistance = Long.MAX_VALUE;
        for (long frontCell : frontCells) {
            if (frontCell == seedCell) {
                continue;
            }
            long distance = squaredSeedDistanceOf(frontCell);
            if (distance < nearestDistance) {
                nearestCell = frontCell;
                nearestDistance = distance;
            }
        }
        return nearestCell;
    }

    public int getNearestFrontX() {
        return HorizontalCellCodec.decodeX(getNearestFrontCell());
    }

    public int getNearestFrontZ() {
        return HorizontalCellCodec.decodeZ(getNearestFrontCell());
    }

    private long squaredSeedDistanceOf(long cell) {
        return squaredSeedDistanceOf(HorizontalCellCodec.decodeX(cell), HorizontalCellCodec.decodeZ(cell));
    }

    private long squaredSeedDistanceOf(int cellX, int cellZ) {
        long distanceX = cellX - (long) seedX;
        long distanceZ = cellZ - (long) seedZ;
        return distanceX * distanceX + distanceZ * distanceZ;
    }

    public String describeFront() {
        StringBuilder description = new StringBuilder();
        for (long frontCell : frontCells) {
            if (description.length() > 0) {
                description.append(", ");
            }
            description.append(HorizontalCellCodec.describe(frontCell));
        }
        return description.toString();
    }
}
