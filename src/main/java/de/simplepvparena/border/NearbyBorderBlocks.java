package de.simplepvparena.border;

import java.util.Arrays;

public final class NearbyBorderBlocks {

    private final long[] positions;
    private final long[] squaredDistances;

    private int count;

    public NearbyBorderBlocks(int maximumCount) {
        if (maximumCount < 1) {
            throw new IllegalArgumentException("maximumCount must be at least one");
        }
        this.positions = new long[maximumCount];
        this.squaredDistances = new long[maximumCount];
    }

    public boolean offer(long encodedPosition, long squaredDistance) {
        if (count == positions.length && squaredDistance >= squaredDistances[count - 1]) {
            return false;
        }
        int insertIndex = count;
        while (insertIndex > 0 && squaredDistances[insertIndex - 1] > squaredDistance) {
            insertIndex--;
        }
        for (int index = Math.min(count, positions.length - 1); index > insertIndex; index--) {
            positions[index] = positions[index - 1];
            squaredDistances[index] = squaredDistances[index - 1];
        }
        positions[insertIndex] = encodedPosition;
        squaredDistances[insertIndex] = squaredDistance;
        if (count < positions.length) {
            count++;
        }
        return true;
    }

    public int getCount() {
        return count;
    }

    public int getMaximumCount() {
        return positions.length;
    }

    public long[] toArray() {
        return Arrays.copyOf(positions, count);
    }
}
