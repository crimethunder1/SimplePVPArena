package de.simplepvparena.arena;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaRegistryTest {

    @Test
    void writesAndReadsTheCalculatedFightArea() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.getArena().setShape(shape(64, 72, 0, 0, 1, 0, 2, 0, 2, 1, -4, 9));
        registry.getArena().setTribuneSpawn(position(50.0D, 70.0D, 50.0D));

        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);

        ArenaRegistry reloadedRegistry = new ArenaRegistry();
        reloadedRegistry.loadFromConfiguration(configuration);

        ArenaShape reloadedShape = reloadedRegistry.getArena().getShape().orElseThrow();
        assertTrue(reloadedRegistry.isArenaFullyConfigured());
        assertEquals("world", reloadedShape.getWorldName());
        assertEquals(64, reloadedShape.getReferenceY());
        assertEquals(72, reloadedShape.getWallTopY());
        assertEquals(5, reloadedShape.getCellCount());
        assertTrue(reloadedShape.containsCell(2, 1));
        assertTrue(reloadedShape.containsCell(-4, 9));
        assertFalse(reloadedShape.containsCell(1, 1));
        assertEquals(70.0D, reloadedRegistry.getArena().getConfiguredTribuneSpawn().orElseThrow().y());
    }

    @Test
    void storesTheCellsAsRunLengthEncodedRows() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.getArena().setShape(shape(64, 70, 0, 0, 0, 1, 0, 2, 1, 0));

        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);

        assertEquals(List.of("0:0..2", "1:0..0"), configuration.getStringList("arena.shape.rows"));
        assertEquals("world", configuration.getString("arena.shape.world"));
        assertEquals(64, configuration.getInt("arena.shape.reference-y"));
        assertEquals(70, configuration.getInt("arena.shape.wall-top-y"));
    }

    @Test
    void keepsOtherEntriesOfTheArenaSection() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("arena.clean-blocks-per-tick", 512);
        configuration.set("arena.shape-maximum-cells", 1234);

        ArenaRegistry registry = new ArenaRegistry();
        registry.getArena().setTribuneSpawn(position(1.0D, 2.0D, 3.0D));
        registry.saveToConfiguration(configuration);

        assertEquals(512, configuration.getInt("arena.clean-blocks-per-tick"));
        assertEquals(1234, configuration.getInt("arena.shape-maximum-cells"));
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
        assertTrue(registry.getArena().getShape().isEmpty());
    }

    @Test
    void ignoresTheRectangleRegionsOfTheFormerArenaSetup() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("arena.regions.fight.corner1.world", "world");
        configuration.set("arena.regions.fight.corner1.x", 0);

        assertTrue(ArenaRegistry.containsLegacyRegionConfiguration(configuration));

        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(configuration);
        registry.saveToConfiguration(configuration);

        assertFalse(registry.isArenaFullyConfigured());
        assertTrue(registry.getArena().getShape().isEmpty());
        assertFalse(ArenaRegistry.containsLegacyRegionConfiguration(configuration));
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
    void reportsAStoredShapeThatCannotBeRead() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("arena.shape.world", "world");
        configuration.set("arena.shape.reference-y", 64);
        configuration.set("arena.shape.rows", List.of("0:0..2", "broken"));

        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(configuration);

        assertTrue(registry.getArena().getShape().isEmpty());
        assertTrue(registry.getShapeLoadFailure().orElseThrow().contains("broken"));
    }

    @Test
    void writesAndReadsTheFloorOfTheCleanedVolume() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.getArena().setShape(new ArenaShape("world", 90, 64, 70,
                new long[] {HorizontalCellCodec.encode(0, 0), HorizontalCellCodec.encode(1, 0)}));

        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);

        assertEquals(90, configuration.getInt("arena.shape.reference-y"));
        assertEquals(64, configuration.getInt("arena.shape.floor-y"));

        ArenaRegistry reloadedRegistry = new ArenaRegistry();
        reloadedRegistry.loadFromConfiguration(configuration);
        ArenaShape reloadedShape = reloadedRegistry.getArena().getShape().orElseThrow();

        assertEquals(90, reloadedShape.getReferenceY());
        assertEquals(64, reloadedShape.getFloorY());
        assertEquals(70, reloadedShape.getWallTopY());
        assertEquals(2L * 7, reloadedShape.getBlockCount());
    }

    @Test
    void readsAShapeWithoutAStoredFloorWithTheReferenceHeightAsItsFloor() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("arena.shape.world", "world");
        configuration.set("arena.shape.reference-y", 64);
        configuration.set("arena.shape.wall-top-y", 70);
        configuration.set("arena.shape.rows", List.of("0:0..1"));

        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(configuration);
        ArenaShape shape = registry.getArena().getShape().orElseThrow();

        assertEquals(64, shape.getFloorY());
        assertEquals(70, shape.getWallTopY());
        assertTrue(registry.getShapeLoadFailure().isEmpty());
    }

    @Test
    void startsWithAnUnconfiguredArenaWhenTheSectionIsMissing() {
        ArenaRegistry registry = new ArenaRegistry();
        registry.loadFromConfiguration(new MemoryConfiguration());

        assertFalse(registry.isArenaFullyConfigured());
        assertTrue(registry.findFullyConfiguredArena().isEmpty());
        assertTrue(registry.getShapeLoadFailure().isEmpty());
        assertEquals(2, registry.getArena().getMissingConfiguration().size());
    }

    private PositionSnapshot position(double x, double y, double z) {
        return new PositionSnapshot("world", x, y, z, 0.0F, 0.0F);
    }

    private ArenaShape shape(int referenceY, int wallTopY, int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return new ArenaShape("world", referenceY, wallTopY, cells);
    }
}
