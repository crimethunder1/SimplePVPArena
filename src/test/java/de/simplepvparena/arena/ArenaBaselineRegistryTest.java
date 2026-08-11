package de.simplepvparena.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBaselineRegistryTest {

    @Test
    void holdsNoBaselineBeforeTheFirstCalculation() {
        ArenaBaselineRegistry registry = new ArenaBaselineRegistry();

        assertFalse(registry.hasSnapshot());
        assertTrue(registry.find(shape(64, 65, 0, 0)).isEmpty());
    }

    @Test
    void handsTheBaselineOutForTheAreaItWasTakenOf() {
        ArenaBaselineRegistry registry = new ArenaBaselineRegistry();
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        registry.store(capture(shape, Map.of(position(0, 64, 0), Material.STONE), 4));

        assertTrue(registry.hasSnapshot());
        assertEquals(Material.STONE,
                registry.find(shape).orElseThrow().findMaterialAt(0, 64, 0).orElseThrow());
        assertTrue(registry.find(shape(64, 65, 0, 0, 1, 0)).isPresent(),
                "a shape that covers the same blocks is the same fight area");
    }

    @Test
    void replacesTheBaselineOfAnEarlierCalculation() {
        ArenaBaselineRegistry registry = new ArenaBaselineRegistry();
        ArenaShape firstShape = shape(64, 65, 0, 0, 1, 0);
        ArenaShape secondShape = shape(64, 65, 0, 0, 1, 0, 2, 0);
        registry.store(capture(firstShape, Map.of(position(0, 64, 0), Material.STONE), 4));

        registry.store(capture(secondShape, Map.of(position(0, 64, 0), Material.SAND), 4));

        assertTrue(registry.find(firstShape).isEmpty(), "the area before the last calculation");
        assertEquals(Material.SAND,
                registry.find(secondShape).orElseThrow().findMaterialAt(0, 64, 0).orElseThrow());
    }

    @Test
    void dropsTheBaselineWithTheArenaConfiguration() {
        ArenaBaselineRegistry registry = new ArenaBaselineRegistry();
        ArenaShape shape = shape(64, 65, 0, 0);
        registry.store(capture(shape, Map.of(), 2));

        registry.discard();

        assertFalse(registry.hasSnapshot());
        assertTrue(registry.find(shape).isEmpty());
    }
}
