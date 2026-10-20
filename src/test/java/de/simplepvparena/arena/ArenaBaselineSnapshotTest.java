package de.simplepvparena.arena;

import de.simplepvparena.support.FakeBlockData;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBaselineSnapshotTest {

    private static final String WORLD_NAME = "event";

    static final Supplier<BlockState> NO_BLOCK_ENTITY = () -> {
        throw new AssertionError("a block without a block entity was asked for one");
    };

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

        builder.record(0, 64, 0, FakeBlockData.of(Material.STONE), NO_BLOCK_ENTITY);
        builder.record(1, 64, 0, FakeBlockData.of(Material.STONE), NO_BLOCK_ENTITY);

        assertFalse(builder.isComplete());
        assertEquals(2L, builder.getRecordedBlockCount());
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void countsABlockThatWasReadTwiceOnlyOnce() {
        ArenaShape shape = shape(64, 64, 0, 0);
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape);

        builder.record(0, 64, 0, FakeBlockData.of(Material.STONE), NO_BLOCK_ENTITY);
        builder.record(0, 64, 0, FakeBlockData.of(Material.SAND), NO_BLOCK_ENTITY);

        assertEquals(1L, builder.getRecordedBlockCount());
        assertEquals(Material.SAND, builder.build().findMaterialAt(0, 64, 0).orElseThrow());
    }

    @Test
    void refusesABlockThatDoesNotBelongToTheFightArea() {
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape(64, 65, 0, 0));

        assertThrows(IllegalArgumentException.class,
                () -> builder.record(5, 64, 5, FakeBlockData.of(Material.STONE), NO_BLOCK_ENTITY));
        assertThrows(IllegalArgumentException.class,
                () -> builder.record(0, 66, 0, FakeBlockData.of(Material.STONE), NO_BLOCK_ENTITY),
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

        assertTrue(new ArenaCleaningReport(4L, 2L, 0, snapshot.describeChange()).describe().contains("baseline"));
        assertFalse(snapshot.describeChange().equals(ArenaCleaningTarget.EMPTY_ARENA.describeChange()));
    }

    @Test
    void tellsTheBlocksUnderTheFloorOfTheVolumeApartFromTheOnesAboveIt() {
        ArenaBaselineSnapshot snapshot = capture(shape(64, 65, 0, 0, 1, 0), Map.of(), 4);

        assertTrue(snapshot.isBelowTheCapturedVolume(63), "the floor the border wall was laid on");
        assertFalse(snapshot.isBelowTheCapturedVolume(64), "the lowest block of the volume itself");
        assertFalse(snapshot.isBelowTheCapturedVolume(65));
        assertFalse(snapshot.isBelowTheCapturedVolume(66), "and a tower over the top of the wall");
    }

    @Test
    void keepsEveryDifferentBlockStateOnlyOnceInThePalette() {
        ArenaShape shape = shape(64, 65, 0, 0, 1, 0, 2, 0, 3, 0);
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape);
        String eastFacing = "facing=east,half=bottom,shape=straight,waterlogged=false";
        String westFacing = "facing=west,half=bottom,shape=straight,waterlogged=false";

        for (int blockX = 0; blockX < 4; blockX++) {
            builder.record(blockX, 64, 0, FakeBlockData.of(Material.STONE), NO_BLOCK_ENTITY);
        }
        builder.record(0, 65, 0, FakeBlockData.of(Material.OAK_STAIRS, eastFacing), NO_BLOCK_ENTITY);
        builder.record(1, 65, 0, FakeBlockData.of(Material.OAK_STAIRS, eastFacing), NO_BLOCK_ENTITY);
        builder.record(2, 65, 0, FakeBlockData.of(Material.OAK_STAIRS, westFacing), NO_BLOCK_ENTITY);
        builder.record(3, 65, 0, FakeBlockData.of(Material.AIR), NO_BLOCK_ENTITY);
        ArenaBaselineSnapshot snapshot = builder.build();

        assertEquals(4, builder.getPaletteSize(), "stone, both stairs and air, each of them once");
        assertEquals(4, snapshot.getPaletteSize());
        assertEquals(4, snapshot.getDistinctBlockStateCount());
        assertEquals(3, snapshot.getDistinctMaterialCount(), "the two stairs are one material");
        assertEquals(7L, snapshot.getNonAirBlockCount());
        assertEquals(FakeBlockData.of(Material.OAK_STAIRS, westFacing),
                snapshot.findBlockDataAt(2, 65, 0).orElseThrow());
        assertEquals(FakeBlockData.of(Material.OAK_STAIRS, eastFacing),
                snapshot.findBlockData(1, 65, 0).orElseThrow());
        assertTrue(snapshot.findBlockData(9, 65, 9).isEmpty(), "outside of the volume no state is known");
        assertTrue(snapshot.findBlockEntity(0, 64, 0).isEmpty(), "and stone holds no contents");
    }

    @Test
    void stopsWithAReadableReasonWhenThePaletteRunsOver() {
        int cellCount = ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE + 1;
        int[] coordinatePairs = new int[cellCount * 2];
        for (int cellIndex = 0; cellIndex < cellCount; cellIndex++) {
            coordinatePairs[cellIndex * 2] = cellIndex % 512;
            coordinatePairs[cellIndex * 2 + 1] = cellIndex / 512;
        }
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape(64, 64, coordinatePairs));
        for (int cellIndex = 0; cellIndex < ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE; cellIndex++) {
            builder.record(cellIndex % 512, 64, cellIndex / 512,
                    FakeBlockData.of(Material.STONE, "variant=" + cellIndex), NO_BLOCK_ENTITY);
        }
        int lastCellIndex = cellCount - 1;

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> builder.record(lastCellIndex % 512, 64, lastCellIndex / 512,
                        FakeBlockData.of(Material.STONE, "variant=" + lastCellIndex), NO_BLOCK_ENTITY));

        assertTrue(failure.getMessage().contains(ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE
                + " different block states"), failure.getMessage());
        assertEquals(ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE, builder.getPaletteSize());
        assertEquals(ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE, builder.getRecordedBlockCount(),
                "the block that ran over is not recorded");
        assertThrows(IllegalStateException.class, builder::build, "and no incomplete baseline comes out of it");
    }

    @Test
    void reachesTheConfiguredDepthBelowTheFloor() {
        ArenaBaselineSnapshot snapshot = capture(shape(64, 65, 0, 0), Map.of(position(0, 54, 0), Material.STONE),
                4, 10);

        assertEquals(12L, snapshot.getBlockCount(), "ten layers under the floor and the two of the fight area");
        assertEquals(54, snapshot.getCapturedVolume().getFloorY());
        assertEquals(64, snapshot.getShape().getFloorY(), "the fight area itself keeps its floor");
        assertEquals(Material.STONE, snapshot.findMaterial(0, 54, 0));
        assertTrue(snapshot.findMaterialAt(0, 53, 0).isEmpty());
        assertTrue(snapshot.isBelowTheCapturedVolume(53), "the first block under the volume");
        assertFalse(snapshot.isBelowTheCapturedVolume(54));
        assertTrue(snapshot.covers(shape(64, 65, 0, 0)), "the depth does not change the area it belongs to");
    }

    static ArenaBaselineSnapshot capture(ArenaShape shape, Map<Long, Material> world, int batchSize) {
        return capture(shape, world, batchSize, 0);
    }

    static ArenaBaselineSnapshot capture(ArenaShape shape, Map<Long, Material> world, int batchSize,
                                         int depthBelowFloor) {
        ArenaBaselineSnapshotBuilder builder = new ArenaBaselineSnapshotBuilder(shape, depthBelowFloor);
        ArenaShapeBlockCursor cursor = new ArenaShapeBlockCursor(builder.getCapturedVolume());
        while (cursor.hasRemainingBlocks()) {
            for (long encodedPosition : cursor.nextBatch(batchSize)) {
                builder.record(encodedPosition, FakeBlockData.of(world.getOrDefault(encodedPosition, Material.AIR)),
                        NO_BLOCK_ENTITY);
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
