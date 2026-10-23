package de.simplepvparena.arena;

import de.simplepvparena.listener.ArenaHeightMarkerListener;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaHeightMarkerPermissionTest {

    private static final int HAND_SLOT = 0;
    private static final int OTHER_MARKER_SLOT = 5;
    private static final int BREAD_SLOT = 1;
    private static final int INVENTORY_SIZE = 41;

    private final FakeServerHarness harness = new FakeServerHarness("event");
    private final ArenaHeightMarkers heightMarkers = new ArenaHeightMarkers();
    private final ArenaHeightMarkerService markerService = new ArenaHeightMarkerService(harness.getPlugin(),
            heightMarkers);
    private final ArenaHeightMarkerListener listener = new ArenaHeightMarkerListener(markerService);
    private final TestPlayer admin = new TestPlayer(true);
    private final ItemStack marker = markerService.switchMarkedEnd(admin.player,
            new RecordingItemStack(ArenaHeightMarkerService.MARKER_MATERIAL, ArenaHeightMarkerService.MARKER_AMOUNT));
    private final ItemStack bread = new RecordingItemStack(Material.BREAD, 1);

    @Test
    void aPlayerWhoIsNoAdminLosesEveryHeightMarkerOnAClickAndSwitchesNothing() {
        TestPlayer player = playerWithTwoMarkers();
        ArenaHeightMarkerMode nextEnd = heightMarkers.getNextEnd();

        PlayerInteractEvent event = new PlayerInteractEvent(player.player, Action.RIGHT_CLICK_AIR, marker, null,
                BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertEquals(Event.Result.DENY, event.useItemInHand());
        assertNull(player.contents[HAND_SLOT]);
        assertNull(player.contents[OTHER_MARKER_SLOT]);
        assertEquals(bread, player.contents[BREAD_SLOT]);
        assertEquals(nextEnd, heightMarkers.getNextEnd());
        assertEquals(List.of(), player.messages);
    }

    @Test
    void aPlayerWhoIsNoAdminLosesTheMarkersOnALeftClickAsWell() {
        TestPlayer player = playerWithTwoMarkers();

        listener.onPlayerInteract(new PlayerInteractEvent(player.player, Action.LEFT_CLICK_BLOCK, marker,
                SharedBukkitServer.proxy(Block.class, (method, arguments) -> null), BlockFace.UP,
                EquipmentSlot.HAND));

        assertNull(player.contents[HAND_SLOT]);
        assertNull(player.contents[OTHER_MARKER_SLOT]);
    }

    @Test
    void aPlayerWhoIsNoAdminMarksNoHeightByPlacingTheMarker() {
        TestPlayer player = playerWithTwoMarkers();
        Block placedBlock = SharedBukkitServer.proxy(Block.class, (method, arguments) -> {
            throw new UnsupportedOperationException("Block." + method.getName());
        });

        BlockPlaceEvent event = new BlockPlaceEvent(placedBlock, null, placedBlock, marker, player.player, true,
                EquipmentSlot.HAND);
        listener.onBlockPlace(event);

        assertTrue(event.isCancelled());
        assertTrue(heightMarkers.isEmpty());
        assertNull(player.contents[HAND_SLOT]);
        assertNull(player.contents[OTHER_MARKER_SLOT]);
        assertEquals(bread, player.contents[BREAD_SLOT]);
        assertEquals(List.of(), player.messages);
    }

    @Test
    void anAdminKeepsSwitchingTheMarkedEnd() {
        admin.contents[HAND_SLOT] = marker;
        admin.messages.clear();
        ArenaHeightMarkerMode nextEnd = heightMarkers.getNextEnd();

        PlayerInteractEvent event = new PlayerInteractEvent(admin.player, Action.RIGHT_CLICK_AIR, marker, null,
                BlockFace.UP, EquipmentSlot.HAND);
        listener.onPlayerInteract(event);

        assertEquals(Event.Result.DENY, event.useItemInHand());
        assertNotNull(admin.contents[HAND_SLOT]);
        assertEquals(1, admin.messages.size(), admin.messages.toString());
        assertNotEquals(nextEnd, heightMarkers.getNextEnd(), "the other end is marked next");
    }

    private TestPlayer playerWithTwoMarkers() {
        TestPlayer player = new TestPlayer(false);
        player.contents[HAND_SLOT] = marker;
        player.contents[OTHER_MARKER_SLOT] = marker;
        player.contents[BREAD_SLOT] = bread;
        return player;
    }

    private static final class TestPlayer {

        private final ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        private final List<String> messages = new ArrayList<>();
        private final Player player;

        private TestPlayer(boolean adminPermission) {
            UUID uniqueId = UUID.randomUUID();
            PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "getContents" -> contents.clone();
                        case "setItem" -> {
                            contents[(Integer) arguments[0]] = (ItemStack) arguments[1];
                            yield null;
                        }
                        case "setItemInMainHand" -> {
                            contents[HAND_SLOT] = (ItemStack) arguments[0];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                    });
            player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getUniqueId" -> uniqueId;
                case "isOnline" -> true;
                case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0])
                        && adminPermission;
                case "getInventory" -> inventory;
                case "sendMessage" -> {
                    messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                    yield null;
                }
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        }
    }
}
