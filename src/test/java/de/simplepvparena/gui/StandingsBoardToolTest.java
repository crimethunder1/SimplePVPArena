package de.simplepvparena.gui;

import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.StandingsBoardToolService;
import de.simplepvparena.listener.StandingsBoardToolListener;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.support.RecordingItemStack;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandingsBoardToolTest {

    private static final int MAIN_ARENA_SLOT = 10;
    private static final int ARENA_STANDINGS_BOARD_SLOT = 23;
    private static final int INVENTORY_SIZE = 41;
    private static final float ADMIN_YAW = 135.0F;

    private final AdminMenuFixture fixture = new AdminMenuFixture();
    private final StandingsBoardToolService toolService = fixture.standingsBoardToolService();
    private final StandingsBoardToolListener listener = new StandingsBoardToolListener(toolService);
    private final World world = fixture.admin().getLocation().getWorld();
    private final List<String> boardChanges = new ArrayList<>();
    private final TestPlayer admin = new TestPlayer("Builder", true);
    private final TestPlayer steve = new TestPlayer("Steve", false);

    StandingsBoardToolTest() {
        fixture.arenaSetupService().setStandingsBoardListener(() -> boardChanges.add("changed"));
    }

    @Test
    void theToolIsANetherStarWithItsNameItsLoreAndAMarker() {
        RecordingItemStack tool = (RecordingItemStack) toolService.createTool();

        assertEquals(Material.NETHER_STAR, tool.getType());
        assertEquals(StandingsBoardToolService.TOOL_MATERIAL, tool.getType());
        assertEquals("Standings board tool", tool.getName());
        assertEquals(List.of("Right click a block to place the standings board above it",
                "Right click the block of the board to remove it"), tool.getLore());
        assertTrue(toolService.isTool(tool));
    }

    @Test
    void theToolIsNeverRecognisedByItsName() {
        assertFalse(toolService.isTool(RecordingItemStack.named(Material.NETHER_STAR, "Standings board tool")));
        assertFalse(toolService.isTool(new RecordingItemStack(Material.NETHER_STAR, 1)));
        assertFalse(toolService.isTool(null));
        assertFalse(toolService.isTool(fixture.registrationStationService().createTool()));
    }

    @Test
    void theTileWithoutABoardHandsTheToolOut() {
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        assertEquals("23 | OAK_SIGN | Get the standings board tool | Puts the standings board tool into your "
                + "inventory / The board shows the round, the match and the placement / of a tournament",
                describeStandingsBoardTile());

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(1, fixture.handedOutItems().size());
        assertTrue(toolService.isTool(fixture.handedOutItems().get(0)));
        assertEquals(List.of("The standings board tool is in your inventory"), fixture.messages());
        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard(),
                "the click places no board");
        assertEquals(0, fixture.arenaConfigurationSaveCount());
        assertEquals(List.of(), boardChanges);
    }

    @Test
    void aFullInventoryGetsNoTool() {
        fixture.fillAdminInventory();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(List.of(), fixture.handedOutItems());
        assertEquals(List.of("Your inventory is full, the standings board tool could not be handed out"),
                fixture.messages());
    }

    @Test
    void aRightClickOfAnAdminPlacesTheBoardOnTheClickedBlockFacingLikeTheAdmin() {
        PlayerInteractEvent event = rightClick(admin, toolService.createTool(), block(7, 63, -3));

        assertEquals(Optional.of(new PositionSnapshot("admin-menu", 7.5D, 63.0D, -2.5D, ADMIN_YAW, 0.0F)),
                fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board stands here now"), admin.messages);
        assertEquals(1, fixture.arenaConfigurationSaveCount());
        assertEquals(List.of("changed"), boardChanges);
        assertEquals(Event.Result.DENY, event.useInteractedBlock(), "the clicked block is not used");
    }

    @Test
    void aRightClickOnAnotherBlockMovesTheBoard() {
        rightClick(admin, toolService.createTool(), block(7, 63, -3));

        rightClick(admin, toolService.createTool(), block(20, 70, 4));

        assertEquals(Optional.of(new PositionSnapshot("admin-menu", 20.5D, 70.0D, 4.5D, ADMIN_YAW, 0.0F)),
                fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board stands here now", "The standings board stands here now"),
                admin.messages);
        assertEquals(2, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void aRightClickOnTheBlockOfTheBoardRemovesIt() {
        rightClick(admin, toolService.createTool(), block(7, 63, -3));

        rightClick(admin, toolService.createTool(), block(7, 63, -3));

        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board stands here now", "The standings board was removed"),
                admin.messages);
        assertEquals(List.of("changed", "changed"), boardChanges);
    }

    @Test
    void theBlockAboveOrBesideTheBoardIsAnotherBlock() {
        rightClick(admin, toolService.createTool(), block(7, 63, -3));

        rightClick(admin, toolService.createTool(), block(7, 64, -3));

        assertEquals(Optional.of(new PositionSnapshot("admin-menu", 7.5D, 64.0D, -2.5D, ADMIN_YAW, 0.0F)),
                fixture.arenaRegistry().getArena().getStandingsBoard());
    }

    @Test
    void theTileWithABoardStillRemovesItWithOneClick() {
        rightClick(admin, toolService.createTool(), block(7, 63, -3));
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        assertEquals("23 | OAK_SIGN | Remove the standings board | Stands at admin-menu 7.5/63.0/-2.5 / "
                + "Click to remove it", describeStandingsBoardTile());

        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(List.of("The standings board was removed"), fixture.messages());
        assertEquals(List.of(), fixture.handedOutItems());
    }

    @Test
    void aRightClickIntoTheAirAsksForABlock() {
        PlayerInteractEvent event = interact(admin, Action.RIGHT_CLICK_AIR, toolService.createTool(), null,
                EquipmentSlot.HAND);

        assertEquals(List.of("Right click a block"), admin.messages);
        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertEquals(Event.Result.DENY, event.useItemInHand());
    }

    @Test
    void aLeftClickAndTheOffHandDoNothing() {
        PlayerInteractEvent leftClick = interact(admin, Action.LEFT_CLICK_BLOCK, toolService.createTool(),
                block(7, 63, -3), EquipmentSlot.HAND);
        interact(admin, Action.RIGHT_CLICK_BLOCK, toolService.createTool(), block(7, 63, -3), EquipmentSlot.OFF_HAND);

        assertEquals(Event.Result.DENY, leftClick.useInteractedBlock(), "the tool breaks no block");
        assertEquals(List.of(), admin.messages);
        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
    }

    @Test
    void aPlayerWhoIsNoAdminChangesNothingAndLosesEveryTool() {
        ItemStack tool = toolService.createTool();
        RecordingItemStack sword = new RecordingItemStack(Material.IRON_SWORD, 1);
        steve.contents[0] = tool;
        steve.contents[7] = toolService.createTool();
        steve.contents[1] = sword;

        PlayerInteractEvent event = rightClick(steve, tool, block(7, 63, -3));

        assertEquals(Event.Result.DENY, event.useInteractedBlock());
        assertEquals(Optional.empty(), fixture.arenaRegistry().getArena().getStandingsBoard());
        assertNull(steve.contents[0]);
        assertNull(steve.contents[7]);
        assertEquals(sword, steve.contents[1]);
        assertEquals(List.of(), steve.messages);
        assertEquals(0, fixture.arenaConfigurationSaveCount());
    }

    @Test
    void anItemWithoutTheMarkerIsLeftAlone() {
        RecordingItemStack star = RecordingItemStack.named(Material.NETHER_STAR, "Standings board tool");
        steve.contents[0] = star;

        PlayerInteractEvent event = rightClick(steve, star, block(7, 63, -3));

        assertEquals(Event.Result.ALLOW, event.useInteractedBlock());
        assertEquals(star, steve.contents[0]);
    }

    @Test
    @UseLanguage("de")
    void handsOutPlacesAndRemovesInGerman() {
        RecordingItemStack tool = (RecordingItemStack) toolService.createTool();
        fixture.openMenuFromTheMainMenu(MAIN_ARENA_SLOT);
        assertEquals("23 | OAK_SIGN | Werkzeug für die Platzierungstafel holen | Legt das Werkzeug für die "
                + "Platzierungstafel in dein Inventar / Die Tafel zeigt die Turnierrunde, das Match und die "
                + "Platzierung / eines Turniers", describeStandingsBoardTile());
        fixture.click(ARENA_STANDINGS_BOARD_SLOT);
        fixture.fillAdminInventory();
        fixture.click(ARENA_STANDINGS_BOARD_SLOT);

        rightClick(admin, tool, block(7, 63, -3));
        rightClick(admin, tool, block(7, 63, -3));
        interact(admin, Action.RIGHT_CLICK_AIR, tool, null, EquipmentSlot.HAND);

        assertEquals("Werkzeug für die Platzierungstafel", tool.getName());
        assertEquals(List.of("Rechtsklick auf einen Block stellt die Platzierungstafel darüber auf",
                "Rechtsklick auf den Block der Tafel entfernt sie wieder"), tool.getLore());
        assertEquals(List.of("Das Werkzeug für die Platzierungstafel liegt in deinem Inventar",
                "Dein Inventar ist voll, das Werkzeug für die Platzierungstafel konnte nicht ausgegeben werden"),
                fixture.messages());
        assertEquals(List.of("Die Platzierungstafel steht jetzt hier", "Die Platzierungstafel wurde entfernt",
                "Klicke mit Rechtsklick auf einen Block"), admin.messages);
    }

    private String describeStandingsBoardTile() {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(ARENA_STANDINGS_BOARD_SLOT + " | "))
                .findFirst()
                .orElseThrow();
    }

    private Block block(int blockX, int blockY, int blockZ) {
        return SharedBukkitServer.proxy(Block.class, (method, arguments) -> switch (method.getName()) {
            case "getWorld" -> world;
            case "getX" -> blockX;
            case "getY" -> blockY;
            case "getZ" -> blockZ;
            case "getType" -> Material.STONE;
            default -> throw new UnsupportedOperationException("Block." + method.getName());
        });
    }

    private PlayerInteractEvent rightClick(TestPlayer player, ItemStack item, Block block) {
        return interact(player, Action.RIGHT_CLICK_BLOCK, item, block, EquipmentSlot.HAND);
    }

    private PlayerInteractEvent interact(TestPlayer player, Action action, ItemStack item, Block block,
                                         EquipmentSlot hand) {
        PlayerInteractEvent event = new PlayerInteractEvent(player.player, action, item, block, BlockFace.UP, hand);
        listener.onPlayerInteract(event);
        return event;
    }

    private final class TestPlayer {

        private final List<String> messages = new ArrayList<>();
        private final ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        private final Player player;

        private TestPlayer(String playerName, boolean adminPermission) {
            PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class,
                    (method, arguments) -> switch (method.getName()) {
                        case "getContents" -> contents.clone();
                        case "setItem" -> {
                            contents[(Integer) arguments[0]] = (ItemStack) arguments[1];
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                    });
            player = SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
                case "getName" -> playerName;
                case "hasPermission" -> AdminPermission.ADMIN_PERMISSION_NAME.equals(arguments[0])
                        && adminPermission;
                case "getLocation" -> new Location(world, 0.5D, 64.0D, 0.5D, ADMIN_YAW, 30.0F);
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
