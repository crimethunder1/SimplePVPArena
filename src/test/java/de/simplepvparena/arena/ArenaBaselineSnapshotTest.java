package de.simplepvparena.arena;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBaselineSnapshotTest {

    private static final String WORLD_NAME = "event";

    @Test
    void keepsTheMaterialOfEveryBlockOfTheVolume() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.SAND);
        world.put(position(0, 65, 0), Material.OAK_PLANKS);

        ArenaBaselineSnapshot snapshot = capture(shape, world, 3);

        assertEquals(4L, snapshot.getBlockCount());
        assertEquals(Material.STONE, snapshot.findMaterialAt(0, 64, 0).orElseThrow());
        assertEquals(Material.SAND, snapshot.findMaterialAt(1, 64, 0).orElseThrow());
        assertEquals(Material.OAK_PLANKS, snapshot.findMaterialAt(0, 65, 0).orElseThrow());
        assertEquals(Material.AIR, snapshot.findMaterialAt(1, 65, 0).orElseThrow());
    }

    @Test
    void countsTheMaterialsAndTheBlocksThatAreNotAir() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        Map<Long, Material> world = new HashMap<>();
        world.put(position(0, 64, 0), Material.STONE);
        world.put(position(1, 64, 0), Material.STONE);

        ArenaBaselineSnapshot snapshot = capture(shape, world, 4);

        assertEquals(2, snapshot.getDistinctMaterialCount(), "stone and the air above it");
        assertEquals(2L, snapshot.getNonAirBlockCount());
        assertTrue(snapshot.describe().contains("4 blocks"));
    }

    @Test
    void refusesABaselineThatIsStillMissingBlocks() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape);

        builder.record(0, 64, 0, Material.STONE);
        builder.record(1, 64, 0, Material.STONE);

        assertFalse(builder.isComplete());
        assertEquals(2L, builder.getRecordedBlockCount());
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void countsABlockThatWasReadTwiceOnlyOnce() {
        ArenaShape shape = shape(64, 64, 0, 0);
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape);

        builder.record(0, 64, 0, Material.STONE);
        builder.record(0, 64, 0, Material.SAND);

        assertEquals(1L, builder.getRecordedBlockCount());
        assertEquals(Material.SAND, builder.build().findMaterialAt(0, 64, 0).orElseThrow());
    }

    @Test
    void refusesABlockThatDoesNotBelongToTheFightArea() {
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape(64, 65, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> builder.record(5, 64, 5, Material.STONE));
        assertThrows(IllegalArgumentException.class, () -> builder.record(0, 66, 0, Material.STONE),
                "the volume ends at the top of the border wall");
    }

    @Test
    void answersAirForABlockOutsideOfTheCapturedVolume() {
        ArenaBaselineSnapshot snapshot = capture(shape(64, 64, 0, 0), Map.of(position(0, 64, 0),
                Material.STONE), 1);

        assertTrue(snapshot.findMaterialAt(9, 64, 9).isEmpty());
        assertEquals(Material.AIR, snapshot.findMaterial(9, 64, 9));
        assertEquals(Material.STONE, snapshot.findMaterial(0, 64, 0));
    }

    @Test
    void tellsABlockOfTheCapturedArenaFromOneThatWasPutThereAfterwards() {
        ArenaBaselineSnapshot snapshot = capture(shape(64, 65, 0, 0, 1, 0),
                Map.of(position(0, 64, 0), Material.STONE), 4);

        assertTrue(snapshot.holdsCapturedMaterial(0, 64, 0, Material.STONE));
        assertFalse(snapshot.holdsCapturedMaterial(0, 64, 0, Material.SAND), "the stone was replaced");
        assertTrue(snapshot.holdsCapturedMaterial(1, 64, 0, Material.AIR), "the baseline holds air here");
        assertFalse(snapshot.holdsCapturedMaterial(1, 64, 0, Material.OAK_PLANKS), "and this was built into it");
        assertFalse(snapshot.holdsCapturedMaterial(0, 66, 0, Material.STONE),
                "above the captured volume nothing was ever read");
    }

    @Test
    void belongsOnlyToTheAreaItWasTakenOf() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0);
        ArenaBaselineSnapshot snapshot = capture(shape, Map.of(), 2);

        assertTrue(snapshot.covers(shape(64, 65, 0, 0, 1, 0)), "the same area calculated again");
        assertFalse(snapshot.covers(shape(64, 65, 0, 0)), "a smaller area");
        assertFalse(snapshot.covers(shape(64, 66, 0, 0, 1, 0)), "a higher border wall");
        assertFalse(snapshot.covers(shape(63, 65, 0, 0, 1, 0)), "another floor height");
    }

    @Test
    void namesTheRestorationInTheReportOfTheCleaning() {
        ArenaBaselineSnapshot snapshot = capture(shape(64, 64, 0, 0), Map.of(), 1);

        assertTrue(snapshot.describeChange().contains("baseline"));
        assertFalse(snapshot.describeChange().equals(ArenaCleaningTarget.EMPTY_ARENA.describeChange()));
    }

    static ArenaBaselineSnapshot capture(ArenaShape shape, Map<Long, Material> world, int batchSize) {
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(shape);
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape);
        while (cursor.hasRemainingBlocks()) {
            for (long encodedPosition : cursor.nextBatch(batchSize)) {
                builder.record(encodedPosition, world.getOrDefault(encodedPosition, Material.AIR));
            }
        }
        assertTrue(builder.isComplete());
        return builder.build();
    }

    static long position(int blockX, int blockY, int blockZ) {
        return BlockPositionCodec.encode(blockX, blockY, blockZ);
    }

    static ArenaShape shape(int referenceY, int wallTopY, int... coordinatePairs) {
        long[] cells = new long[coordinatePairs.length / 2];
        for (int cellIndex = 0; cellIndex < cells.length; cellIndex++) {
            cells[cellIndex] = HorizontalCellCodec.encode(coordinatePairs[cellIndex * 2],
                    coordinatePairs[cellIndex * 2 + 1]);
        }
        return new ArenaShape(WORLD_NAME, referenceY, wallTopY, cells);
    }
}
