package de.simplepvparena.arena;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRegistryTest {

    @Test
    void writesAndReadsTheSingleArena() {
        ArenaRegistry registry = new ArenaRegistry();
        for (ArenaRegionType regionType : ArenaRegionType.values()) {
            registry.getArena().setCorner(regionType, RegionCorner.CORNER_ONE, position(0.0D, 64.0D, 0.0D));
            registry.getArena().setCorner(regionType, RegionCorner.CORNER_TWO, position(10.0D, 80.0D, 10.0D));
        }
        registry.getArena().setTribuneSpawn(position(5.0D, 70.0D, 5.0D));

        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);

        ArenaRegistry reloadedRegistry = new ArenaRegistry();
        reloadedRegistry.loadFromConfiguration(configuration);

        assertTrue(reloadedRegistry.isArenaFullyConfigured());
        assertEquals("world from 0/64/0 to 10/80/10",
                reloadedRegistry.getArena().getRegion(ArenaRegionType.FIGHT).orElseThrow().describe());
        assertEquals(70.0D, reloadedRegistry.getArena().getConfiguredTribuneSpawn().orElseThrow().y());
    }

    @Test
    void keepsOtherEntriesOfTheArenaSection() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("arena.clean-blocks-per-tick", 512);

        ArenaRegistry registry = new ArenaRegistry();
        registry.getArena().setTribuneSpawn(position(1.0D, 2.0D, 3.0D));
        registry.saveToConfiguration(configuration);

        assertEquals(512, configuration.getInt("arena.clean-blocks-per-tick"));
    }

    @Test
    void ignoresAConfigurationOfTheFormerMultiArenaSetup() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("final-arena", "Colosseum");
        configuration.set("arenas.Colosseum.regions.fight.corner1.world", "world");

        assertTrue(ArenaRegistry.containsLegacyMultiArenaConfiguration(configuration));

        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(configuration);

        assertFalse(registry.isArenaFullyConfigured());
        assertTrue(registry.getArena().getConfiguredRegions().isEmpty());
    }

    @Test
    void dropsTheEntriesOfTheFormerMultiArenaSetupWithTheNextSave() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("final-arena", "Colosseum");
        configuration.set("arenas.Colosseum.regions.fight.corner1.world", "world");

        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(configuration);
        registry.saveToConfiguration(configuration);

        assertFalse(ArenaRegistry.containsLegacyMultiArenaConfiguration(configuration));
    }

    @Test
    void startsWithAnUnconfiguredArenaWhenTheSectionIsMissing() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(new MemoryConfiguration());

        assertFalse(registry.isArenaFullyConfigured());
        assertTrue(registry.findFullyConfiguredArena().isEmpty());
        assertEquals(ArenaRegionType.values().length, registry.getArena().getMissingRegionTypes().size());
    }

    private PositionSnapshot position(double x, double y, double z) {
        return new PositionSnapshot("world", x, y, z, 0.0F, 0.0F);
    }
}
