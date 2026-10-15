package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import org.bukkit.Location;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPermissionInTheArenaTest {

    private static final int ARENA_FLOOR_X = 5;
    private static final int ARENA_FLOOR_Z = 5;
    private static final int OPEN_X = 7;
    private static final int OPEN_Z = 7;
    private static final int TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 3;
    private static final int TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 3;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer permittedAdmin;
    private ArenaMatchHarness.FakePlayer operator;
    private ArenaMatchHarness.FakePlayer ordinaryPlayer;
    private PlayerMoveListener moveListener;
    private SpectatorBorderListener borderListener;
    private ArenaBuildListener buildListener;
    private ArenaBreakListener breakListener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        permittedAdmin = harness.addPlayer("PermittedAdmin");
        permittedAdmin.grantAdminPermission();
        operator = harness.addPlayer("Operator");
        operator.makeOperator();
        ordinaryPlayer = harness.addPlayer("OrdinaryPlayer");
        ArenaBaselineService baselineService = new ArenaBaselineService(harness.getPlugin(),
                new ArenaBaselineRegistry());
        assertTrue(baselineService.startCapture(harness.getShape(), report -> { }));
        harness.runTick();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        moveListener = new PlayerMoveListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry());
        borderListener = new SpectatorBorderListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getBorderService());
        buildListener = new ArenaBuildListener(harness.getMatchService(), harness.getArenaRegistry());
        breakListener = new ArenaBreakListener(harness.getMatchService(), harness.getArenaRegistry(),
                baselineService);
    }

    @Test
    void neverStopsAnAdminWithThePermissionButWithoutOperatorStatus() {
        assertFalse(permittedAdmin.player().isOp(), "the admin is no operator");

        assertFalse(isMovementCancelledFor(permittedAdmin, harness.at(-0.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "walking onto the border wall");
        assertFalse(isMovementCancelledFor(permittedAdmin, harness.at(0.5D, 5.5D), harness.at(1.5D, 5.5D)),
                "and over the edge into the arena");
        assertFalse(isMovementCancelledFor(permittedAdmin, harness.at(1.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "and back out of it");
        assertFalse(isTeleportCancelledFor(permittedAdmin, harness.at(TRIBUNE_X + 0.5D, TRIBUNE_Z + 0.5D),
                harness.at(5.5D, 5.5D)), "and a teleport into the arena");
    }

    @Test
    void letsAnAdminWithThePermissionButWithoutOperatorStatusBuildAndBreak() {
        assertFalse(isPlacementCancelledFor(permittedAdmin, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "inside the fight area while a match runs");
        assertFalse(isBreakCancelledFor(permittedAdmin, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z),
                "the arena itself");
        assertFalse(isPlacementCancelledFor(permittedAdmin, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z),
                "and on the tribune");
        assertFalse(isBreakCancelledFor(permittedAdmin, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
    }

    @Test
    void stopsAPlayerWithoutThePermissionAndWithoutOperatorStatus() {
        assertTrue(isMovementCancelledFor(ordinaryPlayer, harness.at(-0.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "at the border wall");
        assertTrue(isMovementCancelledFor(ordinaryPlayer, harness.at(0.5D, 5.5D), harness.at(1.5D, 5.5D)),
                "and at the edge of the arena");
        assertTrue(isTeleportCancelledFor(ordinaryPlayer, harness.at(TRIBUNE_X + 0.5D, TRIBUNE_Z + 0.5D),
                harness.at(5.5D, 5.5D)), "and on a teleport into the arena");
    }

    @Test
    void keepsAPlayerWithoutThePermissionAndWithoutOperatorStatusFromBuilding() {
        assertTrue(isPlacementCancelledFor(ordinaryPlayer, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z),
                "inside the fight area");
        assertTrue(isBreakCancelledFor(ordinaryPlayer, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
        assertTrue(isPlacementCancelledFor(ordinaryPlayer, TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z),
                "and on the tribune");
        assertTrue(isBreakCancelledFor(ordinaryPlayer, TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z));
    }

    @Test
    void treatsAnOperatorWithoutAnExplicitPermissionAsAnAdmin() {
        assertFalse(isMovementCancelledFor(operator, harness.at(-0.5D, 5.5D), harness.at(0.5D, 5.5D)),
                "at the border wall");
        assertFalse(isMovementCancelledFor(operator, harness.at(0.5D, 5.5D), harness.at(1.5D, 5.5D)),
                "and at the edge of the arena");
        assertFalse(isTeleportCancelledFor(operator, harness.at(TRIBUNE_X + 0.5D, TRIBUNE_Z + 0.5D),
                harness.at(5.5D, 5.5D)), "and on a teleport into the arena");
        assertFalse(isPlacementCancelledFor(operator, OPEN_X, ArenaMatchHarness.GROUND_Y + 1, OPEN_Z));
        assertFalse(isBreakCancelledFor(operator, ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z));
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

    private boolean isTeleportCancelledFor(ArenaMatchHarness.FakePlayer player, Location origin,
                                           Location destination) {
        player.moveTo(origin);
        PlayerTeleportEvent event = new PlayerTeleportEvent(player.player(), origin, destination,
                PlayerTeleportEvent.TeleportCause.COMMAND);
        moveListener.onPlayerTeleport(event);
        return event.isCancelled();
    }

    private boolean isPlacementCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY,
                                            int blockZ) {
        BlockPlaceEvent event = new BlockPlaceEvent(harness.createBlock(blockX, blockY, blockZ), null,
                harness.createBlock(blockX, blockY - 1, blockZ), null, player.player(), true, EquipmentSlot.HAND);
        buildListener.onBlockPlace(event);
        return event.isCancelled();
    }

    private boolean isBreakCancelledFor(ArenaMatchHarness.FakePlayer player, int blockX, int blockY, int blockZ) {
        BlockBreakEvent event = new BlockBreakEvent(harness.createBlock(blockX, blockY, blockZ), player.player());
        breakListener.onBlockBreak(event);
        return event.isCancelled();
    }
}
