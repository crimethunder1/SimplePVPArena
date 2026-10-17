package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldOutsideTheArenaTest {

    private static final int ARENA_FLOOR_X = 5;
    private static final int ARENA_FLOOR_Z = 5;
    private static final int OPEN_X = 7;
    private static final int OPEN_Z = 7;
    private static final int TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int WALL_X = ArenaMatchHarness.WALL_MINIMUM;
    private static final int WALL_Z = 5;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaMatchHarness.FakePlayer admin;
    private ArenaBuildListener buildListener;
    private ArenaBreakListener breakListener;
    private SpectatorBorderListener borderListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        harness.setBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z, Material.STONE);
        harness.setBlock(WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z, Material.STONE);
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        admin = harness.addPlayer("Admin");
        admin.makeOperator();
        ArenaBaselineService baselineService = new ArenaBaselineService(harness.getPlugin(),
                new ArenaBaselineRegistry());
        assertTrue(baselineService.startCapture(harness.getShape(), report -> { }));
        harness.runTick();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        buildListener = new ArenaBuildListener(harness.getMatchService(), harness.getArenaRegistry());
        breakListener = new ArenaBreakListener(harness.getMatchService(), harness.getArenaRegistry(),
                baselineService);
        borderListener = new SpectatorBorderListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getBorderService());
    }

    @Test
    void keepsASpectatorFromBuildingAndMiningOnTheTribune() {
        assertTrue(isPlacementCancelledFor(spectator, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z));
        assertTrue(isBreakCancelledFor(spectator, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
    }

    @Test
    void keepsAFighterOfTheRunningRoundOutOfTheWorldAroundTheArena() {
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()),
                "the match starter really made the player an active fighter");

        assertTrue(isPlacementCancelledFor(fighter, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z));
        assertTrue(isBreakCancelledFor(fighter, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
    }

    @Test
    void letsAnAdminBuildAndBreakEverywhere() {
        assertFalse(isPlacementCancelledFor(admin, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z),
                "on the tribune");
        assertFalse(isBreakCancelledFor(admin, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
        assertFalse(isPlacementCancelledFor(admin, WALL_X, ArenaMatchHarness.GROUND_Y + 1, WALL_Z),
                "in the column the admin's own border wall stands in");
        assertFalse(isBreakCancelledFor(admin, WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z));
        assertFalse(isPlacementCancelledFor(admin, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "and inside the fight area, while a match runs");
        assertFalse(isBreakCancelledFor(admin, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
    }

    @Test
    void changesNothingInsideTheFightArea() {
        assertFalse(isPlacementCancelledFor(fighter, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "the fighter of the running round still builds inside the arena");
        harness.setBlock(OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z, Material.OAK_PLANKS);
        assertFalse(isBreakCancelledFor(fighter, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "and takes down again what was placed during the match");
        assertTrue(isBreakCancelledFor(fighter, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z),
                "the arena itself still stays as it is");
        assertTrue(isPlacementCancelledFor(spectator, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "and whoever is not fighting is still kept out of the fight area");
    }

    @Test
    void protectsTheBorderWallFromEverybodyButTheAdmin() {
        assertTrue(harness.getBorderService().isBorderBlock(harness.createBlock(WALL_X,
                ArenaMatchHarness.GROUND_Y, WALL_Z)), "the harness really built a marked wall there");

        assertTrue(breakThroughTheListeners(spectator, WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z));
        assertTrue(harness.getBorderService().isBorderBlock(harness.createBlock(WALL_X,
                ArenaMatchHarness.GROUND_Y, WALL_Z)),
                "a block that was never really broken keeps its mark");

        assertFalse(breakThroughTheListeners(admin, WALL_X, ArenaMatchHarness.GROUND_Y, WALL_Z));
        assertFalse(harness.getBorderService().isBorderBlock(harness.createBlock(WALL_X,
                ArenaMatchHarness.GROUND_Y, WALL_Z)),
                "and the admin who takes a piece of their wall out really takes its mark with it");
    }

    @Test
    void leavesEveryOtherWorldOfTheServerAlone() {
        assertFalse(isPlacementCancelledFor(spectator, harness.createBlockInTheLobby(0, 100, 0)));
        assertFalse(isBreakCancelledFor(spectator, harness.createBlockInTheLobby(0, 100, 0)));
    }

    @Test
    void locksNothingWhileNoFightAreaIsCalculated() {
        harness.getArenaRegistry().getArena().setShape(null);

        assertFalse(isPlacementCancelledFor(spectator, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z));
        assertFalse(isBreakCancelledFor(spectator, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
        assertFalse(isPlacementCancelledFor(spectator, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
    }

    @Test
    void coversTheBucketOutsideTheFightAreaAsWell() {
        assertTrue(isBucketEmptyCancelledFor(spectator, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z));
        assertTrue(isBucketFillCancelledFor(spectator, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
        assertTrue(isBucketEmptyCancelledFor(fighter, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z),
                "a fighter of the running round is a player on the tribune out there like any other");
        assertFalse(isBucketEmptyCancelledFor(admin, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z));
        assertFalse(isBucketFillCancelledFor(admin, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
    }

    private boolean breakThroughTheListeners(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                             int blockZ) {
        BlockBreakEvent event = new BlockBreakEvent(harness.createBlock(blockX, blockY, blockZ), player.player());
        breakListener.onBlockBreak(event);
        if (!event.isCancelled()) {
            borderListener.onBlockBreak(event);
        }
        return event.isCancelled();
    }

    private boolean isPlacementCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                            int blockZ) {
        return isPlacementCancelledFor(player, harness.createBlock(blockX, blockY, blockZ));
    }

    private boolean isPlacementCancelledFor(ArenaMatchHarness.FakePlayer player, Block placedBlock) {
        BlockPlaceEvent event = new BlockPlaceEvent(placedBlock, null, placedBlock, null, player.player(),
                true, EquipmentSlot.HAND);
        buildListener.onBlockPlace(event);
        return event.isCancelled();
    }

    private boolean isBreakCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY, int blockZ) {
        return isBreakCancelledFor(player, harness.createBlock(blockX, blockY, blockZ));
    }

    private boolean isBreakCancelledFor(ArenaMatchHarness.FakePlayer player, Block block) {
        BlockBreakEvent event = new BlockBreakEvent(block, player.player());
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
}
