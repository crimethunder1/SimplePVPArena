package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionBoundaryTest {

    private static final String WORLD_NAME = "event";

    @Test
    void normalizesTheCornersRegardlessOfTheirOrder() {
        RegionBoundary boundary = new RegionBoundary(WORLD_NAME, 20, 80, -5, 0, 60, 15);

        assertEquals(0, boundary.getMinimumX());
        assertEquals(60, boundary.getMinimumY());
        assertEquals(-5, boundary.getMinimumZ());
        assertEquals(20, boundary.getMaximumX());
        assertEquals(80, boundary.getMaximumY());
        assertEquals(15, boundary.getMaximumZ());
    }

    @Test
    void containsPositionsInsideTheBoundary() {
        RegionBoundary boundary = new RegionBoundary(WORLD_NAME, 0, 60, 0, 10, 70, 10);

        assertTrue(boundary.contains(WORLD_NAME, 5.5, 65.0, 5.5));
        assertTrue(boundary.contains(WORLD_NAME, 0.0, 60.0, 0.0));
        assertTrue(boundary.contains(WORLD_NAME, 10.9, 70.9, 10.9));
    }

    @Test
    void doesNotContainPositionsOutsideTheBoundary() {
        RegionBoundary boundary = new RegionBoundary(WORLD_NAME, 0, 60, 0, 10, 70, 10);

        assertFalse(boundary.contains(WORLD_NAME, -0.5, 65.0, 5.0));
        assertFalse(boundary.contains(WORLD_NAME, 11.0, 65.0, 5.0));
        assertFalse(boundary.contains(WORLD_NAME, 5.0, 71.0, 5.0));
    }

    @Test
    void doesNotContainPositionsOfAnotherWorld() {
        RegionBoundary boundary = new RegionBoundary(WORLD_NAME, 0, 60, 0, 10, 70, 10);

        assertFalse(boundary.contains("lobby", 5.0, 65.0, 5.0));
    }

    @Test
    void reportsTheCenterOfTheBoundary() {
        RegionBoundary boundary = new RegionBoundary(WORLD_NAME, 0, 60, 0, 10, 70, 20);

        PositionSnapshot center = boundary.getCenterPosition();

        assertEquals(WORLD_NAME, center.worldName());
        assertEquals(5.5, center.x());
        assertEquals(60.0, center.y());
        assertEquals(10.5, center.z());
    }
}
