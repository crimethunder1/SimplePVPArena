package de.simplepvparena.registration;

import de.simplepvparena.listener.RegistrationStationListener;
import de.simplepvparena.support.RecordingItemStack;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrationStationListenerTest {

    private static final int HAND_SLOT = 0;
    private static final int OTHER_SLOT = 7;

    private final StationTestServer server = new StationTestServer();
    private final RegistrationStationListener listener = new RegistrationStationListener(server.stationService());
    private final StationTestServer.TestPlayer admin = server.joinPlayer("Admin", true);
    private final StationTestServer.TestPlayer steve = server.joinPlayer("Steve", false);
    private final Block button = server.placeBlock(10, 64, 20, Material.STONE_BUTTON);

    @Test
    void anAdminMakesAButtonAStationWithTheTool() {
        PlayerInteractEvent event = rightClick(admin, server.createTool(), button);

        assertTrue(server.stations().isStation(RegistrationStation.of(button)));
        assertEquals(List.of("This button is now a registration station"), admin.messages());
        assertEquals(Event.Result.DENY, event.useInteractedBlock(), "the button is not pressed by the tool");
        assertEquals(0, server.registrationList().getCount());
    }

    @Test
    void anAdminMakesAStationANormalButtonAgainWithTheTool() {
        rightClick(admin, server.createTool(), button);

        rightClick(admin, server.createTool(), button);

        assertFalse(server.stations().isStation(RegistrationStation.of(button)));
        assertEquals("This button is no longer a registration station", admin.messages().get(1));
    }

    @Test
    void anAdminIsAskedForAButtonWhenTheToolClicksSomethingElse() {
        Block stone = server.placeBlock(11, 64, 20, Material.STONE);

        rightClick(admin, server.createTool(), stone);
        PlayerInteractEvent airClick = interact(admin, Action.RIGHT_CLICK_AIR, server.createTool(), null,
                EquipmentSlot.HAND);

        assertEquals(List.of(), server.stations().stations());
        assertEquals(List.of("Click a button", "Click a button"), admin.messages());
        assertEquals(Event.Result.DENY, airClick.useItemInHand());
    }

    @Test
    void theToolDoesNothingInTheOffHand() {
        interact(admin, Action.RIGHT_CLICK_BLOCK, server.createTool(), button, EquipmentSlot.OFF_HAND);

        assertEquals(List.of(), server.stations().stations());
        assertEquals(List.of(), admin.messages());
    }

    @Test
    void theToolDisappearsFromThePlayerWhoIsNoAdmin() {
        ItemStack tool = server.createTool();
        ItemStack otherTool = server.createTool();
        RecordingItemStack sword = new RecordingItemStack(Material.IRON_SWORD, 1);
        steve.putIntoSlot(HAND_SLOT, tool);
        steve.putIntoSlot(OTHER_SLOT, otherTool);
        steve.putIntoSlot(1, sword);

        PlayerInteractEvent event = rightClick(steve, tool, button);

        assertEquals(Event.Result.DENY, event.useInteractedBlock());
        assertEquals(List.of(), server.stations().stations());
        assertNull(steve.itemInSlot(HAND_SLOT));
        assertNull(steve.itemInSlot(OTHER_SLOT));
        assertEquals(sword, steve.itemInSlot(1));
        assertEquals(List.of(), steve.messages());
        assertEquals(0, server.registrationList().getCount());
    }

    @Test
    void theToolDisappearsFromThePlayerWhoIsNoAdminOnALeftClick() {
        ItemStack tool = server.createTool();
        steve.putIntoSlot(HAND_SLOT, tool);

        interact(steve, Action.LEFT_CLICK_AIR, tool, null, EquipmentSlot.HAND);

        assertNull(steve.itemInSlot(HAND_SLOT));
    }

    @Test
    void aClickOnTheStationRegistersThePlayerAndPressesTheButton() {
        rightClick(admin, server.createTool(), button);

        PlayerInteractEvent event = rightClick(steve, null, button);

        assertTrue(server.registrationList().isRegistered(steve.uniqueId()));
        assertEquals(List.of("You are registered for the next event"), steve.messages());
        assertEquals(Event.Result.ALLOW, event.useInteractedBlock());
    }

    @Test
    void aClickWithAnItemInTheHandRegistersThePlayerToo() {
        rightClick(admin, server.createTool(), button);

        rightClick(steve, new RecordingItemStack(Material.BREAD, 1), button);

        assertTrue(server.registrationList().isRegistered(steve.uniqueId()));
    }

    @Test
    void aSecondClickTellsThePlayerHowToLeave() {
        rightClick(admin, server.createTool(), button);
        rightClick(steve, null, button);

        rightClick(steve, null, button);

        assertEquals(1, server.registrationList().getCount());
        assertEquals("You are already registered, sneak and click the station or type /tournament leave to leave",
                steve.messages().get(1));
    }

    @Test
    void aClickWhileSneakingUnregistersThePlayer() {
        rightClick(admin, server.createTool(), button);
        rightClick(steve, null, button);
        steve.setSneaking(true);

        PlayerInteractEvent event = rightClick(steve, null, button);

        assertFalse(server.registrationList().isRegistered(steve.uniqueId()));
        assertEquals("You are no longer registered", steve.messages().get(1));
        assertEquals(Event.Result.ALLOW, event.useInteractedBlock());
    }

    @Test
    void aClickWhileSneakingTellsAPlayerWhoIsNotRegistered() {
        rightClick(admin, server.createTool(), button);
        steve.setSneaking(true);

        rightClick(steve, null, button);

        assertEquals(List.of("You are not registered"), steve.messages());
    }

    @Test
    void anAdminRegistersLikeEveryOtherPlayer() {
        rightClick(admin, server.createTool(), button);

        rightClick(admin, null, button);

        assertTrue(server.registrationList().isRegistered(admin.uniqueId()));
    }

    @Test
    void theStationIsClosedWhileATournamentIsInProgress() {
        rightClick(admin, server.createTool(), button);
        server.startTournament();

        rightClick(steve, null, button);

        assertEquals(0, server.registrationList().getCount());
        assertEquals(List.of("Registration is closed while a tournament is in progress"), steve.messages());
    }

    @Test
    void ignoresANormalButtonALeftClickAndTheOffHand() {
        Block normalButton = server.placeBlock(12, 64, 20, Material.OAK_BUTTON);
        rightClick(admin, server.createTool(), button);

        rightClick(steve, null, normalButton);
        interact(steve, Action.LEFT_CLICK_BLOCK, null, button, EquipmentSlot.HAND);
        interact(steve, Action.RIGHT_CLICK_BLOCK, null, button, EquipmentSlot.OFF_HAND);

        assertEquals(0, server.registrationList().getCount());
        assertEquals(List.of(), steve.messages());
    }

    @Test
    void ignoresAStationWhoseBlockIsNoButtonAnyMore() {
        rightClick(admin, server.createTool(), button);
        Block replacedBlock = server.placeBlock(10, 64, 20, Material.STONE);

        rightClick(steve, null, replacedBlock);

        assertEquals(0, server.registrationList().getCount());
    }

    @Test
    void aPlayerWhoIsNoAdminCannotBreakAStation() {
        rightClick(admin, server.createTool(), button);
        BlockBreakEvent event = new BlockBreakEvent(button, steve.player());

        breakBlock(event);

        assertTrue(event.isCancelled());
        assertTrue(server.stations().isStation(RegistrationStation.of(button)));
        assertEquals(List.of("This is a registration station"), steve.actionBarTexts());
    }

    @Test
    void anAdminWhoBreaksAStationRemovesIt() {
        rightClick(admin, server.createTool(), button);
        BlockBreakEvent event = new BlockBreakEvent(button, admin.player());

        breakBlock(event);

        assertFalse(event.isCancelled());
        assertFalse(server.stations().isStation(RegistrationStation.of(button)));
        assertEquals("This button is no longer a registration station", admin.messages().get(1));
    }

    @Test
    void aPlayerMayBreakANormalButton() {
        BlockBreakEvent event = new BlockBreakEvent(button, steve.player());

        breakBlock(event);

        assertFalse(event.isCancelled());
        assertEquals(List.of(), steve.actionBarTexts());
    }

    private void breakBlock(BlockBreakEvent event) {
        listener.onBlockBreak(event);
        if (!event.isCancelled()) {
            listener.onStationBroken(event);
        }
    }

    private PlayerInteractEvent rightClick(StationTestServer.TestPlayer player, ItemStack item, Block block) {
        return interact(player, Action.RIGHT_CLICK_BLOCK, item, block, EquipmentSlot.HAND);
    }

    private PlayerInteractEvent interact(StationTestServer.TestPlayer player, Action action, ItemStack item,
                                         Block block, EquipmentSlot hand) {
        PlayerInteractEvent event = new PlayerInteractEvent(player.player(), action, item, block, BlockFace.UP, hand);
        listener.onPlayerInteract(event);
        return event;
    }
}
