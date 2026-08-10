package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionBlockCursorTest {

    @Test
    void countsEveryBlockOfTheRegion() {
        RegionBlockCursor cursor = new RegionBlockCursor(new RegionBoundary("world", 0, 0, 0, 1, 2, 3));

        assertEquals(2L * 3L * 4L, cursor.getTotalBlockCount());
        assertEquals(24L, cursor.getRemainingBlockCount());
        assertTrue(cursor.hasRemainingBlocks());
    }

    @Test
    void handsOutEveryPositionExactlyOnceAcrossTheBatches() {
        RegionBoundary region = new RegionBoundary("world", -2, 60, -2, 1, 62, 1);
        RegionBlockCursor cursor = new RegionBlockCursor(region);
        Set<Long> visitedPositions = new HashSet<>();

        while (cursor.hasRemainingBlocks()) {
            for (long encodedPosition : cursor.nextBatch(7)) {
                assertTrue(visitedPositions.add(encodedPosition),
                        "position " + BlockPositionCodec.describe(encodedPosition) + " was handed out twice");
                assertTrue(region.contains("world",
                        BlockPositionCodec.decodeX(encodedPosition),
                        BlockPositionCodec.decodeY(encodedPosition),
                        BlockPositionCodec.decodeZ(encodedPosition)));
            }
        }

        assertEquals(cursor.getTotalBlockCount(), visitedPositions.size());
        assertEquals(cursor.getTotalBlockCount(), cursor.getVisitedBlockCount());
    }

    @Test
    void neverHandsOutMorePositionsThanTheRegionHolds() {
        RegionBlockCursor cursor = new RegionBlockCursor(new RegionBoundary("world", 0, 0, 0, 0, 0, 2));

        assertEquals(3, cursor.nextBatch(100).length);
        assertFalse(cursor.hasRemainingBlocks());
        assertEquals(0, cursor.nextBatch(100).length);
    }

    @Test
    void rejectsABatchSizeBelowOne() {
        RegionBlockCursor cursor = new RegionBlockCursor(new RegionBoundary("world", 0, 0, 0, 1, 1, 1));

        assertThrows(IllegalArgumentException.class, () -> cursor.nextBatch(0));
    }
}
