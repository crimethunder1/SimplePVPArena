package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.support.FakeBlockData;
import org.bukkit.ExplosionResult;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaEnvironmentProtectionTest {

    private static final int ARENA_FLOOR_X = 5;
    private static final int ARENA_FLOOR_Z = 5;
    private static final int OPEN_X = 7;
    private static final int OPEN_Z = 7;
    private static final int OPEN_Y = ArenaMatchHarness.GROUND_Y + 1;
    private static final int TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int WALL_X = ArenaMatchHarness.WALL_MINIMUM;
    private static final int WALL_Z = 5;
    private static final int UNDER_THE_FLOOR_Y = ArenaMatchHarness.GROUND_Y - 1;
    private static final int FIRST_Y_UNDER_THE_CAPTURED_VOLUME =
            ArenaMatchHarness.GROUND_Y - ArenaBaselineService.DEFAULT_DEPTH_BELOW_FLOOR - 1;
    private static final int OVER_THE_WALL_Y = ArenaMatchHarness.WALL_TOP_Y + 1;
    private static final int LAST_X_INSIDE_THE_WALL = ArenaMatchHarness.WALL_MAXIMUM - 1;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaBaselineService baselineService;
    private ArenaEnvironmentProtectionListener protectionListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        spectator = harness.addPlayer("Spectator");
        admin = harness.addPlayer("Admin");
        admin.grantAdminPermission();
        baselineService = new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        assertTrue(baselineService.startCapture(harness.getShape(), report -> { }));
        harness.runTick();
        assertFalse(baselineService.isCapturing(), "the baseline is complete before anything explodes");
        protectionListener = new ArenaEnvironmentProtectionListener(harness.getArenaRegistry(), baselineService);
    }

    @Test
    void keepsOnlyTheBlocksInsideTheFightAreaOfAnEntityExplosion() {
        Block arenaFloor = harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z);
        Block openAir = harness.createBlock(OPEN_X, OPEN_Y, OPEN_Z);
        Block groundInsideTheBaseline = harness.createBlock(ARENA_FLOOR_X, UNDER_THE_FLOOR_Y, ARENA_FLOOR_Z);

        List<Block> destroyedBlocks = explodeAnEntity(arenaFloor, openAir, groundInsideTheBaseline,
                harness.createBlock(WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z),
                harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z),
                harness.createBlock(ARENA_FLOOR_X, FIRST_Y_UNDER_THE_CAPTURED_VOLUME, ARENA_FLOOR_Z));

        assertEquals(List.of(arenaFloor, openAir, groundInsideTheBaseline), destroyedBlocks);
    }

    @Test
    void keepsOnlyTheBlocksInsideTheFightAreaOfABlockExplosion() {
        Block arenaFloor = harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z);
        Block openAir = harness.createBlock(OPEN_X, OPEN_Y, OPEN_Z);

        List<Block> destroyedBlocks = explodeABlock(harness.createBlock(1, OPEN_Y, WALL_Z), arenaFloor,
                harness.createBlock(WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z), openAir,
                harness.createBlock(WALL_X - 1, ArenaMatchHarness.GROUND_Y, WALL_Z));

        assertEquals(List.of(arenaFloor, openAir), destroyedBlocks);
    }

    @Test
    void removesEveryBlockOfAnExplosionOutsideTheFightArea() {
        assertTrue(explodeAnEntity(harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z),
                harness.createBlock(TRIBUNE_X + 1, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z),
                harness.createBlock(TRIBUNE_X, UNDER_THE_FLOOR_Y, TRIBUNE_Z)).isEmpty(), "a creeper on the tribune");
        assertTrue(explodeABlock(harness.createBlock(TRIBUNE_X, OPEN_Y, TRIBUNE_Z),
                harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z),
                harness.createBlock(TRIBUNE_X, OPEN_Y, TRIBUNE_Z)).isEmpty(), "and a bed blown up there");
    }

    @Test
    void protectsTheGroundUnderTheFloorFromAnExplosionWithoutABaseline() {
        Block arenaFloor = harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z);
        Block groundUnderTheFloor = harness.createBlock(ARENA_FLOOR_X, UNDER_THE_FLOOR_Y, ARENA_FLOOR_Z);
        baselineService.discardSnapshot();

        assertEquals(List.of(arenaFloor), explodeAnEntity(arenaFloor, groundUnderTheFloor),
                "without a baseline the cleaning only reaches down to the floor of the fight area");
    }

    @Test
    void leavesAnExplosionInAnotherWorldAlone() {
        Block lobbyBlock = harness.createBlockInTheLobby(0, 100, 0);

        assertEquals(List.of(lobbyBlock), explodeABlock(harness.createBlockInTheLobby(0, 101, 0), lobbyBlock));
    }

    @Test
    void leavesEveryExplosionAloneWhileNoFightAreaIsCalculated() {
        Block tribune = harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z);
        harness.getArenaRegistry().getArena().setShape(null);

        assertEquals(List.of(tribune), explodeAnEntity(tribune));
    }

    @Test
    void keepsFireFromBurningAwayTheTribuneButNotInsideTheFightArea() {
        assertTrue(isBurnCancelled(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z), "the tribune");
        assertTrue(isBurnCancelled(WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z), "the border wall");
        assertFalse(isBurnCancelled(OPEN_X, OPEN_Y, OPEN_Z), "while the fight area burns, the cleaning restores it");
    }

    @Test
    void keepsFireFromSpreadingOutOfTheFightArea() {
        assertTrue(isSpreadCancelled(WALL_X, OPEN_Y, WALL_Z), "onto the border wall");
        assertTrue(isSpreadCancelled(TRIBUNE_X, OPEN_Y, TRIBUNE_Z), "onto the tribune");
        assertTrue(isSpreadCancelled(ARENA_FLOOR_X, FIRST_Y_UNDER_THE_CAPTURED_VOLUME, ARENA_FLOOR_Z),
                "and into the ground under the captured volume");
        assertFalse(isSpreadCancelled(OPEN_X, OPEN_Y, OPEN_Z), "it still spreads inside the fight area");
    }

    @Test
    void letsOnlyAnAdminSetFireOutsideTheFightArea() {
        assertTrue(isIgnitionByPlayerCancelled(spectator, TRIBUNE_X, OPEN_Y, TRIBUNE_Z), "a spectator");
        assertTrue(isIgnitionByEntityCancelled(TRIBUNE_X, OPEN_Y, TRIBUNE_Z), "a burning arrow");
        assertFalse(isIgnitionByPlayerCancelled(admin, TRIBUNE_X, OPEN_Y, TRIBUNE_Z), "only the admin");
        assertFalse(isIgnitionByPlayerCancelled(spectator, OPEN_X, OPEN_Y, OPEN_Z),
                "while inside the fight area nothing is refused");
    }

    @Test
    void letsAPistonPushInsideTheFightArea() {
        assertFalse(isExtensionCancelled(harness.createBlock(3, OPEN_Y, 5), BlockFace.EAST,
                harness.createBlock(4, OPEN_Y, 5), harness.createBlock(5, OPEN_Y, 5)));
        assertFalse(isExtensionCancelled(harness.createBlock(5, OPEN_Y + 1, 5), BlockFace.DOWN,
                harness.createBlock(5, OPEN_Y, 5)), "down onto the floor of the arena");
    }

    @Test
    void keepsAPistonFromPushingABlockOutOfTheFightArea() {
        assertTrue(isExtensionCancelled(harness.createBlock(8, OPEN_Y, 5), BlockFace.EAST,
                harness.createBlock(9, OPEN_Y, 5)), "into the border wall");
        assertTrue(isExtensionCancelled(harness.createBlock(5, FIRST_Y_UNDER_THE_CAPTURED_VOLUME + 2, 5),
                BlockFace.DOWN, harness.createBlock(5, FIRST_Y_UNDER_THE_CAPTURED_VOLUME + 1, 5)),
                "and into the ground under the captured volume");
        assertTrue(isExtensionCancelled(harness.createBlock(TRIBUNE_X, OPEN_Y, TRIBUNE_Z), BlockFace.EAST,
                harness.createBlock(TRIBUNE_X + 1, OPEN_Y, TRIBUNE_Z)), "and on the tribune");
    }

    @Test
    void keepsAPistonHeadFromReachingIntoTheProtectedArea() {
        assertTrue(isExtensionCancelled(harness.createBlock(9, OPEN_Y, 5), BlockFace.EAST));
        assertFalse(isExtensionCancelled(harness.createBlock(8, OPEN_Y, 5), BlockFace.EAST),
                "while a head that stays in the fight area extends");
    }

    @Test
    void keepsAStickyPistonFromPullingABlockOutOfTheProtectedArea() {
        Block piston = harness.createBlock(1, OPEN_Y, 5);
        Block blockBehindTheWall = harness.createBlock(WALL_X - 1, OPEN_Y, WALL_Z);

        assertTrue(isRetractionCancelled(piston, BlockFace.WEST, blockBehindTheWall),
                "with the direction the piston faces");
        assertTrue(isRetractionCancelled(piston, BlockFace.EAST, blockBehindTheWall),
                "and with the direction the pulled block moves");
    }

    @Test
    void letsAStickyPistonPullAlongTheEdgeOfTheFightArea() {
        Block piston = harness.createBlock(7, OPEN_Y, 5);
        Block pulledBlock = harness.createBlock(9, OPEN_Y, 5);

        assertFalse(isRetractionCancelled(piston, BlockFace.EAST, pulledBlock),
                "with the direction the piston faces");
        assertFalse(isRetractionCancelled(piston, BlockFace.WEST, pulledBlock),
                "and with the direction the pulled block moves");
    }

    @Test
    void keepsEntitiesFromChangingBlocksOutsideTheFightArea() {
        assertTrue(isEntityChangeCancelled(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z), "the tribune");
        assertTrue(isEntityChangeCancelled(WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z), "the border wall");
        assertTrue(isEntityChangeCancelled(ARENA_FLOOR_X, FIRST_Y_UNDER_THE_CAPTURED_VOLUME, ARENA_FLOOR_Z),
                "the ground under the captured volume");
        assertFalse(isEntityChangeCancelled(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z),
                "while inside the fight area the cleaning restores what changes");
        assertFalse(isEntityChangeCancelled(OPEN_X, OPEN_Y, OPEN_Z));
    }

    @Test
    void keepsLavaAndWaterFromFlowingOverTheWallOutOfTheFightArea() {
        assertTrue(isFlowCancelled(Material.LAVA, LAST_X_INSIDE_THE_WALL, OVER_THE_WALL_Y, WALL_Z,
                ArenaMatchHarness.WALL_MAXIMUM, OVER_THE_WALL_Y, WALL_Z), "lava onto the top of the wall");
        assertTrue(isFlowCancelled(Material.WATER, 1, OVER_THE_WALL_Y, WALL_Z,
                WALL_X, OVER_THE_WALL_Y, WALL_Z), "and water on the other side");
    }

    @Test
    void letsALiquidFlowInsideTheFightArea() {
        assertFalse(isFlowCancelled(Material.LAVA, ARENA_FLOOR_X, OPEN_Y, ARENA_FLOOR_Z,
                ARENA_FLOOR_X + 1, OPEN_Y, ARENA_FLOOR_Z), "sideways");
        assertFalse(isFlowCancelled(Material.WATER, ARENA_FLOOR_X, OPEN_Y + 2, ARENA_FLOOR_Z,
                ARENA_FLOOR_X, OPEN_Y + 1, ARENA_FLOOR_Z), "and down");
    }

    @Test
    void letsALiquidFlowOutsideTheFightAreaAndIntoIt() {
        assertFalse(isFlowCancelled(Material.LAVA, TRIBUNE_X, OPEN_Y, TRIBUNE_Z,
                TRIBUNE_X + 1, OPEN_Y, TRIBUNE_Z), "on the tribune");
        assertFalse(isFlowCancelled(Material.WATER, ArenaMatchHarness.WALL_MAXIMUM, OVER_THE_WALL_Y, WALL_Z,
                LAST_X_INSIDE_THE_WALL, OVER_THE_WALL_Y, WALL_Z), "and from the wall into the fight area");
    }

    @Test
    void letsEveryLiquidFlowWhileNoFightAreaIsCalculated() {
        harness.getArenaRegistry().getArena().setShape(null);

        assertFalse(isFlowCancelled(Material.LAVA, LAST_X_INSIDE_THE_WALL, OVER_THE_WALL_Y, WALL_Z,
                ArenaMatchHarness.WALL_MAXIMUM, OVER_THE_WALL_Y, WALL_Z));
    }

    @Test
    void letsALiquidFlowInAnotherWorld() {
        BlockFromToEvent event = new BlockFromToEvent(harness.createBlockInTheLobby(LAST_X_INSIDE_THE_WALL,
                OVER_THE_WALL_Y, WALL_Z), harness.createBlockInTheLobby(ArenaMatchHarness.WALL_MAXIMUM,
                OVER_THE_WALL_Y, WALL_Z));

        protectionListener.onBlockFromTo(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void letsADragonEggTeleportOutOfTheFightArea() {
        assertFalse(isFlowCancelled(Material.DRAGON_EGG, LAST_X_INSIDE_THE_WALL, OVER_THE_WALL_Y, WALL_Z,
                ArenaMatchHarness.WALL_MAXIMUM, OVER_THE_WALL_Y, WALL_Z));
    }

    private boolean isFlowCancelled(Material material, int sourceX, int sourceY, int sourceZ,
                                    int destinationX, int destinationY, int destinationZ) {
        harness.setBlock(sourceX, sourceY, sourceZ, material);
        BlockFromToEvent event = new BlockFromToEvent(harness.createBlock(sourceX, sourceY, sourceZ),
                harness.createBlock(destinationX, destinationY, destinationZ));
        protectionListener.onBlockFromTo(event);
        return event.isCancelled();
    }

    private List<Block> explodeAnEntity(Block... destroyedBlocks) {
        EntityExplodeEvent event = new EntityExplodeEvent(createEntity(), harness.at(ARENA_FLOOR_X, ARENA_FLOOR_Z),
                new ArrayList<>(List.of(destroyedBlocks)), 1.0F, ExplosionResult.DESTROY);
        protectionListener.onEntityExplode(event);
        assertFalse(event.isCancelled(), "the explosion itself still goes off");
        return event.blockList();
    }

    private List<Block> explodeABlock(Block explodedBlock, Block... destroyedBlocks) {
        BlockState explodedBlockState = ArenaMatchHarness.proxy(BlockState.class, (method, arguments) -> {
            throw new UnsupportedOperationException("BlockState." + method.getName());
        });
        BlockExplodeEvent event = new BlockExplodeEvent(explodedBlock, explodedBlockState,
                new ArrayList<>(List.of(destroyedBlocks)), 1.0F, ExplosionResult.DESTROY);
        protectionListener.onBlockExplode(event);
        assertFalse(event.isCancelled(), "the explosion itself still goes off");
        return event.blockList();
    }

    private boolean isBurnCancelled(int blockX, int blockY, int blockZ) {
        BlockBurnEvent event = new BlockBurnEvent(harness.createBlock(blockX, blockY, blockZ),
                harness.createBlock(blockX, blockY + 1, blockZ));
        protectionListener.onBlockBurn(event);
        return event.isCancelled();
    }

    private boolean isSpreadCancelled(int blockX, int blockY, int blockZ) {
        BlockIgniteEvent event = new BlockIgniteEvent(harness.createBlock(blockX, blockY, blockZ),
                BlockIgniteEvent.IgniteCause.SPREAD, harness.createBlock(blockX + 1, blockY, blockZ));
        protectionListener.onBlockIgnite(event);
        return event.isCancelled();
    }

    private boolean isIgnitionByPlayerCancelled(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                                int blockZ) {
        BlockIgniteEvent event = new BlockIgniteEvent(harness.createBlock(blockX, blockY, blockZ),
                BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, player.player());
        protectionListener.onBlockIgnite(event);
        return event.isCancelled();
    }

    private boolean isIgnitionByEntityCancelled(int blockX, int blockY, int blockZ) {
        BlockIgniteEvent event = new BlockIgniteEvent(harness.createBlock(blockX, blockY, blockZ),
                BlockIgniteEvent.IgniteCause.ARROW, createEntity());
        protectionListener.onBlockIgnite(event);
        return event.isCancelled();
    }

    private boolean isExtensionCancelled(Block piston, BlockFace direction, Block... movedBlocks) {
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(movedBlocks), direction);
        protectionListener.onBlockPistonExtend(event);
        return event.isCancelled();
    }

    private boolean isRetractionCancelled(Block piston, BlockFace direction, Block... pulledBlocks) {
        BlockPistonRetractEvent event = new BlockPistonRetractEvent(piston, List.of(pulledBlocks), direction);
        protectionListener.onBlockPistonRetract(event);
        return event.isCancelled();
    }

    private boolean isEntityChangeCancelled(int blockX, int blockY, int blockZ) {
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(createEntity(),
                harness.createBlock(blockX, blockY, blockZ), FakeBlockData.of(Material.AIR));
        protectionListener.onEntityChangeBlock(event);
        return event.isCancelled();
    }

    private static Entity createEntity() {
        return ArenaMatchHarness.proxy(Entity.class, (method, arguments) -> {
            throw new UnsupportedOperationException("Entity." + method.getName());
        });
    }
}
