package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaBreakProtectionTest {

    private static final int ARENA_FLOOR_X = 5;
    private static final int ARENA_FLOOR_Z = 5;
    private static final int POND_X = 3;
    private static final int POND_Z = 3;
    private static final int OPEN_X = 7;
    private static final int OPEN_Z = 7;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaBaselineService baselineService;
    private ArenaBreakListener breakListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        harness.setBlock(POND_X, ArenaMatchHarness.GROUND_Y, POND_Z, Material.WATER);
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        baselineService = new ArenaBaselineService(harness.getPlugin(), new ArenaBaselineRegistry());
        captureBaseline();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        breakListener = new ArenaBreakListener(harness.getMatchService(), harness.getArenaRegistry(),
                baselineService);
    }

    @Test
    void takesABaselineOfTheWholeCalculatedVolume() {
        assertTrue(baselineService.findSnapshot(harness.getShape()).isPresent());
        assertTrue(harness.getShape().containsBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z),
                "the floor of the arena");
        assertTrue(harness.getShape().containsBlock(OPEN_X, ArenaMatchHarness.WALL_TOP_Y, OPEN_Z),
                "and the air up to the top of the border wall");
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()),
                "the match starter really made him an active fighter");
    }

    @Test
    void keepsAFighterFromBreakingTheArenaItself() {
        assertTrue(isBreakCancelledFor(fighter, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
    }

    @Test
    void letsEveryFighterBreakWhatWasPlacedDuringTheMatch() {
        placeDuringTheMatch(OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z, Material.OAK_PLANKS);

        assertFalse(isBreakCancelledFor(fighter, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "the fighter who built it");
        assertFalse(isBreakCancelledFor(opponent, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "and the opponent it was built against");
    }

    @Test
    void letsAFighterBreakWhatWasBuiltAboveTheBorderWall() {
        placeDuringTheMatch(OPEN_X, ArenaMatchHarness.WALL_TOP_Y + 2, OPEN_Z, Material.OAK_PLANKS);

        assertFalse(isBreakCancelledFor(fighter, OPEN_X, ArenaMatchHarness.WALL_TOP_Y + 2, OPEN_Z));
    }

    @Test
    void letsAFighterBreakABlockThatReplacedOneOfTheArena() {
        placeDuringTheMatch(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z, Material.SAND);

        assertFalse(isBreakCancelledFor(fighter, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
    }

    @Test
    void letsAnAdminBreakTheArenaItself() {
        spectator.makeOperator();

        assertFalse(isBreakCancelledFor(spectator, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
    }

    @Test
    void keepsEverybodyWhoIsNotFightingOutOfTheFightArea() {
        placeDuringTheMatch(OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z, Material.OAK_PLANKS);

        assertTrue(isBreakCancelledFor(spectator, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
        assertTrue(isBreakCancelledFor(spectator, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
    }

    @Test
    void leavesEveryBreakOutsideTheFightAreaToTheWorldLock() {
        spectator.makeOperator();

        assertFalse(isBreakCancelledFor(spectator, ArenaMatchHarness.WALL_MAXIMUM + 3,
                ArenaMatchHarness.GROUND_Y, ArenaMatchHarness.WALL_MAXIMUM + 3), "the admin on the tribune");
        assertFalse(isBreakCancelledFor(spectator, ArenaMatchHarness.WALL_MINIMUM, ArenaMatchHarness.GROUND_Y,
                5), "and in the column his own border wall stands in");
        assertTrue(isBreakCancelledFor(fighter, ArenaMatchHarness.WALL_MINIMUM, ArenaMatchHarness.GROUND_Y,
                5), "while a fighter of the running round is no admin out there either");
    }

    @Test
    void refusesEveryBreakInsideTheFightAreaWithoutABaseline() {
        placeDuringTheMatch(OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z, Material.OAK_PLANKS);
        baselineService.discardSnapshot();

        assertTrue(isBreakCancelledFor(fighter, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
        assertTrue(isBreakCancelledFor(fighter, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
        spectator.makeOperator();
        assertFalse(isBreakCancelledFor(spectator, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z),
                "the admin is never locked out of his own arena");
    }

    @Test
    void letsEveryFighterScoopUpALiquidThatWasPouredDuringTheMatch() {
        placeDuringTheMatch(OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z, Material.WATER);

        assertFalse(isBucketFillCancelledFor(fighter, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
        assertFalse(isBucketFillCancelledFor(opponent, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
    }

    @Test
    void keepsAFighterFromScoopingUpTheWaterOfTheArenaItself() {
        assertTrue(isBucketFillCancelledFor(fighter, POND_X, ArenaMatchHarness.GROUND_Y, POND_Z));

        spectator.makeOperator();
        assertFalse(isBucketFillCancelledFor(spectator, POND_X, ArenaMatchHarness.GROUND_Y, POND_Z),
                "the admin who built the pond empties it again");
    }

    @Test
    void letsOnlyTheFightersPourALiquidIntoTheArena() {
        assertFalse(isBucketEmptyCancelledFor(fighter, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
        assertTrue(isBucketEmptyCancelledFor(spectator, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
        assertTrue(isBucketEmptyCancelledFor(spectator, ArenaMatchHarness.WALL_MAXIMUM + 3,
                ArenaMatchHarness.GROUND_Y, ArenaMatchHarness.WALL_MAXIMUM + 3),
                "and nobody pours a lake over the tribune either");

        spectator.makeOperator();
        assertFalse(isBucketEmptyCancelledFor(spectator, ArenaMatchHarness.WALL_MAXIMUM + 3,
                ArenaMatchHarness.GROUND_Y, ArenaMatchHarness.WALL_MAXIMUM + 3),
                "the admin fills the pond of his own tribune");
    }

    private void captureBaseline() {
        assertTrue(baselineService.startCapture(harness.getShape(), report -> { }),
                "the fight area of the harness is small enough for a baseline");
        harness.runTick();
        assertFalse(baselineService.isCapturing(), "the baseline has to be complete before the match starts");
    }

    private void placeDuringTheMatch(int blockX, int blockY, int blockZ, Material material) {
        harness.setBlock(blockX, blockY, blockZ, material);
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
}
