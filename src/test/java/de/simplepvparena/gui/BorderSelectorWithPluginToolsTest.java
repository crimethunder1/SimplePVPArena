package de.simplepvparena.gui;

import de.simplepvparena.arena.StandingsBoardToolService;
import de.simplepvparena.border.BorderMarkIndex;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.listener.BorderSelectorListener;
import de.simplepvparena.listener.RegistrationStationListener;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.registration.RegistrationStationService;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderSelectorWithPluginToolsTest {

    private static final int BLOCK_X = 4;
    private static final int BLOCK_Y = 64;
    private static final int BLOCK_Z = -6;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final RegistrationStationService stationService = fixture.registrationStationService();
    private final StandingsBoardToolService standingsBoardToolService = fixture.standingsBoardToolService();
    private final World world = fixture.admin().getLocation().getWorld();
    private final BorderMarkIndex borderMarks = new BorderMarkIndex();
    private final BorderSelectorModeRegistry selectorModes = new BorderSelectorModeRegistry();
    private final BorderSelectorListener selectorListener = new BorderSelectorListener(selectorModes,
            new SpectatorBorderService(borderMarks, () -> { }),
            itemStack -> stationService.isTool(itemStack) || standingsBoardToolService.isTool(itemStack));
    private final RegistrationStationListener stationListener = new RegistrationStationListener(stationService);
    private final UUID adminUniqueId = UUID.randomUUID();
    private final List<String> messages = new ArrayList<>();
    private final List<String> actionBarTexts = new ArrayList<>();
    private final Player admin = createAdmin();
    private final Block button = createBlock(Material.STONE_BUTTON);

    BorderSelectorWithPluginToolsTest() {
        assertTrue(selectorModes.toggle(adminUniqueId), "the border selector mode is on");
    }

    @Test
    void theRegistrationStationToolSetsNoBorderMarkAndOnlyDoesItsOwnTask() {
        PlayerInteractEvent event = rightClick(stationService.createTool());

        assertEquals(0, borderMarks.getMarkedBlockCount(), "no border mark");
        assertFalse(borderMarks.isMarkedBlock(world.getName(), BLOCK_X, BLOCK_Y, BLOCK_Z));
        assertEquals(List.of(), actionBarTexts, "no report of a border mark");
        assertTrue(stationService.isStation(button), "the button is a registration station now");
        assertEquals(List.of("This button is now a registration station"), messages);
        assertTrue(event.isCancelled(), "the tool cancels the click as before");
    }

    @Test
    void theStandingsBoardToolSetsNoBorderMark() {
        PlayerInteractEvent event = new PlayerInteractEvent(admin, Action.RIGHT_CLICK_BLOCK,
                standingsBoardToolService.createTool(), button, BlockFace.UP, EquipmentSlot.HAND);

        selectorListener.onPlayerInteract(event);

        assertEquals(0, borderMarks.getMarkedBlockCount());
        assertEquals(List.of(), actionBarTexts);
        assertFalse(event.isCancelled(), "the selector leaves the click to the tool");
    }

    @Test
    void anItemThatOnlyCarriesTheNameOfTheToolStillSetsABorderMark() {
        rightClick(RecordingItemStack.named(RegistrationStationService.TOOL_MATERIAL, "Registration station tool"));

        assertTrue(borderMarks.isMarkedBlock(world.getName(), BLOCK_X, BLOCK_Y, BLOCK_Z));
        assertFalse(stationService.isStation(button));
    }

    @Test
    void withoutAToolTheClickSetsABorderMarkAsBefore() {
        PlayerInteractEvent event = rightClick(null);

        assertTrue(borderMarks.isMarkedBlock(world.getName(), BLOCK_X, BLOCK_Y, BLOCK_Z));
        assertEquals(1, borderMarks.getMarkedBlockCount());
        assertEquals(List.of("Marked as a border block: " + BLOCK_X + " " + BLOCK_Y + " " + BLOCK_Z),
                actionBarTexts);
        assertTrue(event.isCancelled());
        assertFalse(stationService.isStation(button), "the button is not pressed either");
    }

    @Test
    void withAnyOtherItemTheClickSetsABorderMarkAsBefore() {
        rightClick(new RecordingItemStack(Material.STONE, 1));

        assertTrue(borderMarks.isMarkedBlock(world.getName(), BLOCK_X, BLOCK_Y, BLOCK_Z));
    }

    private PlayerInteractEvent rightClick(ItemStack itemInHand) {
        PlayerInteractEvent event = new PlayerInteractEvent(admin, Action.RIGHT_CLICK_BLOCK, itemInHand, button,
                BlockFace.UP, EquipmentSlot.HAND);
        selectorListener.onPlayerInteract(event);
        stationListener.onPlayerInteract(event);
        return event;
    }

    private Player createAdmin() {
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> adminUniqueId;
            case "getName" -> "Builder";
            case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0]);
            case "sendMessage" -> {
                messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            case "sendActionBar" -> {
                actionBarTexts.add(TournamentMessages.toPlainText((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }

    private Block createBlock(Material material) {
        return SharedBukkitServer.proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> BLOCK_X;
            case "getY" -> BLOCK_Y;
            case "getZ" -> BLOCK_Z;
            case "getType" -> material;
            case "getLocation" -> new Location(world, BLOCK_X, BLOCK_Y, BLOCK_Z);
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }
}
