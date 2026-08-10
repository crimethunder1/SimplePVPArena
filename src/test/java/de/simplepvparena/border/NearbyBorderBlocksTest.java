package de.simplepvparena.border;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearbyBorderBlocksTest {

    @Test
    void handsOutTheOfferedBlocksClosestFirst() {
        NearbyBorderBlocks nearbyBlocks = new NearbyBorderBlocks(5);

        assertTrue(nearbyBlocks.offer(30L, 9L));
        assertTrue(nearbyBlocks.offer(10L, 1L));
        assertTrue(nearbyBlocks.offer(20L, 4L));

        assertEquals(3, nearbyBlocks.getCount());
        assertArrayEquals(new long[] {10L, 20L, 30L}, nearbyBlocks.toArray());
    }

    @Test
    void keepsOnlyTheClosestBlocksOnceTheLimitIsReached() {
        NearbyBorderBlocks nearbyBlocks = new NearbyBorderBlocks(3);

        nearbyBlocks.offer(40L, 16L);
        nearbyBlocks.offer(30L, 9L);
        nearbyBlocks.offer(20L, 4L);

        assertTrue(nearbyBlocks.offer(10L, 1L), "closer than the farthest kept block");
        assertFalse(nearbyBlocks.offer(50L, 25L), "farther away than every kept block");

        assertEquals(3, nearbyBlocks.getCount());
        assertEquals(3, nearbyBlocks.getMaximumCount());
        assertArrayEquals(new long[] {10L, 20L, 30L}, nearbyBlocks.toArray());
    }

    @Test
    void dropsABlockAtTheSameDistanceAsTheFarthestKeptOne() {
        NearbyBorderBlocks nearbyBlocks = new NearbyBorderBlocks(2);

        nearbyBlocks.offer(10L, 1L);
        nearbyBlocks.offer(20L, 4L);

        assertFalse(nearbyBlocks.offer(30L, 4L));
        assertArrayEquals(new long[] {10L, 20L}, nearbyBlocks.toArray());
    }

    @Test
    void keepsTheOfferedOrderOfBlocksAtTheSameDistance() {
        NearbyBorderBlocks nearbyBlocks = new NearbyBorderBlocks(4);

        nearbyBlocks.offer(10L, 4L);
        nearbyBlocks.offer(20L, 4L);
        nearbyBlocks.offer(30L, 1L);
        nearbyBlocks.offer(40L, 4L);

        assertArrayEquals(new long[] {30L, 10L, 20L, 40L}, nearbyBlocks.toArray());
    }

    @Test
    void reportsNothingWithoutASingleOfferedBlock() {
        NearbyBorderBlocks nearbyBlocks = new NearbyBorderBlocks(1);

        assertEquals(0, nearbyBlocks.getCount());
        assertEquals(0, nearbyBlocks.toArray().length);
    }

    @Test
    void refusesALimitBelowOneBlock() {
        assertThrows(IllegalArgumentException.class, () -> new NearbyBorderBlocks(0));
    }
}
