package de.simplepvparena.laser;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.match.LaserZoneSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaserRingTest {

    private static final double DELTA = 1.0E-9D;

    private final LaserZoneSettings settings = LaserZoneSettings.defaults();
    private final LaserRing ring = new LaserRing(0.0D, 0.0D, 15.0D, settings);

    @Test
    void shrinksEvenlyFromTheStartRadiusToTheMinimumRadiusAndStopsThere() {
        long shrinkTicks = settings.shrinkSeconds() * 20L;

        assertEquals(15.0D, ring.radiusAfter(0L), DELTA);
        assertEquals(9.0D, ring.radiusAfter(shrinkTicks / 2L), DELTA);
        assertEquals(3.0D, ring.radiusAfter(shrinkTicks), DELTA);
        assertEquals(3.0D, ring.radiusAfter(shrinkTicks * 4L), DELTA);
    }

    @Test
    void neverStartsBelowTheMinimumRadius() {
        LaserRing tinyRing = new LaserRing(0.0D, 0.0D, 1.0D, settings);

        assertEquals(settings.minimumRadius(), tinyRing.startRadius(), DELTA);
        assertEquals(settings.minimumRadius(), tinyRing.radiusAfter(100L), DELTA);
    }

    @Test
    void leavesFightersClearOfTheLasersUnharmed() {
        assertTrue(ring.damageAt(0.0D, 0.0D, 10.0D).isEmpty());
        assertTrue(ring.damageAt(9.6D, 0.0D, 10.0D).isEmpty());
    }

    @Test
    void hurtsHalfAHeartOnTouchAndUpToTwoHeartsFarOutside() {
        assertEquals(OptionalDouble.of(1.0D), ring.damageAt(9.75D, 0.0D, 10.0D));
        assertEquals(OptionalDouble.of(1.0D), ring.damageAt(0.0D, -10.5D, 10.0D));
        assertEquals(OptionalDouble.of(2.0D), ring.damageAt(11.8D, 0.0D, 10.0D));
        assertEquals(OptionalDouble.of(3.0D), ring.damageAt(13.8D, 0.0D, 10.0D));
        assertEquals(OptionalDouble.of(4.0D), ring.damageAt(15.7D, 0.0D, 10.0D));
        assertEquals(OptionalDouble.of(4.0D), ring.damageAt(40.0D, 0.0D, 10.0D));
    }

    @Test
    void neverHurtsMoreFarAwayThanCloseBy() {
        double previous = 0.0D;
        for (double distance = 9.7D; distance < 30.0D; distance += 0.1D) {
            double damage = ring.damageAt(distance, 0.0D, 10.0D).orElseThrow();
            assertTrue(damage >= previous, "damage must not drop at distance " + distance);
            previous = damage;
        }
    }

    @Test
    void waitsTheConfiguredSecondsBetweenTwoHits() {
        assertEquals(60L, ring.damageIntervalTicks());
    }

    @Test
    void scalesThePoleCountWithTheRadiusWithinLimits() {
        assertEquals(LaserRing.MINIMUM_POLE_COUNT, LaserRing.poleCountFor(2.0D));
        assertEquals(14, LaserRing.poleCountFor(8.0D));
        assertEquals(LaserRing.MAXIMUM_POLE_COUNT, LaserRing.poleCountFor(40.0D));
    }

    @Test
    void findsTheLargestCircleThatStaysOnARoundArenaFloor() {
        ArenaShape disc = disc(10);

        double radius = LaserRing.largestRadiusInside(disc, 0.5D, 0.5D);

        assertTrue(radius > 9.0D && radius < 10.5D, "radius was " + radius);
    }

    @Test
    void keepsThePolesOffTheWallOfASquareArena() {
        ArenaShape square = square(-8, 8);

        double radius = LaserRing.largestRadiusInside(square, 0.5D, 0.5D);

        assertEquals(8.5D - LaserRing.MARGIN_TO_THE_ARENA_EDGE, radius, DELTA);
    }

    private static ArenaShape disc(int radius) {
        List<Long> cells = new ArrayList<>();
        for (int x = -radius - 1; x <= radius + 1; x++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
                if (Math.hypot(x + 0.5D - 0.5D, z + 0.5D - 0.5D) <= radius) {
                    cells.add(HorizontalCellCodec.encode(x, z));
                }
            }
        }
        return shapeOf(cells);
    }

    private static ArenaShape square(int minimum, int maximum) {
        List<Long> cells = new ArrayList<>();
        for (int x = minimum; x <= maximum; x++) {
            for (int z = minimum; z <= maximum; z++) {
                cells.add(HorizontalCellCodec.encode(x, z));
            }
        }
        return shapeOf(cells);
    }

    private static ArenaShape shapeOf(List<Long> cells) {
        long[] encoded = cells.stream().mapToLong(Long::longValue).toArray();
        return new ArenaShape("arena", 64, 70, encoded);
    }
}
