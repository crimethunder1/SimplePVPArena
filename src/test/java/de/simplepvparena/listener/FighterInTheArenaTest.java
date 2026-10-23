package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FighterInTheArenaTest {

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer spectator;
    private PlayerMoveListener moveListener;
    private SpectatorBorderListener borderListener;
    private ArenaBuildListener buildListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        moveListener = new PlayerMoveListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), harness.getPlugin().getLogger());
        borderListener = new SpectatorBorderListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getBorderService());
        buildListener = new ArenaBuildListener(harness.getMatchService(), harness.getArenaRegistry());
    }

    @Test
    void buildsAWallThatEnclosesTheCalculatedFightArea() {
        assertTrue(harness.getShape().containsCell(1, 5), "the cell inside the wall");
        assertFalse(harness.getShape().containsCell(ArenaMatchHarness.WALL_MINIMUM, 5),
                "and the column the wall stands in");
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()),
                "the match starter really made the player an active fighter");
    }

    @Test
    void neverStopsAnActiveFighterAtTheBorderWall() {
        assertFalse(isMovementCancelled(harness.at(1.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "stepping onto the wall");
        assertFalse(isMovementCancelled(harness.at(0.5D, 5.5D), harness.at(-0.5D, 5.5D)),
                "and past it");
        assertFalse(isMovementCancelled(harness.at(-0.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "and back onto it");
        assertFalse(isMovementCancelled(harness.at(0.5D, 5.5D), harness.at(1.5D, 5.5D)),
                "and back into the fight area");
    }

    @Test
    void neverStopsAnActiveFighterAboveTheBorderWall() {
        Location insideHigh = new Location(harness.getWorld(), 1.5D, ArenaMatchHarness.GROUND_Y + 6, 5.5D);
        Location aboveTheWall = new Location(harness.getWorld(), 0.5D, ArenaMatchHarness.GROUND_Y + 6, 5.5D);

        assertFalse(isMovementCancelled(insideHigh, aboveTheWall));
        assertFalse(isMovementCancelled(aboveTheWall, insideHigh));
    }

    @Test
    void letsAnActiveFighterWalkThroughTheFightArea() {
        assertFalse(isMovementCancelled(harness.at(2.5D, 2.5D), harness.at(3.5D, 2.5D)));
        assertFalse(isMovementCancelled(harness.at(3.5D, 2.5D), harness.at(3.5D, 3.5D)));
    }

    @Test
    void keepsStoppingASpectatorAtTheBorderWall() {
        assertTrue(isMovementCancelledFor(spectator, harness.at(-0.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "walking into the wall from the tribune");
        assertTrue(isMovementCancelledFor(spectator, harness.at(11.5D, 5.5D), harness.at(10.5D, 5.5D)),
                "and from the other side of it");
    }

    @Test
    void tellsAFlyingSpectatorFromAFighter() {
        harness.getSpectatorService().enterArenaFlight(spectator.player());

        assertEquals(SpectatorMode.ARENA_FLIGHT,
                harness.getSpectatorService().findSpectatorMode(spectator.uniqueId()).orElseThrow());
        assertFalse(harness.getMatchService().isActiveFighter(spectator.uniqueId()));
        assertTrue(harness.getSpectatorService().findSpectatorMode(fighter.uniqueId()).isEmpty(),
                "and the fighter is in no spectator mode at all");
    }

    @Test
    void leavesAnActiveFighterInTheSurvivalModeWithoutASpectatorState() {
        assertEquals(GameMode.SURVIVAL, fighter.gameMode());
        assertTrue(harness.getSpectatorService().findSpectatorMode(fighter.uniqueId()).isEmpty());
    }

    @Test
    void letsAnActiveFighterBuildInsideTheArena() {
        assertFalse(isPlacementCancelledFor(fighter, 5, ArenaMatchHarness.GROUND_Y + 1, 5),
                "in the middle of the fight area");
        assertFalse(isPlacementCancelledFor(fighter, 1, ArenaMatchHarness.GROUND_Y + 4, 1),
                "and high up in a corner of it");
    }

    @Test
    void keepsSpectatorsFromBuildingInsideTheArena() {
        assertTrue(isPlacementCancelledFor(spectator, 5, ArenaMatchHarness.GROUND_Y + 1, 5));
    }

    @Test
    void leavesEveryPlacementOutsideTheFightAreaToTheWorldLock() {
        spectator.makeOperator();

        assertFalse(isPlacementCancelledFor(spectator, ArenaMatchHarness.WALL_MAXIMUM + 3,
                ArenaMatchHarness.GROUND_Y, ArenaMatchHarness.WALL_MAXIMUM + 3), "the admin on the tribune");
        assertFalse(isPlacementCancelledFor(spectator, ArenaMatchHarness.WALL_MINIMUM, ArenaMatchHarness.GROUND_Y,
                5), "and in the column the admin's own border wall stands in");
        assertTrue(isPlacementCancelledFor(fighter, ArenaMatchHarness.WALL_MAXIMUM + 3,
                ArenaMatchHarness.GROUND_Y, ArenaMatchHarness.WALL_MAXIMUM + 3),
                "while a fighter of the running round is no admin out there either");
    }

    @Test
    void letsAnAdminBuildInsideTheArena() {
        spectator.makeOperator();

        assertFalse(isPlacementCancelledFor(spectator, 5, ArenaMatchHarness.GROUND_Y + 1, 5));
    }

    private boolean isMovementCancelled(Location origin, Location destination) {
        return isMovementCancelledFor(fighter, origin, destination);
    }

    private boolean isPlacementCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY, int blockZ) {
        BlockPlaceEvent event = new BlockPlaceEvent(harness.createBlock(blockX, blockY, blockZ), null,
                harness.createBlock(blockX, blockY - 1, blockZ), null, player.player(), true, EquipmentSlot.HAND);
        buildListener.onBlockPlace(event);
        return event.isCancelled();
    }

    private boolean isMovementCancelledFor(ArenaMatchHarness.FakePlayer player, Location origin,
                                           Location destination) {
        player.moveTo(origin);
        PlayerMoveEvent event = new PlayerMoveEvent(player.player(), origin, destination);
        borderListener.onPlayerMove(event);
        if (!event.isCancelled()) {
            moveListener.onPlayerMove(event);
        }
        return event.isCancelled();
    }
}
