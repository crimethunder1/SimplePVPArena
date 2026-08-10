package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderLinePointsTest {

    private static final double PRECISION = 1.0E-9D;
    private static final int FLOOR_Y = 64;

    @Test
    void spreadsThePointsBetweenTheTwoBlocksWithoutTouchingThem() {
        List<double[]> points = walk(block(10, FLOOR_Y, 20), block(11, FLOOR_Y, 20),
                BorderLinePoints.BLOCK_CENTER_OFFSET, 2);

        assertEquals(2, points.size());
        assertPoint(points.get(0), 10.5D + 1.0D / 3.0D, FLOOR_Y + 0.5D, 20.5D);
        assertPoint(points.get(1), 10.5D + 2.0D / 3.0D, FLOOR_Y + 0.5D, 20.5D);
    }

    @Test
    void runsAlongTheTopFacesWithTheTopFaceOffset() {
        List<double[]> points = walk(block(10, FLOOR_Y, 20), block(10, FLOOR_Y, 21),
                BorderLinePoints.BLOCK_TOP_FACE_OFFSET, 1);

        assertEquals(1, points.size());
        assertPoint(points.get(0), 10.5D, FLOOR_Y + 1.0D, 21.0D);
    }

    @Test
    void risesBetweenTwoBlocksOfDifferentHeights() {
        List<double[]> points = walk(block(10, FLOOR_Y, 20), block(11, FLOOR_Y + 4, 20),
                BorderLinePoints.BLOCK_TOP_FACE_OFFSET, 3);

        assertEquals(3, points.size());
        assertPoint(points.get(0), 10.75D, FLOOR_Y + 2.0D, 20.5D);
        assertPoint(points.get(1), 11.0D, FLOOR_Y + 3.0D, 20.5D);
        assertPoint(points.get(2), 11.25D, FLOOR_Y + 4.0D, 20.5D);
    }

    @Test
    void walksThePointsFromTheFirstPositionToTheSecondOne() {
        List<double[]> points = walk(block(0, FLOOR_Y, 0), block(1, FLOOR_Y, 1),
                BorderLinePoints.BLOCK_TOP_FACE_OFFSET, 5);

        assertEquals(5, points.size());
        for (int pointIndex = 1; pointIndex < points.size(); pointIndex++) {
            assertTrue(points.get(pointIndex)[0] > points.get(pointIndex - 1)[0],
                    "every point lies further along the line than the one before it");
        }
    }

    @Test
    void refusesALineWithoutASinglePoint() {
        assertThrows(IllegalArgumentException.class, () -> BorderLinePoints.walk(
                block(10, FLOOR_Y, 20), block(11, FLOOR_Y, 20), BorderLinePoints.BLOCK_CENTER_OFFSET, 0,
                (pointX, pointY, pointZ) -> {
                }));
    }

    private static List<double[]> walk(long fromPosition, long toPosition, double verticalBlockOffset,
                                       int pointCount) {
        List<double[]> points = new ArrayList<>();
        BorderLinePoints.walk(fromPosition, toPosition, verticalBlockOffset, pointCount,
                (pointX, pointY, pointZ) -> points.add(new double[] {pointX, pointY, pointZ}));
        return points;
    }

    private static void assertPoint(double[] point, double expectedX, double expectedY, double expectedZ) {
        assertEquals(expectedX, point[0], PRECISION);
        assertEquals(expectedY, point[1], PRECISION);
        assertEquals(expectedZ, point[2], PRECISION);
    }

    private static long block(int blockX, int blockY, int blockZ) {
        return BlockPositionCodec.encode(blockX, blockY, blockZ);
    }
}
