package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.RegionBoundary;

import java.util.Arrays;

public final class MarkedBlockPositions {

    public static final long[] EMPTY_POSITIONS = new long[0];

    private MarkedBlockPositions() {
    }

    public static boolean contains(long[] positions, long encodedPosition) {
        for (long position : positions) {
            if (position == encodedPosition) {
                return true;
            }
        }
        return false;
    }

    public static long[] add(long[] positions, long encodedPosition) {
        if (contains(positions, encodedPosition)) {
            return positions;
        }
        long[] extendedPositions = Arrays.copyOf(positions, positions.length + 1);
        extendedPositions[positions.length] = encodedPosition;
        return extendedPositions;
    }

    public static long[] remove(long[] positions, long encodedPosition) {
        if (!contains(positions, encodedPosition)) {
            return positions;
        }
        long[] reducedPositions = new long[positions.length - 1];
        int targetIndex = 0;
        for (long position : positions) {
            if (position != encodedPosition) {
                reducedPositions[targetIndex++] = position;
            }
        }
        return reducedPositions;
    }

    public static long[] removeInsideRegion(long[] positions, RegionBoundary region) {
        long[] keptPositions = new long[positions.length];
        int targetIndex = 0;
        for (long position : positions) {
            if (!isInsideRegion(position, region)) {
                keptPositions[targetIndex++] = position;
            }
        }
        return targetIndex == positions.length ? positions : Arrays.copyOf(keptPositions, targetIndex);
    }

    private static boolean isInsideRegion(long encodedPosition, RegionBoundary region) {
        int blockX = BlockPositionCodec.decodeX(encodedPosition);
        int blockY = BlockPositionCodec.decodeY(encodedPosition);
        int blockZ = BlockPositionCodec.decodeZ(encodedPosition);
        return blockX >= region.getMinimumX() && blockX <= region.getMaximumX()
                && blockY >= region.getMinimumY() && blockY <= region.getMaximumY()
                && blockZ >= region.getMinimumZ() && blockZ <= region.getMaximumZ();
    }
}
