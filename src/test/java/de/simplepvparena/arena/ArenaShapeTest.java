package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void numbersEveryBlockOfTheVolumeExactlyOnce() {
        ArenaShape shape = shape(0, 0, 1, 0, 1, 1);

        assertEquals(3L * 7L, shape.getBlockCount());
        for (long blockIndex = 0; blockIndex < shape.getBlockCount(); blockIndex++) {
            long encodedPosition = shape.encodeBlockAtIndex(blockIndex);
            assertEquals(blockIndex, shape.findBlockIndex(BlockPositionCodec.decodeX(encodedPosition),
                            BlockPositionCodec.decodeY(encodedPosition),
                            BlockPositionCodec.decodeZ(encodedPosition)),
                    "the index of " + BlockPositionCodec.describe(encodedPosition));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> shape.encodeBlockAtIndex(3L * 7L));
        assertThrows(IndexOutOfBoundsException.class, () -> shape.encodeBlockAtIndex(-1L));
    }

    @Test
    void hasNoIndexForABlockOutsideOfTheVolume() {
        ArenaShape shape = shape(0, 0, 1, 0);

        assertEquals(-1L, shape.findBlockIndex(5, 64, 5), "a column that is not part of the shape");
        assertEquals(-1L, shape.findBlockIndex(0, 63, 0), "below the floor of the arena");
        assertEquals(-1L, shape.findBlockIndex(0, 71, 0), "above the top of the border wall");
    }

    @Test
    void keepsItsAreaWhenItsVerticalExtentIsReplaced() {
        ArenaShape shape = shape(0, 0, 1, 0);

        ArenaShape movedShape = shape.withVerticalExtent(20, 30);

        assertEquals(20, movedShape.getFloorY());
        assertEquals(30, movedShape.getWallTopY());
        assertEquals(64, movedShape.getReferenceY(), "the height the calculation ran at is not moved");
        assertEquals(WORLD_NAME, movedShape.getWorldName());
        assertEquals(shape.getCellCount(), movedShape.getCellCount());
        assertTrue(movedShape.containsCell(0, 0));
        assertTrue(movedShape.containsCell(1, 0));
        assertTrue(movedShape.containsBlock(0, 25, 0), "the new volume is what a cleaning writes");
        assertFalse(movedShape.containsBlock(0, 64, 0), "and the old one is not");
        assertEquals(64, shape.getFloorY(), "the shape it was made from is unchanged");
        assertEquals(70, shape.getWallTopY());
    }

    @Test
    void remembersThatItsVolumeWasMarkedAndNotReadOffTheWall() {
        ArenaShape shape = shape(0, 0, 1, 0);

        assertFalse(shape.hasMarkedHeights(), "a calculated shape follows its border wall");
        assertTrue(shape.withVerticalExtent(20, 30).hasMarkedHeights(),
                "and the one way to move that volume is an admin marking it");
        assertTrue(new ArenaShape(WORLD_NAME, 64, 20, 30,
                        new long[] {HorizontalCellCodec.encode(0, 0)}, true).hasMarkedHeights(),
                "a shape read back from config.yml keeps what was written about it");
    }

    @Test
    void comparesTheAreaWithoutTheVolumeItStandsUnder() {
        ArenaShape shape = shape(0, 0, 1, 0);
        ArenaShape movedShape = shape.withVerticalExtent(20, 30);

        assertTrue(shape.coversTheSameArea(movedShape), "the cells did not move with the volume");
        assertFalse(shape.hasSameVolume(movedShape), "the blocks it covers did");
        assertTrue(shape.coversTheSameArea(shape(1, 0, 0, 0)), "the order of the cells is not the area");
        assertFalse(shape.coversTheSameArea(shape(0, 0)));
        assertFalse(shape.coversTheSameArea(new ArenaShape("lobby", 64, 70,
                new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)})));
        assertFalse(shape.coversTheSameArea(null));
    }

    @Test
    void comparesTwoShapesByTheBlocksTheyCover() {
        ArenaShape shape = shape(0, 0, 1, 0);

        assertTrue(shape.hasSameVolume(shape(0, 0, 1, 0)));
        assertTrue(shape.hasSameVolume(shape(1, 0, 0, 0)), "the order of the cells is not the area");
        assertFalse(shape.hasSameVolume(shape(0, 0)));
        assertFalse(shape.hasSameVolume(shape(0, 0, 1, 0, 2, 0)));
        assertFalse(shape.hasSameVolume(new ArenaShape("lobby", 64, 70,
                new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)})));
        assertFalse(shape.hasSameVolume(new ArenaShape(WORLD_NAME, 64, 71,
                new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)})));
        assertFalse(shape.hasSameVolume(null));
        assertTrue(shape.hasSameVolume(shape.withVerticalExtent(64, 70)),
                "a baseline asks which blocks are covered, never who decided the two heights");
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
