package de.simplepvparena.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBaselineRestoreTest {

    @Test
    void turnsBlocksAddedSinceTheCalculationIntoAirAgain() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.STONE);
        ArenaBaselineSnapshot baseline = capture(shape, world, 4);

        world.put(position(0, 65, 0), Material.COBBLESTONE);
        world.put(position(1, 65, 0), Material.OAK_PLANKS);

        assertEquals(2L, clean(world, shape, baseline));
        assertEquals(Material.AIR, world.get(position(0, 65, 0)));
        assertEquals(Material.AIR, world.get(position(1, 65, 0)));
        assertEquals(Material.STONE, world.get(position(0, 64, 0)), "the floor of the baseline stays");
    }

    @Test
    void putsBlocksThatWereBrokenOutOfTheArenaBackAgain() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.SAND);
        ArenaBaselineSnapshot baseline = capture(shape, world, 4);

        world.put(position(0, 64, 0), Material.AIR);
        world.put(position(1, 64, 0), Material.COBBLESTONE);

        assertEquals(2L, clean(world, shape, baseline));
        assertEquals(Material.STONE, world.get(position(0, 64, 0)));
        assertEquals(Material.SAND, world.get(position(1, 64, 0)),
                "a block that was replaced becomes the material of the baseline again");
    }

    @Test
    void leavesAnArenaNobodyChangedAlone() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.STONE);
        ArenaBaselineSnapshot baseline = capture(shape, world, 4);

        assertEquals(0L, clean(world, shape, baseline), "no block has to be written");
        assertEquals(Material.STONE, world.get(position(0, 64, 0)));
    }

    @Test
    void emptiesTheFightAreaWithoutABaseline() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.STONE);
        world.put(position(0, 65, 0), Material.COBBLESTONE);

        assertEquals(3L, clean(world, shape, ArenaCleaningTarget.EMPTY_ARENA));
        assertEquals(Material.AIR, world.get(position(0, 64, 0)));
        assertEquals(Material.AIR, world.get(position(1, 64, 0)));
        assertEquals(Material.AIR, world.get(position(0, 65, 0)));
    }

    @Test
    void reportsWhetherTheArenaWasRestoredOrEmptied() {
        ArenaBaselineSnapshot baseline = capture(shape(64, 64, 0, 0), Map.of(), 1);

        String restored = new ArenaCleaningReport(4L, 2L, 1, baseline.describeChange()).describe();
        String emptied = new ArenaCleaningReport(4L, 2L, 1,
                ArenaCleaningTarget.EMPTY_ARENA.describeChange()).describe();

        assertTrue(restored.startsWith("2 of 4 checked blocks "));
        assertTrue(restored.contains("baseline"), restored);
        assertTrue(emptied.contains("were set to air"), emptied);
        assertTrue(emptied.endsWith("1 border marks were removed"));
    }

    private long clean(Map<Long, Material> world, ArenaShape shape, ArenaCleaningTarget cleaningTarget) {
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape);
        long changedBlockCount = 0L;
        while (cursor.hasRemainingBlocks()) {
            for (long encodedPosition : cursor.nextBatch(3)) {
                Material targetMaterial = cleaningTarget.findMaterial(
                        BlockPositionCodec.decodeX(encodedPosition),
                        BlockPositionCodec.decodeY(encodedPosition),
                        BlockPositionCodec.decodeZ(encodedPosition));
                if (world.getOrDefault(encodedPosition, Material.AIR) == targetMaterial) {
                    continue;
                }
                world.put(encodedPosition, targetMaterial);
                changedBlockCount++;
            }
        }
        return changedBlockCount;
    }
}
