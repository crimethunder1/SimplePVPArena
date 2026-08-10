package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.RegionBoundary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkedBlockPositionsTest {

    @Test
    void recognizesAMarkedBlockAgain() {
        long markedPosition = BlockPositionCodec.encode(10, 64, -3);
        long[] positions = MarkedBlockPositions.add(MarkedBlockPositions.EMPTY_POSITIONS, markedPosition);

        assertTrue(MarkedBlockPositions.contains(positions, markedPosition));
        assertFalse(MarkedBlockPositions.contains(positions, BlockPositionCodec.encode(10, 65, -3)));
        assertFalse(MarkedBlockPositions.contains(MarkedBlockPositions.EMPTY_POSITIONS, markedPosition));
    }

    @Test
    void marksABlockOnlyOnce() {
        long markedPosition = BlockPositionCodec.encode(1, 2, 3);
        long[] positions = MarkedBlockPositions.add(MarkedBlockPositions.EMPTY_POSITIONS, markedPosition);
        long[] positionsAfterSecondMark = MarkedBlockPositions.add(positions, markedPosition);

        assertEquals(1, positionsAfterSecondMark.length);
        assertArrayEquals(positions, positionsAfterSecondMark);
    }

    @Test
    void removesTheMarkOfASingleBlock() {
        long firstPosition = BlockPositionCodec.encode(1, 2, 3);
        long secondPosition = BlockPositionCodec.encode(4, 5, 6);
        long[] positions = MarkedBlockPositions.add(
                MarkedBlockPositions.add(MarkedBlockPositions.EMPTY_POSITIONS, firstPosition), secondPosition);

        long[] remainingPositions = MarkedBlockPositions.remove(positions, firstPosition);

        assertArrayEquals(new long[] {secondPosition}, remainingPositions);
        assertArrayEquals(remainingPositions, MarkedBlockPositions.remove(remainingPositions, firstPosition));
    }

    @Test
    void removesEveryMarkInsideACleanedRegion() {
        long insideRegion = BlockPositionCodec.encode(5, 64, 5);
        long onTheRegionBorder = BlockPositionCodec.encode(0, 60, 10);
        long outsideRegion = BlockPositionCodec.encode(5, 100, 5);
        long[] positions = new long[] {insideRegion, onTheRegionBorder, outsideRegion};

        long[] remainingPositions = MarkedBlockPositions.removeInsideRegion(positions,
                new RegionBoundary("world", 0, 60, 0, 10, 70, 10));

        assertArrayEquals(new long[] {outsideRegion}, remainingPositions);
    }

    @Test
    void keepsThePositionsWhenNothingIsInsideTheRegion() {
        long[] positions = new long[] {BlockPositionCodec.encode(100, 64, 100)};

        assertArrayEquals(positions, MarkedBlockPositions.removeInsideRegion(positions,
                new RegionBoundary("world", 0, 0, 0, 10, 10, 10)));
    }
}
