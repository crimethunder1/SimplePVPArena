package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.support.FakeBlockData;
import org.bukkit.ExplosionResult;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldOutsideTheEffectAreaTest {

    private static final int GROUND_Y = ArenaMatchHarness.GROUND_Y;
    private static final int OPEN_Y = GROUND_Y + 1;
    private static final int FAR_X = 300;
    private static final int FAR_Z = 5;
    private static final int TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int ARENA_FLOOR_X = 5;
    private static final int ARENA_FLOOR_Z = 5;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaBuildListener buildListener;
    private ArenaBreakListener breakListener;
    private ArenaEnvironmentProtectionListener protectionListener;
    private int edgeZ;
    private int lastXInside;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        ArenaBaselineService baselineService = new ArenaBaselineService(harness.getPlugin(),
                new ArenaBaselineRegistry());
        assertTrue(baselineService.startCapture(harness.getShape(), report -> { }));
        harness.runTick();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        buildListener = new ArenaBuildListener(harness.getMatchService(), harness.getArenaRegistry());
        breakListener = new ArenaBreakListener(harness.getMatchService(), harness.getArenaRegistry(),
                baselineService);
        protectionListener = new ArenaEnvironmentProtectionListener(harness.getArenaRegistry(), baselineService);
        findTheEdgeOfTheEffectArea();
    }

    @Test
    void reachesFromTheArenaOverTheTribuneButNotToTheFarBlocks() {
        EffectArea effectArea = harness.getArenaRegistry().findEffectArea().orElseThrow();

        assertTrue(effectArea.containsBlock(ArenaMatchHarness.WORLD_NAME, TRIBUNE_X, TRIBUNE_Z));
        assertFalse(effectArea.containsBlock(ArenaMatchHarness.WORLD_NAME, FAR_X, FAR_Z));
        assertTrue(effectArea.containsBlock(ArenaMatchHarness.WORLD_NAME, lastXInside, edgeZ));
        assertFalse(effectArea.containsBlock(ArenaMatchHarness.WORLD_NAME, lastXInside + 1, edgeZ));
    }

    @Test
    void letsEverybodyBuildAndMineOutsideOfTheEffectArea() {
        for (ArenaMatchHarness.FakePlayer player : List.of(spectator, fighter)) {
            assertFalse(isPlacementCancelledFor(player, FAR_X, OPEN_Y, FAR_Z));
            assertFalse(isBreakCancelledFor(player, FAR_X, GROUND_Y, FAR_Z));
            assertFalse(isBucketEmptyCancelledFor(player, FAR_X, OPEN_Y, FAR_Z));
            assertFalse(isBucketFillCancelledFor(player, FAR_X, GROUND_Y, FAR_Z));
        }
    }

    @Test
    void keepsTheLockOnTheLastBlockInsideAndLiftsItOnTheFirstBlockOutside() {
        assertTrue(isPlacementCancelledFor(spectator, lastXInside, OPEN_Y, edgeZ));
        assertTrue(isBreakCancelledFor(spectator, lastXInside, GROUND_Y, edgeZ));
        assertFalse(isPlacementCancelledFor(spectator, lastXInside + 1, OPEN_Y, edgeZ));
        assertFalse(isBreakCancelledFor(spectator, lastXInside + 1, GROUND_Y, edgeZ));
    }

    @Test
    void keepsTheBlocksInsideTheEffectAreaOfAnExplosionAndLeavesTheOthersToIt() {
        Block arenaFloor = harness.createBlock(ARENA_FLOOR_X, GROUND_Y, ARENA_FLOOR_Z);
        Block tribune = harness.createBlock(TRIBUNE_X, GROUND_Y, TRIBUNE_Z);
        Block lastBlockInside = harness.createBlock(lastXInside, GROUND_Y, edgeZ);
        Block firstBlockOutside = harness.createBlock(lastXInside + 1, GROUND_Y, edgeZ);
        Block farBlock = harness.createBlock(FAR_X, GROUND_Y, FAR_Z);
        Block farGround = harness.createBlock(FAR_X, GROUND_Y - 30, FAR_Z);

        assertEquals(List.of(arenaFloor, firstBlockOutside, farBlock, farGround),
                explodeAnEntity(arenaFloor, tribune, lastBlockInside, firstBlockOutside, farBlock, farGround));
        assertEquals(List.of(firstBlockOutside, farBlock),
                explodeABlock(harness.createBlock(FAR_X, OPEN_Y, FAR_Z), tribune, firstBlockOutside, farBlock));
    }

    @Test
    void leavesFireAndEntitiesOutsideOfTheEffectAreaAlone() {
        assertFalse(isBurnCancelled(FAR_X, GROUND_Y, FAR_Z));
        assertFalse(isSpreadCancelled(FAR_X, OPEN_Y, FAR_Z));
        assertFalse(isIgnitionByPlayerCancelled(spectator, FAR_X, OPEN_Y, FAR_Z));
        assertFalse(isEntityChangeCancelled(FAR_X, GROUND_Y, FAR_Z));
        assertFalse(isBurnCancelled(lastXInside + 1, GROUND_Y, edgeZ), "from the first block outside on");
        assertTrue(isBurnCancelled(lastXInside, GROUND_Y, edgeZ), "while the last block inside stays protected");
        assertTrue(isEntityChangeCancelled(lastXInside, GROUND_Y, edgeZ));
        assertTrue(isIgnitionByPlayerCancelled(spectator, TRIBUNE_X, OPEN_Y, TRIBUNE_Z));
    }

    @Test
    void letsAPistonWorkOutsideOfTheEffectAreaButNotPushIntoIt() {
        assertFalse(isExtensionCancelled(harness.createBlock(FAR_X, OPEN_Y, FAR_Z), BlockFace.EAST,
                harness.createBlock(FAR_X + 1, OPEN_Y, FAR_Z)));
        assertTrue(isExtensionCancelled(harness.createBlock(lastXInside + 2, OPEN_Y, edgeZ), BlockFace.WEST,
                harness.createBlock(lastXInside + 1, OPEN_Y, edgeZ)), "a block pushed over the edge into the area");
    }

    @Test
    void movesTheEdgeWithTheMargin() {
        harness.getArenaRegistry().setEffectMargin(0);

        assertFalse(isPlacementCancelledFor(spectator, TRIBUNE_X, OPEN_Y, TRIBUNE_Z),
                "without a margin the tribune of the harness lies outside of the area");
        assertFalse(isBurnCancelled(TRIBUNE_X, GROUND_Y, TRIBUNE_Z));
        assertTrue(isPlacementCancelledFor(spectator, ArenaMatchHarness.WALL_MINIMUM, OPEN_Y, ARENA_FLOOR_Z),
                "while the border wall still lies inside it");
    }

    private void findTheEdgeOfTheEffectArea() {
        EffectArea effectArea = harness.getArenaRegistry().findEffectArea().orElseThrow();
        edgeZ = (int) Math.floor(effectArea.getCenter().z());
        int blockX = (int) Math.floor(effectArea.getCenter().x());
        while (effectArea.containsBlock(ArenaMatchHarness.WORLD_NAME, blockX + 1, edgeZ)) {
            blockX++;
        }
        lastXInside = blockX;
    }

    private boolean isPlacementCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                            int blockZ) {
        Block placedBlock = harness.createBlock(blockX, blockY, blockZ);
        BlockPlaceEvent event = new BlockPlaceEvent(placedBlock, null, placedBlock, null, player.player(),
                true, EquipmentSlot.HAND);
        buildListener.onBlockPlace(event);
        return event.isCancelled();
    }

    private boolean isBreakCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY, int blockZ) {
        BlockBreakEvent event = new BlockBreakEvent(harness.createBlock(blockX, blockY, blockZ), player.player());
        breakListener.onBlockBreak(event);
        return event.isCancelled();
    }

    private boolean isBucketFillCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                             int blockZ) {
        PlayerBucketFillEvent event = new PlayerBucketFillEvent(player.player(),
                harness.createBlock(blockX, blockY, blockZ), harness.createBlock(blockX, blockY - 1, blockZ),
                BlockFace.UP, Material.BUCKET, null, EquipmentSlot.HAND);
        breakListener.onPlayerBucketFill(event);
        return event.isCancelled();
    }

    private boolean isBucketEmptyCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                              int blockZ) {
        PlayerBucketEmptyEvent event = new PlayerBucketEmptyEvent(player.player(),
                harness.createBlock(blockX, blockY, blockZ), harness.createBlock(blockX, blockY - 1, blockZ),
                BlockFace.UP, Material.WATER_BUCKET, null, EquipmentSlot.HAND);
        breakListener.onPlayerBucketEmpty(event);
        return event.isCancelled();
    }

    private List<Block> explodeAnEntity(Block... destroyedBlocks) {
        EntityExplodeEvent event = new EntityExplodeEvent(createEntity(), harness.at(FAR_X, FAR_Z),
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

    private boolean isExtensionCancelled(Block piston, BlockFace direction, Block... movedBlocks) {
        BlockPistonExtendEvent event = new BlockPistonExtendEvent(piston, List.of(movedBlocks), direction);
        protectionListener.onBlockPistonExtend(event);
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
