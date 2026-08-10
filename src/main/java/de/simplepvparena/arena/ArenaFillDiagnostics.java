package de.simplepvparena.arena;

import java.util.Collection;

public final class ArenaFillDiagnostics {

    public static final int FRONT_SAMPLE_SIZE = 8;

    private static final String NO_DIRECTION_NAME = "nowhere";

    private final int seedX;
    private final int seedZ;
    private final int cellCount;
    private final int minimumX;
    private final int minimumZ;
    private final int maximumX;
    private final int maximumZ;
    private final long[] frontCells;

    private ArenaFillDiagnostics(int seedX, int seedZ, int cellCount, int minimumX, int minimumZ,
                                 int maximumX, int maximumZ, long[] frontCells) {
        this.seedX = seedX;
        this.seedZ = seedZ;
        this.cellCount = cellCount;
        this.minimumX = minimumX;
        this.minimumZ = minimumZ;
        this.maximumX = maximumX;
        this.maximumZ = maximumZ;
        this.frontCells = frontCells;
    }

    public static ArenaFillDiagnostics fromFillOrder(int seedX, int seedZ,
                                                     Collection<Long> filledCellsInFillOrder) {
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
                frontCells);
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
        long distanceX = HorizontalCellCodec.decodeX(cell) - (long) seedX;
        long distanceZ = HorizontalCellCodec.decodeZ(cell) - (long) seedZ;
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
