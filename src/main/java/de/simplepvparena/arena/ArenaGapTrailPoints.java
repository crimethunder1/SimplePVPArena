package de.simplepvparena.arena;

final class ArenaGapTrailPoints {

    static final int POINT_COUNT = 12;

    static final double POINT_SPACING = 1.5D;

    static final double ARRIVAL_DISTANCE = 4.0D;

    private static final double TRAIL_HEIGHT_OFFSET = 0.2D;

    private static final double BLOCK_CENTER_OFFSET = 0.5D;

    @FunctionalInterface
    interface PointConsumer {

        void accept(double pointX, double pointY, double pointZ);
    }

    private ArenaGapTrailPoints() {
    }

    static boolean hasArrived(double adminX, double adminZ, int targetX, int targetZ) {
        double distanceX = targetX + BLOCK_CENTER_OFFSET - adminX;
        double distanceZ = targetZ + BLOCK_CENTER_OFFSET - adminZ;
        return distanceX * distanceX + distanceZ * distanceZ <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE;
    }

    static void walkTowards(double adminX, double adminY, double adminZ, int targetX, int targetZ,
                            PointConsumer consumer) {
        double stepX = targetX + BLOCK_CENTER_OFFSET - adminX;
        double stepZ = targetZ + BLOCK_CENTER_OFFSET - adminZ;
        double distance = Math.sqrt(stepX * stepX + stepZ * stepZ);
        if (distance <= 0.0D) {
            return;
        }
        double directionX = stepX / distance;
        double directionZ = stepZ / distance;
        double pointY = adminY + TRAIL_HEIGHT_OFFSET;
        for (int point = 1; point <= POINT_COUNT; point++) {
            double reach = point * POINT_SPACING;
            if (reach > distance) {
                return;
            }
            consumer.accept(adminX + directionX * reach, pointY, adminZ + directionZ * reach);
        }
    }
}
