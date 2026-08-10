package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.HorizontalCellCodec;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderMarkIndexTest {

    private static final String WORLD = "event";
    private static final String OTHER_WORLD = "lobby";

    private final BorderMarkIndex borderMarks = new BorderMarkIndex();

    @Test
    void recognizesAMarkedBlockAgain() {
        assertTrue(borderMarks.mark(WORLD, 10, 64, 20));

        assertTrue(borderMarks.isMarkedBlock(WORLD, 10, 64, 20));
        assertFalse(borderMarks.isMarkedBlock(WORLD, 10, 65, 20));
        assertFalse(borderMarks.isMarkedBlock(OTHER_WORLD, 10, 64, 20), "the worlds are kept apart");
    }

    @Test
    void marksABlockOnlyOnce() {
        assertTrue(borderMarks.mark(WORLD, 10, 64, 20));

        assertFalse(borderMarks.mark(WORLD, 10, 64, 20));
        assertEquals(1, borderMarks.getMarkedBlockCount());
    }

    @Test
    void barsTheWholeColumnOfASingleMark() {
        borderMarks.mark(WORLD, 10, 200, 20);

        assertTrue(borderMarks.isBlockedColumn(WORLD, 10, 20));
        assertFalse(borderMarks.isBlockedColumn(WORLD, 11, 20));
        assertFalse(borderMarks.isBlockedColumn(OTHER_WORLD, 10, 20));
    }

    @Test
    void keepsAColumnBarredUntilItsLastMarkIsCleared() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.mark(WORLD, 10, 65, 20);

        assertTrue(borderMarks.clearMark(WORLD, 10, 65, 20));
        assertTrue(borderMarks.isBlockedColumn(WORLD, 10, 20), "the second border block still bars the column");

        assertTrue(borderMarks.clearMark(WORLD, 10, 64, 20));
        assertFalse(borderMarks.isBlockedColumn(WORLD, 10, 20));
    }

    @Test
    void reportsNothingToDoWhenAnUnmarkedBlockIsCleared() {
        assertFalse(borderMarks.clearMark(WORLD, 10, 64, 20));

        borderMarks.mark(WORLD, 10, 64, 20);

        assertFalse(borderMarks.clearMark(WORLD, 10, 65, 20));
        assertFalse(borderMarks.clearMark(OTHER_WORLD, 10, 64, 20));
        assertTrue(borderMarks.isMarkedBlock(WORLD, 10, 64, 20));
    }

    @Test
    void handsTheHeightsOfAColumnToTheArenaCalculation() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.mark(WORLD, 10, 66, 20);
        borderMarks.mark(WORLD, 11, 64, 20);

        int[] borderHeights = borderMarks.createColumnReader(WORLD).readBorderHeights(10, 20);
        Arrays.sort(borderHeights);

        assertArrayEquals(new int[] {64, 66}, borderHeights);
        assertEquals(0, borderMarks.readBorderHeights(WORLD, 12, 20).length);
        assertEquals(0, borderMarks.readBorderHeights(OTHER_WORLD, 10, 20).length);
    }

    @Test
    void dropsEveryMarkInsideACleanedArena() {
        borderMarks.mark(WORLD, 5, 64, 5);
        borderMarks.mark(WORLD, 6, 60, 5);
        borderMarks.mark(WORLD, 5, 100, 5);
        borderMarks.mark(WORLD, 7, 64, 5);
        borderMarks.mark(OTHER_WORLD, 5, 64, 5);

        assertEquals(1, borderMarks.removeInsideShape(shape()));

        assertFalse(borderMarks.isMarkedBlock(WORLD, 5, 64, 5), "the block the cleaning set to air");
        assertTrue(borderMarks.isMarkedBlock(WORLD, 6, 60, 5), "below the cleaned volume");
        assertTrue(borderMarks.isMarkedBlock(WORLD, 5, 100, 5), "above the cleaned volume");
        assertTrue(borderMarks.isBlockedColumn(WORLD, 5, 5),
                "the mark above the cleaned volume still bars the column");
        assertTrue(borderMarks.isMarkedBlock(WORLD, 7, 64, 5), "the wall around the arena");
        assertTrue(borderMarks.isMarkedBlock(OTHER_WORLD, 5, 64, 5), "another world is never touched");
    }

    @Test
    void reportsNoClearedMarksForAShapeOfAnUnknownWorld() {
        borderMarks.mark(OTHER_WORLD, 5, 64, 5);

        assertEquals(0, borderMarks.removeInsideShape(shape()));
        assertTrue(borderMarks.isMarkedBlock(OTHER_WORLD, 5, 64, 5));
    }

    @Test
    void handsOutEveryMarkedPositionOfAWorldAndTakesItBack() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.mark(WORLD, 10, 65, 20);
        borderMarks.mark(WORLD, -3, 64, -7);
        borderMarks.mark(OTHER_WORLD, 1, 2, 3);

        long[] markedPositions = borderMarks.getMarkedPositions(WORLD);

        assertEquals(3, markedPositions.length);
        assertArrayEquals(markedPositions.clone(), sorted(markedPositions), "sorted, so the file stays stable");
        assertEquals(Set.of(WORLD, OTHER_WORLD), borderMarks.getWorldNames());

        BorderMarkIndex restoredMarks = new BorderMarkIndex();
        restoredMarks.replaceWorld(WORLD, markedPositions);

        assertTrue(restoredMarks.isMarkedBlock(WORLD, 10, 64, 20));
        assertTrue(restoredMarks.isMarkedBlock(WORLD, 10, 65, 20));
        assertTrue(restoredMarks.isMarkedBlock(WORLD, -3, 64, -7));
        assertTrue(restoredMarks.isBlockedColumn(WORLD, -3, -7));
        assertEquals(3, restoredMarks.getMarkedBlockCount());
        assertEquals(0, restoredMarks.getMarkedPositions(OTHER_WORLD).length);
    }

    @Test
    void forgetsAWorldWhoseLastMarkIsGone() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.clearMark(WORLD, 10, 64, 20);

        assertEquals(Set.of(), borderMarks.getWorldNames());
        assertEquals(0, borderMarks.getMarkedBlockCount());

        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.replaceWorld(WORLD, new long[0]);

        assertEquals(Set.of(), borderMarks.getWorldNames());
    }

    @Test
    void clearsEveryMarkOfEveryWorld() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.mark(OTHER_WORLD, 1, 2, 3);

        borderMarks.clear();

        assertEquals(0, borderMarks.getMarkedBlockCount());
        assertEquals(Set.of(), borderMarks.getWorldNames());
    }

    @Test
    void answersTheColumnLookupOfTheMovementRule() {
        borderMarks.mark(WORLD, 10, 64, 20);
        BlockedColumnLookup lookup = borderMarks.lookupIn(WORLD);

        assertTrue(lookup.isBlockedColumn(10, 20));
        assertFalse(lookup.isBlockedColumn(11, 20));
        assertFalse(borderMarks.lookupIn(OTHER_WORLD).isBlockedColumn(10, 20));
    }

    @Test
    void collectsTheBorderBlocksAroundAPosition() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.mark(WORLD, 12, 64, 20);
        borderMarks.mark(WORLD, 11, 64, 20);
        borderMarks.mark(WORLD, 40, 64, 20);
        borderMarks.mark(OTHER_WORLD, 10, 64, 20);

        long[] nearbyMarks = borderMarks.collectMarksNear(WORLD, 9, 64, 20, 4, 10);

        assertArrayEquals(new long[] {
                BlockPositionCodec.encode(10, 64, 20),
                BlockPositionCodec.encode(11, 64, 20),
                BlockPositionCodec.encode(12, 64, 20)}, nearbyMarks);
        assertEquals(0, borderMarks.collectMarksNear("unknown", 9, 64, 20, 4, 10).length);
    }

    @Test
    void leavesOutTheBorderBlocksOutsideOfTheRadius() {
        borderMarks.mark(WORLD, 10, 64, 20);
        borderMarks.mark(WORLD, 10, 70, 20);
        borderMarks.mark(WORLD, 14, 64, 20);
        borderMarks.mark(WORLD, 10, 64, 24);

        long[] nearbyMarks = borderMarks.collectMarksNear(WORLD, 10, 64, 20, 3, 10);

        assertArrayEquals(new long[] {BlockPositionCodec.encode(10, 64, 20)}, nearbyMarks);
    }

    @Test
    void reportsAtMostTheRequestedNumberOfBorderBlocks() {
        for (int blockY = 60; blockY <= 68; blockY++) {
            borderMarks.mark(WORLD, 10, blockY, 20);
        }

        long[] nearbyMarks = borderMarks.collectMarksNear(WORLD, 10, 64, 20, 8, 3);

        assertArrayEquals(new long[] {
                BlockPositionCodec.encode(10, 64, 20),
                BlockPositionCodec.encode(10, 63, 20),
                BlockPositionCodec.encode(10, 65, 20)}, nearbyMarks);
    }

    @Test
    void answersTheSameForASmallAndForALargeWall() {
        BorderMarkIndex smallWall = new BorderMarkIndex();
        BorderMarkIndex largeWall = new BorderMarkIndex();
        for (BorderMarkIndex wall : List.of(smallWall, largeWall)) {
            wall.mark(WORLD, 10, 64, 20);
            wall.mark(WORLD, 11, 64, 21);
        }
        for (int blockX = 100; blockX < 140; blockX++) {
            largeWall.mark(WORLD, blockX, 64, 200);
        }

        long[] expectedMarks = {BlockPositionCodec.encode(10, 64, 20), BlockPositionCodec.encode(11, 64, 21)};

        assertArrayEquals(expectedMarks, smallWall.collectMarksNear(WORLD, 10, 64, 20, 2, 10));
        assertArrayEquals(expectedMarks, largeWall.collectMarksNear(WORLD, 10, 64, 20, 2, 10));
    }

    @Test
    void refusesANegativeRadius() {
        borderMarks.mark(WORLD, 10, 64, 20);

        assertThrows(IllegalArgumentException.class,
                () -> borderMarks.collectMarksNear(WORLD, 10, 64, 20, -1, 10));
    }

    private static long[] sorted(long[] positions) {
        long[] sortedPositions = positions.clone();
        Arrays.sort(sortedPositions);
        return sortedPositions;
    }

    private static ArenaShape shape() {
        return new ArenaShape(WORLD, 61, 70, new long[] {
                HorizontalCellCodec.encode(5, 5),
                HorizontalCellCodec.encode(6, 5)});
    }

    @Test
    void keepsTheEncodedPositionsCompatibleWithTheBlockPositionCodec() {
        borderMarks.mark(WORLD, -3, 64, -7);

        long[] markedPositions = borderMarks.getMarkedPositions(WORLD);

        assertArrayEquals(new long[] {BlockPositionCodec.encode(-3, 64, -7)}, markedPositions);
    }
}
