package de.simplepvparena.arena;

public final class ArenaCleaningLayerSchedule {

    public static final long MINIMUM_ANIMATED_LAYER_TICKS = 1L;

    private final int layerCount;
    private final long phaseDurationTicks;

    private ArenaCleaningLayerSchedule(int layerCount, long phaseDurationTicks) {
        this.layerCount = layerCount;
        this.phaseDurationTicks = phaseDurationTicks;
    }

    public static ArenaCleaningLayerSchedule of(int layerCount, long totalDurationTicks) {
        return new ArenaCleaningLayerSchedule(Math.max(1, layerCount),
                Math.max(0L, totalDurationTicks) / 2L);
    }

    public int getLayerCount() {
        return layerCount;
    }

    public long getPhaseDurationTicks() {
        return phaseDurationTicks;
    }

    public boolean isAnimated() {
        return phaseDurationTicks > 0L;
    }

    public long ticksForLayer(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= layerCount) {
            throw new IndexOutOfBoundsException("the layer index " + layerIndex + " is outside of the "
                    + layerCount + " layers of the shape");
        }
        if (!isAnimated()) {
            return 0L;
        }
        return Math.max(MINIMUM_ANIMATED_LAYER_TICKS, ticksUpTo(layerIndex + 1) - ticksUpTo(layerIndex));
    }

    public long getPhaseTickCount() {
        long tickCount = 0L;
        for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
            tickCount += ticksForLayer(layerIndex);
        }
        return tickCount;
    }

    public long getTotalTickCount() {
        return 2L * getPhaseTickCount();
    }

    private long ticksUpTo(int layerIndex) {
        return (long) layerIndex * phaseDurationTicks / layerCount;
    }
}
