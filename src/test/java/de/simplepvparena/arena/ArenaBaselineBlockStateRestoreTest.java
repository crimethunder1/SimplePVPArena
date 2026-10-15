package de.simplepvparena.arena;

import de.simplepvparena.support.FakeBlockData;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBaselineBlockStateRestoreTest {

    private static final String WORLD_NAME = "event";
    private static final int FLOOR_Y = 64;
    private static final int WALL_TOP_Y = 66;
    private static final int MAXIMUM_TICKS = 200;

    private FakeServerHarness harness;
    private ArenaShape shape;
    private ArenaBaselineService baselineService;
    private ArenaCleaningService cleaningService;

    @BeforeEach
    void setUp() {
        harness = new FakeServerHarness(WORLD_NAME);
        long[] cells = new long[9];
        int cellIndex = 0;
        for (int blockX = 0; blockX < 3; blockX++) {
            for (int blockZ = 0; blockZ < 3; blockZ++) {
                cells[cellIndex++] = HorizontalCellCodec.encode(blockX, blockZ);
            }
        }
        shape = new ArenaShape(WORLD_NAME, FLOOR_Y, WALL_TOP_Y, cells);
        baselineService = new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        cleaningService = new ArenaCleaningService(harness.getPlugin(), (world, cleanedShape) -> 0);
        cleaningService.setAnimationDurationTicks(0L);
    }

    @Test
    void restoresStairsAndSlabsWithTheirOrientation() {
        BlockData stairs = FakeBlockData.of(Material.OAK_STAIRS,
                "facing=east,half=bottom,shape=straight,waterlogged=false");
        BlockData slab = FakeBlockData.of(Material.STONE_SLAB, "type=top,waterlogged=false");
        harness.setBlockData(0, FLOOR_Y, 0, stairs);
        harness.setBlockData(1, FLOOR_Y, 0, slab);
        capture(0);

        harness.setBlockData(0, FLOOR_Y, 0, FakeBlockData.of(Material.OAK_STAIRS,
                "facing=west,half=top,shape=straight,waterlogged=false"));
        harness.setBlockData(1, FLOOR_Y, 0, FakeBlockData.of(Material.STONE_SLAB, "type=bottom,waterlogged=false"));
        ArenaCleaningReport report = clean();

        assertEquals(stairs, harness.getBlockData(0, FLOOR_Y, 0),
                "the material alone is the same, the stairs still have to face east again");
        assertEquals(slab, harness.getBlockData(1, FLOOR_Y, 0), "and the slab sits in the upper half again");
        assertEquals(2L, report.changedBlockCount());
        assertEquals(0, harness.getWritesWithPhysicsCount(), "the cleaning writes without physics");
    }

    @Test
    void closesBothHalvesOfADoorThatWasOpenedDuringTheMatch() {
        BlockData closedLowerHalf = door("lower", false);
        BlockData closedUpperHalf = door("upper", false);
        harness.setBlockData(1, FLOOR_Y, 1, closedLowerHalf);
        harness.setBlockData(1, FLOOR_Y + 1, 1, closedUpperHalf);
        capture(0);

        harness.setBlockData(1, FLOOR_Y, 1, door("lower", true));
        harness.setBlockData(1, FLOOR_Y + 1, 1, door("upper", true));
        clean();

        assertEquals(closedLowerHalf, harness.getBlockData(1, FLOOR_Y, 1));
        assertEquals(closedUpperHalf, harness.getBlockData(1, FLOOR_Y + 1, 1));
        assertEquals(0, harness.getWritesWithPhysicsCount());
    }

    @Test
    void putsBackBothHalvesOfAnOpenDoorThatWasBrokenDuringTheMatch() {
        BlockData openLowerHalf = door("lower", true);
        BlockData openUpperHalf = door("upper", true);
        harness.setBlockData(1, FLOOR_Y, 1, openLowerHalf);
        harness.setBlockData(1, FLOOR_Y + 1, 1, openUpperHalf);
        capture(0);

        harness.setBlock(1, FLOOR_Y, 1, Material.AIR);
        harness.setBlock(1, FLOOR_Y + 1, 1, Material.AIR);
        clean();

        assertEquals(openLowerHalf, harness.getBlockData(1, FLOOR_Y, 1), "the lower half, still open");
        assertEquals(openUpperHalf, harness.getBlockData(1, FLOOR_Y + 1, 1), "and the upper half on top of it");
        assertEquals(0, harness.getWritesWithPhysicsCount());
    }

    @Test
    void restoresTheConnectionsOfFencesAndPanesAndTheWaterInABlock() {
        BlockData fence = FakeBlockData.of(Material.OAK_FENCE,
                "east=true,north=false,south=true,waterlogged=false,west=false");
        BlockData pane = FakeBlockData.of(Material.GLASS_PANE,
                "east=false,north=true,south=true,waterlogged=false,west=false");
        BlockData waterloggedStairs = FakeBlockData.of(Material.STONE_STAIRS,
                "facing=north,half=bottom,shape=straight,waterlogged=true");
        BlockData stillWater = FakeBlockData.of(Material.WATER, "level=0");
        harness.setBlockData(0, FLOOR_Y, 0, fence);
        harness.setBlockData(1, FLOOR_Y, 0, pane);
        harness.setBlockData(2, FLOOR_Y, 0, waterloggedStairs);
        harness.setBlockData(2, FLOOR_Y, 2, stillWater);
        capture(0);

        harness.setBlockData(0, FLOOR_Y, 0, FakeBlockData.of(Material.OAK_FENCE,
                "east=true,north=true,south=true,waterlogged=false,west=true"));
        harness.setBlockData(1, FLOOR_Y, 0, FakeBlockData.of(Material.GLASS_PANE,
                "east=false,north=false,south=false,waterlogged=false,west=false"));
        harness.setBlockData(2, FLOOR_Y, 0, FakeBlockData.of(Material.STONE_STAIRS,
                "facing=north,half=bottom,shape=straight,waterlogged=false"));
        harness.setBlockData(2, FLOOR_Y, 2, FakeBlockData.of(Material.WATER, "level=3"));
        clean();

        assertEquals(fence, harness.getBlockData(0, FLOOR_Y, 0), "the fence connects as it did");
        assertEquals(pane, harness.getBlockData(1, FLOOR_Y, 0), "and so does the pane");
        assertEquals(waterloggedStairs, harness.getBlockData(2, FLOOR_Y, 0), "the drained stairs hold water again");
        assertEquals(stillWater, harness.getBlockData(2, FLOOR_Y, 2), "and the water is a source again");
        assertEquals(0, harness.getWritesWithPhysicsCount());
    }

    @Test
    void writesBackTheSignTheBannerTheHeadAndTheChestWithWhatTheyHeld() {
        BlockData sign = FakeBlockData.withBlockEntity(Material.OAK_SIGN, "rotation=4,waterlogged=false");
        BlockData banner = FakeBlockData.withBlockEntity(Material.WHITE_BANNER, "rotation=0");
        BlockData head = FakeBlockData.withBlockEntity(Material.PLAYER_HEAD, "powered=false,rotation=8");
        BlockData chest = FakeBlockData.withBlockEntity(Material.CHEST, "facing=north,type=single,waterlogged=false");
        String signText = "front: Welcome, black, glowing; back: Arena; waxed";
        String bannerPatterns = "patterns: red cross, blue border";
        String headProfile = "profile: Notch";
        String chestItems = "items: 3 diamonds, 16 arrows";
        harness.setBlockData(0, FLOOR_Y, 0, sign);
        harness.setBlockEntityContent(0, FLOOR_Y, 0, signText);
        harness.setBlockData(1, FLOOR_Y, 0, banner);
        harness.setBlockEntityContent(1, FLOOR_Y, 0, bannerPatterns);
        harness.setBlockData(2, FLOOR_Y, 0, head);
        harness.setBlockEntityContent(2, FLOOR_Y, 0, headProfile);
        harness.setBlockData(0, FLOOR_Y, 2, chest);
        harness.setBlockEntityContent(0, FLOOR_Y, 2, chestItems);
        harness.setBlock(1, FLOOR_Y, 1, Material.STONE);
        ArenaBaselineSnapshot snapshot = capture(0);

        harness.setBlock(0, FLOOR_Y, 0, Material.AIR);
        harness.setBlockEntityContent(1, FLOOR_Y, 0, "patterns: none");
        harness.setBlock(2, FLOOR_Y, 0, Material.STONE);
        harness.setBlockEntityContent(0, FLOOR_Y, 2, "items: none");
        clean();

        assertEquals(4, snapshot.getBlockEntityCount(), "only the four blocks with contents are kept as a state");
        assertEquals(sign, harness.getBlockData(0, FLOOR_Y, 0));
        assertEquals(signText, harness.getBlockEntityContent(0, FLOOR_Y, 0), "the broken sign says what it said");
        assertEquals(bannerPatterns, harness.getBlockEntityContent(1, FLOOR_Y, 0), "the banner has its patterns");
        assertEquals(head, harness.getBlockData(2, FLOOR_Y, 0));
        assertEquals(headProfile, harness.getBlockEntityContent(2, FLOOR_Y, 0), "the head shows the same player");
        assertEquals(chestItems, harness.getBlockEntityContent(0, FLOOR_Y, 2), "and the emptied chest is full");
        assertEquals(Material.STONE, harness.getBlock(1, FLOOR_Y, 1));
        assertEquals(0, harness.getWritesWithPhysicsCount());
    }

    @Test
    void closesAHoleFiveBlocksDeepAndLeavesTheGroundUnderTheCapturedVolumeAlone() {
        fillGround(FLOOR_Y - 11, FLOOR_Y);
        ArenaBaselineSnapshot snapshot = capture(10);

        for (int blockY = FLOOR_Y - 4; blockY <= FLOOR_Y; blockY++) {
            harness.setBlock(1, blockY, 1, Material.AIR);
        }
        harness.setBlock(1, FLOOR_Y - 11, 1, Material.AIR);
        ArenaCleaningReport report = clean();

        assertEquals(FLOOR_Y - 10, snapshot.getCapturedVolume().getFloorY());
        for (int blockY = FLOOR_Y - 4; blockY <= FLOOR_Y; blockY++) {
            assertEquals(Material.STONE, harness.getBlock(1, blockY, 1), "the hole is closed at y " + blockY);
        }
        assertEquals(Material.AIR, harness.getBlock(1, FLOOR_Y - 11, 1),
                "eleven blocks under the floor lies outside the volume and is not touched");
        assertEquals(9L * (WALL_TOP_Y - (FLOOR_Y - 10) + 1), report.checkedBlockCount());
        assertEquals(5L, report.changedBlockCount());
    }

    @Test
    void takesOnlyTheFloorLayerWithADepthOfZero() {
        fillGround(FLOOR_Y - 11, FLOOR_Y);
        ArenaBaselineSnapshot snapshot = capture(0);

        for (int blockY = FLOOR_Y - 4; blockY <= FLOOR_Y; blockY++) {
            harness.setBlock(1, blockY, 1, Material.AIR);
        }
        clean();

        assertEquals(FLOOR_Y, snapshot.getCapturedVolume().getFloorY());
        assertEquals(Material.STONE, harness.getBlock(1, FLOOR_Y, 1), "the floor layer is restored");
        for (int blockY = FLOOR_Y - 4; blockY < FLOOR_Y; blockY++) {
            assertEquals(Material.AIR, harness.getBlock(1, blockY, 1), "nothing under it is, y " + blockY);
        }
    }

    @Test
    void keepsTheDepthOfTheBaselineWhenTheSettingChangesAfterwards() {
        fillGround(FLOOR_Y - 11, FLOOR_Y);
        capture(10);
        baselineService.setDepthBelowFloor(0);

        harness.setBlock(1, FLOOR_Y - 3, 1, Material.AIR);
        clean();

        assertEquals(Material.STONE, harness.getBlock(1, FLOOR_Y - 3, 1),
                "a changed depth only applies to the next calculation");
    }

    @Test
    void stopsTheCaptureWhenTheAreaHoldsMoreBlockStatesThanThePaletteCanTellApart() {
        long[] cells = new long[256 * 257];
        int cellIndex = 0;
        for (int blockX = 0; blockX < 256; blockX++) {
            for (int blockZ = 0; blockZ < 257; blockZ++) {
                cells[cellIndex] = HorizontalCellCodec.encode(blockX, blockZ);
                harness.setBlockData(blockX, FLOOR_Y, blockZ, FakeBlockData.of(Material.STONE, "variant=" + cellIndex));
                cellIndex++;
            }
        }
        ArenaShape largeShape = new ArenaShape(WORLD_NAME, FLOOR_Y, FLOOR_Y, cells);
        baselineService.setDepthBelowFloor(0);
        List<ArenaBaselineReport> reports = new ArrayList<>();

        assertTrue(baselineService.startCapture(largeShape, reports::add));
        harness.runUntilIdle(MAXIMUM_TICKS);

        assertEquals(1, reports.size());
        assertFalse(reports.get(0).isCaptured());
        assertTrue(reports.get(0).describe().contains(ArenaBaselineSnapshotBuilder.MAXIMUM_PALETTE_SIZE
                + " different block states"), reports.get(0).describe());
        assertTrue(baselineService.findStoredSnapshot().isEmpty(), "no baseline with wrong blocks is kept");
    }

    private void fillGround(int lowestY, int highestY) {
        for (int blockX = 0; blockX < 3; blockX++) {
            for (int blockZ = 0; blockZ < 3; blockZ++) {
                for (int blockY = lowestY; blockY <= highestY; blockY++) {
                    harness.setBlock(blockX, blockY, blockZ, Material.STONE);
                }
            }
        }
    }

    private static BlockData door(String half, boolean open) {
        return FakeBlockData.of(Material.OAK_DOOR,
                "facing=north,half=" + half + ",hinge=left,open=" + open + ",powered=false");
    }

    private ArenaBaselineSnapshot capture(int depthBelowFloor) {
        baselineService.setDepthBelowFloor(depthBelowFloor);
        List<ArenaBaselineReport> reports = new ArrayList<>();
        assertTrue(baselineService.startCapture(shape, reports::add));
        harness.runUntilIdle(MAXIMUM_TICKS);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).isCaptured(), reports.get(0).describe());
        return baselineService.findSnapshot(shape).orElseThrow();
    }

    private ArenaCleaningReport clean() {
        List<ArenaCleaningReport> reports = new ArrayList<>();
        assertTrue(cleaningService.startCleaning(ArenaCleaningPlan.of(shape, baselineService.findStoredSnapshot()),
                reports::add));
        harness.runUntilIdle(MAXIMUM_TICKS);
        assertEquals(1, reports.size());
        assertTrue(reports.get(0).isSuccessful(), reports.get(0).describeOutcome());
        return reports.get(0);
    }
}
