package de.simplepvparena.intro;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class VersusCameraPath {

    public static final int TOTAL_TICKS = 120;
    public static final int FLIGHT_TICKS = 90;
    public static final double LOOK_HEIGHT = 1.4D;
    public static final double START_HEIGHT = 5.5D;
    public static final double END_HEIGHT = 1.7D;
    public static final double START_SIDE = 6.0D;
    public static final double EXTRA_DISTANCE_PER_HALF_WIDTH = 1.15D;
    public static final double START_EXTRA_DISTANCE = 9.0D;

    private VersusCameraPath() {
    }

    public static Point at(int tick, double stageHalfWidth) {
        double progress = smooth(Math.min(1.0D, Math.max(0.0D, tick / (double) FLIGHT_TICKS)));
        double endDistance = stageHalfWidth * EXTRA_DISTANCE_PER_HALF_WIDTH + 1.5D;
        double startDistance = endDistance + START_EXTRA_DISTANCE;
        double forward = lerp(startDistance, endDistance, progress);
        double right = START_SIDE * (1.0D - progress);
        double up = lerp(START_HEIGHT, END_HEIGHT, progress);
        double yawOffset = Math.toDegrees(Math.atan2(right, forward));
        double pitch = Math.toDegrees(Math.atan2(up - LOOK_HEIGHT, Math.hypot(right, forward)));
        return new Point(right, up, forward, (float) yawOffset, (float) pitch);
    }

    static double smooth(double value) {
        return value * value * (3.0D - 2.0D * value);
    }

    private static double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    public record Point(double right, double up, double forward, float yawOffset, float pitch) {
    }
}
