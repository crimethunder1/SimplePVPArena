package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaGapTrailPointsTest {

    private static final double TOLERANCE = 1.0E-9D;

    private final List<double[]> points = new ArrayList<>();

    @Test
    void trailStartsAtTheAdminAndRunsTowardsTheTarget() {
        walkFrom(0.5D, 64.0D, 0.5D, 100, 0);

        assertEquals(ArenaGapTrailPoints.POINT_COUNT, points.size());
        for (int pointIndex = 0; pointIndex < points.size(); pointIndex++) {
            double[] point = points.get(pointIndex);
            assertEquals(0.5D + (pointIndex + 1) * ArenaGapTrailPoints.POINT_SPACING, point[0], TOLERANCE);
            assertEquals(0.5D, point[2], TOLERANCE, "a target straight east never moves the trail on z");
        }
    }

    @Test
    void trailNeverGrowsWithTheDistanceToTheTarget() {
        walkFrom(0.5D, 64.0D, 0.5D, 5000, 5000);

        assertEquals(ArenaGapTrailPoints.POINT_COUNT, points.size());
    }

    @Test
    void trailStopsShortOfATargetNearerThanItsFullLength() {
        walkFrom(0.5D, 64.0D, 0.5D, 4, 0);

        assertEquals(2, points.size());
        assertEquals(2.0D, points.get(0)[0], TOLERANCE);
        assertEquals(3.5D, points.get(1)[0], TOLERANCE);
    }

    @Test
    void trailIsEmptyForAnAdminStandingExactlyOnTheTarget() {
        walkFrom(10.5D, 64.0D, -3.5D, 10, -4);

        assertTrue(points.isEmpty());
    }

    @Test
    void trailIsDrawnJustAboveTheFeetOfTheAdmin() {
        walkFrom(0.5D, 71.0D, 0.5D, 60, 60);

        for (double[] point : points) {
            assertEquals(71.2D, point[1], TOLERANCE);
        }
    }

    @Test
    void trailRunsDiagonallyTowardsADiagonalTarget() {
        walkFrom(0.5D, 64.0D, 0.5D, 100, 100);

        double[] firstPoint = points.get(0);
        double diagonalStep = ArenaGapTrailPoints.POINT_SPACING / Math.sqrt(2.0D);
        assertEquals(0.5D + diagonalStep, firstPoint[0], TOLERANCE);
        assertEquals(0.5D + diagonalStep, firstPoint[2], TOLERANCE);
    }

    @Test
    void arrivalIsReachedInsideTheArrivalDistance() {
        assertTrue(ArenaGapTrailPoints.hasArrived(10.5D, 20.5D, 10, 20));
        assertTrue(ArenaGapTrailPoints.hasArrived(13.5D, 20.5D, 10, 20));
        assertFalse(ArenaGapTrailPoints.hasArrived(15.5D, 20.5D, 10, 20));
    }

    @Test
    void arrivalMeasuresBothAxesAndIgnoresTheHeight() {
        assertFalse(ArenaGapTrailPoints.hasArrived(13.5D, 23.5D, 10, 20),
                "three blocks on each axis are more than four blocks away");
        assertTrue(ArenaGapTrailPoints.hasArrived(8.5D, 18.5D, 10, 20));
    }

    private void walkFrom(double adminX, double adminY, double adminZ, int targetX, int targetZ) {
        ArenaGapTrailPoints.walkTowards(adminX, adminY, adminZ, targetX, targetZ,
                (pointX, pointY, pointZ) -> points.add(new double[] {pointX, pointY, pointZ}));
    }
}
