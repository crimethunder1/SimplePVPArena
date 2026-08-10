package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaShapeTest {

    private static final String WORLD_NAME = "event";

    @Test
    void containsEveryCellOfTheShapeAtAnyHeight() {
        ArenaShape shape = shape(1, 1, 2, 1, 1, 2);

        assertTrue(shape.contains(WORLD_NAME, 1.5D, 1.5D));
        assertTrue(shape.contains(WORLD_NAME, 2.9D, 1.0D));
        assertFalse(shape.contains(WORLD_NAME, 2.5D, 2.5D), "a cell next to the shape stays outside");
        assertFalse(shape.contains("lobby", 1.5D, 1.5D), "another world is never inside the arena");
    }

    @Test
    void coversTheBlocksBetweenTheFloorAndTheTopOfTheWall() {
        ArenaShape shape = shape(1, 1);

        assertTrue(shape.containsBlock(1, 64, 1));
        assertTrue(shape.containsBlock(1, 70, 1));
        assertFalse(shape.containsBlock(1, 63, 1), "the floor below the arena is not cleaned");
        assertFalse(shape.containsBlock(1, 71, 1), "nothing above the wall is cleaned");
        assertFalse(shape.containsBlock(2, 64, 2));
    }

    @Test
    void dropsDuplicatedCells() {
        ArenaShape shape = shape(1, 1, 1, 1, 2, 1);

        assertEquals(2, shape.getCellCount());
        assertEquals(2L * (70 - 64 + 1), shape.getBlockCount());
    }

    @Test
    void reportsTheAreaTheShapeSpans() {
        ArenaShape shape = shape(-2, 5, 3, 5, 3, 9);

        RegionBoundary boundingBox = shape.getBoundingBox();

        assertEquals(-2, boundingBox.getMinimumX());
        assertEquals(3, boundingBox.getMaximumX());
        assertEquals(5, boundingBox.getMinimumZ());
        assertEquals(9, boundingBox.getMaximumZ());
        assertEquals(64, boundingBox.getMinimumY());
        assertEquals(70, boundingBox.getMaximumY());
    }

    @Test
    void reportsACenterThatBelongsToTheShape() {
        ArenaShape shape = shape(0, 0, 1, 0, 2, 0, 2, 1, 2, 2);

        PositionSnapshot center = shape.getCenterPosition();

        assertTrue(shape.contains(WORLD_NAME, center.x(), center.z()));
        assertEquals(64.0D, center.y());
        assertEquals(WORLD_NAME, center.worldName());
    }

    private ArenaShape shape(int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return new ArenaShape(WORLD_NAME, 64, 70, cells);
    }
}
