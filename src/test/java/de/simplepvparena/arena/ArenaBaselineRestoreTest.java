package de.simplepvparena.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.capture;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.position;
import static de.simplepvparena.arena.ArenaBaselineSnapshotTest.shape;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBaselineRestoreTest {

    private static final int BATCH_SIZE = 3;
    private static final int MAXIMUM_TICKS = 200;

    @Test
    void turnsBlocksAddedSinceTheCalculationIntoAirAgain() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.STONE);
        ArenaBaselineSnapshot baseline = capture(shape, world, 4);

        world.put(position(0, 65, 0), Material.COBBLESTONE);
        world.put(position(1, 65, 0), Material.OAK_PLANKS);

        assertEquals(2L, clean(world, shape, Optional.of(baseline)));
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

        assertEquals(2L, clean(world, shape, Optional.of(baseline)));
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

        assertEquals(0L, clean(world, shape, Optional.of(baseline)), "no block has to be written");
        assertEquals(Material.STONE, world.get(position(0, 64, 0)));
    }

    @Test
    void emptiesTheFightAreaWithoutABaseline() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.STONE);
        world.put(position(0, 65, 0), Material.COBBLESTONE);

        assertEquals(3L, clean(world, shape, Optional.empty()));
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
        assertTrue(emptied.endsWith("1 border mark was removed"));
    }

    private long clean(Map<Long, Material> world, ArenaShape shape, Optional<ArenaBaselineSnapshot> baseline) {
        FakeServerHarness harness = new FakeServerHarness(shape.getWorldName());
        world.forEach((encodedPosition, material) -> harness.setBlock(BlockPositionCodec.decodeX(encodedPosition),
                BlockPositionCodec.decodeY(encodedPosition), BlockPositionCodec.decodeZ(encodedPosition), material));
        ArenaCleaningService cleaningService = new ArenaCleaningService(harness.getPlugin(),
                (cleanedWorld, cleanedShape) -> 0);
        cleaningService.setBlocksPerTick(BATCH_SIZE);
        cleaningService.setAnimationDurationTicks(0L);
        ArenaCleaningPlan plan = ArenaCleaningPlan.of(shape, baseline);
        List<ArenaCleaningReport> reports = new ArrayList<>();

        assertTrue(cleaningService.startCleaning(plan, reports::add));
        harness.runUntilIdle(MAXIMUM_TICKS);

        assertEquals(1, reports.size());
        assertTrue(reports.get(0).isSuccessful(), reports.get(0).describeOutcome());
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(plan.getCleanedVolume());
        while (cursor.hasRemainingBlocks()) {
            for (long encodedPosition : cursor.nextBatch(BATCH_SIZE)) {
                world.put(encodedPosition, harness.getBlock(BlockPositionCodec.decodeX(encodedPosition),
                        BlockPositionCodec.decodeY(encodedPosition), BlockPositionCodec.decodeZ(encodedPosition)));
            }
        }
        return reports.get(0).changedBlockCount();
    }
}
