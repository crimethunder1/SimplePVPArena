package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaGapTrailRegistryTest {

    private final ArenaGapTrailRegistry registry = new ArenaGapTrailRegistry();
    private final List<String> cancelledTrails = new ArrayList<>();

    @Test
    void aSecondTrailReplacesAndCancelsTheFirstOne() {
        UUID admin = UUID.randomUUID();

        registry.replace(admin, canceller("first"));
        registry.replace(admin, canceller("second"));

        assertEquals(List.of("first"), cancelledTrails);
        assertTrue(registry.isRunningFor(admin));
        assertEquals(1, registry.getRunningCount(), "an admin never has more than one trail");
    }

    @Test
    void stoppingCancelsTheTrailAndForgetsIt() {
        UUID admin = UUID.randomUUID();
        registry.replace(admin, canceller("trail"));

        registry.stop(admin);

        assertEquals(List.of("trail"), cancelledTrails);
        assertFalse(registry.isRunningFor(admin));
        assertEquals(0, registry.getRunningCount(), "a stopped trail leaves no task reference behind");
    }

    @Test
    void stoppingAnAdminWithoutATrailDoesNothing() {
        registry.stop(UUID.randomUUID());

        assertEquals(List.of(), cancelledTrails);
        assertEquals(0, registry.getRunningCount());
    }

    @Test
    void stoppingTwiceCancelsOnlyOnce() {
        UUID admin = UUID.randomUUID();
        registry.replace(admin, canceller("trail"));

        registry.stop(admin);
        registry.stop(admin);

        assertEquals(List.of("trail"), cancelledTrails);
    }

    @Test
    void trailsOfDifferentAdminsDoNotTouchEachOther() {
        UUID firstAdmin = UUID.randomUUID();
        UUID secondAdmin = UUID.randomUUID();
        registry.replace(firstAdmin, canceller("first"));
        registry.replace(secondAdmin, canceller("second"));

        registry.stop(firstAdmin);

        assertEquals(List.of("first"), cancelledTrails);
        assertFalse(registry.isRunningFor(firstAdmin));
        assertTrue(registry.isRunningFor(secondAdmin));
    }

    @Test
    void stoppingAllCancelsEveryTrailAndEmptiesTheRegistry() {
        registry.replace(UUID.randomUUID(), canceller("first"));
        registry.replace(UUID.randomUUID(), canceller("second"));

        registry.stopAll();

        assertEquals(2, cancelledTrails.size());
        assertTrue(cancelledTrails.containsAll(List.of("first", "second")));
        assertEquals(0, registry.getRunningCount());
    }

    private Runnable canceller(String trailName) {
        return () -> cancelledTrails.add(trailName);
    }
}
