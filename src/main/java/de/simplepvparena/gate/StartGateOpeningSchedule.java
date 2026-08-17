package de.simplepvparena.gate;

public final class StartGateOpeningSchedule {

    public static final long TICKS_PER_SECOND = 20L;

    public static final long MINIMUM_LAYER_TICKS = 1L;

    private final int layerCount;
    private final int durationSeconds;
    private final long totalDurationTicks;

    private StartGateOpeningSchedule(int layerCount, int durationSeconds) {
        this.layerCount = layerCount;
        this.durationSeconds = durationSeconds;
        this.totalDurationTicks = durationSeconds * TICKS_PER_SECOND;
    }

    public static StartGateOpeningSchedule of(int layerCount, int durationSeconds) {
        return new StartGateOpeningSchedule(Math.max(1, layerCount),
                Math.max(StartGateDuration.MINIMUM_SECONDS, durationSeconds));
    }

    public int getLayerCount() {
        return layerCount;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public long getTotalDurationTicks() {
        return totalDurationTicks;
    }

    public long ticksForLayer(int layerIndex) {
        requireLayer(layerIndex);
        return Math.max(MINIMUM_LAYER_TICKS, ticksUpTo(layerIndex + 1) - ticksUpTo(layerIndex));
    }

    public long getRemovalTickOfLayer(int layerIndex) {
        requireLayer(layerIndex);
        long removalTick = 0L;
        for (int walkedLayer = 0; walkedLayer <= layerIndex; walkedLayer++) {
            removalTick += ticksForLayer(walkedLayer);
        }
        return removalTick;
    }

    public long getTotalTickCount() {
        return getRemovalTickOfLayer(layerCount - 1);
    }

    private long ticksUpTo(int layerIndex) {
        return (long) layerIndex * totalDurationTicks / layerCount;
    }

    private void requireLayer(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= layerCount) {
            throw new IndexOutOfBoundsException("the layer index " + layerIndex + " is outside of the "
                    + layerCount + " layers of the start gate");
        }
    }
}
