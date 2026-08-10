package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;

final class BorderLinePoints {

    static final double BLOCK_CENTER_OFFSET = 0.5D;

    static final double BLOCK_TOP_FACE_OFFSET = 1.0D;

    @FunctionalInterface
    interface PointConsumer {

        void accept(double pointX, double pointY, double pointZ);
    }

    private BorderLinePoints() {
    }

    static void walk(long fromPosition, long toPosition, double verticalBlockOffset, int pointCount,
                     PointConsumer consumer) {
        if (pointCount < 1) {
            throw new IllegalArgumentException("pointCount must be at least one");
        }
        double fromX = BlockPositionCodec.decodeX(fromPosition) + BLOCK_CENTER_OFFSET;
        double fromY = BlockPositionCodec.decodeY(fromPosition) + verticalBlockOffset;
        double fromZ = BlockPositionCodec.decodeZ(fromPosition) + BLOCK_CENTER_OFFSET;
        double stepX = BlockPositionCodec.decodeX(toPosition) + BLOCK_CENTER_OFFSET - fromX;
        double stepY = BlockPositionCodec.decodeY(toPosition) + verticalBlockOffset - fromY;
        double stepZ = BlockPositionCodec.decodeZ(toPosition) + BLOCK_CENTER_OFFSET - fromZ;
        for (int point = 1; point <= pointCount; point++) {
            double progress = (double) point / (pointCount + 1);
            consumer.accept(fromX + stepX * progress, fromY + stepY * progress, fromZ + stepZ * progress);
        }
    }
}
