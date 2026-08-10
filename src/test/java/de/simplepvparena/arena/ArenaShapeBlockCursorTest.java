package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaShapeBlockCursorTest {

    @Test
    void countsEveryBlockBetweenTheFloorAndTheTopOfTheWall() {
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape(64, 66, 0, 0, 1, 0, 1, 1));

        assertEquals(9L, cursor.getTotalBlockCount());
        assertEquals(9L, cursor.getRemainingBlockCount());
        assertTrue(cursor.hasRemainingBlocks());
    }

    @Test
    void handsOutEveryBlockOfTheShapeExactlyOnce() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0, 1, 1);
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape);
        Set<Long> visitedPositions = new HashSet<>();

        while (cursor.hasRemainingBlocks()) {
            for (long encodedPosition : cursor.nextBatch(2)) {
                assertTrue(visitedPositions.add(encodedPosition),
                        "the position " + BlockPositionCodec.describe(encodedPosition) + " was handed out twice");
                assertTrue(shape.containsBlock(BlockPositionCodec.decodeX(encodedPosition),
                                BlockPositionCodec.decodeY(encodedPosition),
                                BlockPositionCodec.decodeZ(encodedPosition)),
                        "the cursor only walks through the shape");
            }
        }

        assertEquals(6, visitedPositions.size());
        assertEquals(6L, cursor.getVisitedBlockCount());
        assertFalse(cursor.hasRemainingBlocks());
        assertEquals(0, cursor.nextBatch(10).length);
    }

    @Test
    void neverHandsOutMoreBlocksThanTheShapeHolds() {
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape(64, 64, 3, 3));

        assertEquals(1, cursor.nextBatch(100).length);
        assertThrows(IllegalArgumentException.class, () -> cursor.nextBatch(0));
    }

    private ArenaShape shape(int referenceY, int wallTopY, int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return new ArenaShape("event", referenceY, wallTopY, cells);
    }
}
