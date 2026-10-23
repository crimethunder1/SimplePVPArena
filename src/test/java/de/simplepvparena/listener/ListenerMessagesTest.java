package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineRegistry;
import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.SpectatorMessage;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeSpectatorItems;
import de.simplepvparena.support.UseLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListenerMessagesTest {

    private static final int ARENA_FLOOR_X = 5;
    private static final int ARENA_FLOOR_Z = 5;
    private static final int TRIBUNE_X = ArenaMatchHarness.WALL_MAXIMUM + 5;
    private static final int TRIBUNE_Z = ArenaMatchHarness.WALL_MAXIMUM + 5;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer spectator;
    private ArenaMatchHarness.FakePlayer admin;
    private final BorderSelectorModeRegistry selectorModes = new BorderSelectorModeRegistry();

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        harness.setBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y, TRIBUNE_Z, Material.STONE);
        fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        spectator = harness.addPlayer("Spectator");
        admin = harness.addPlayer("Admin");
        admin.makeOperator();
        harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    @Test
    @UseLanguage("en")
    void refusesBuildingAndBreakingInEnglish() {
        assertEquals(List.of("Only admins change blocks outside the arena",
                "Only the fighters of the running round build inside the arena"), buildMessages());
        assertEquals(List.of("Only admins change blocks outside the arena",
                "Only the fighters of the running round change blocks inside the arena",
                "The arena stays as it is, only what was placed during the match comes down again"), breakMessages());
    }

    @Test
    @UseLanguage("de")
    void refusesBuildingAndBreakingInGerman() {
        assertEquals(List.of("Nur Admins verändern Blöcke außerhalb der Arena",
                "Nur die Kämpfer der laufenden Runde bauen in der Arena"), buildMessages());
        assertEquals(List.of("Nur Admins verändern Blöcke außerhalb der Arena",
                "Nur die Kämpfer der laufenden Runde verändern Blöcke in der Arena",
                "Die Arena bleibt, wie sie ist, nur was während des Matches gesetzt wurde, lässt sich wieder abbauen"),
                breakMessages());
    }

    @Test
    @UseLanguage("en")
    void welcomesAFighterBackInEnglish() {
        rejoin(fighter);

        assertEquals(List.of("You rejoined during match M1, you take part again from the next round on"),
                fighter.messages());
    }

    @Test
    @UseLanguage("de")
    void welcomesAFighterBackInGerman() {
        rejoin(fighter);

        assertEquals(List.of("Du bist während des Matches M1 zurückgekehrt, du machst ab der nächsten Runde wieder "
                + "mit"), fighter.messages());
    }

    @Test
    @UseLanguage("en")
    void answersTheSpectatorItemInEnglish() {
        useSpectatorItem(fighter);
        useSpectatorItem(spectator);

        assertEquals(List.of("You cannot switch to spectator mode while you are fighting"), fighter.messages());
        assertEquals(List.of("You are not watching the arena, use /tournament spectate"), spectator.messages());
        assertEquals("The tribune spawn is not set, use the spectator item to stop flying",
                Messages.text(SpectatorMessage.TRIBUNE_SPAWN_NOT_SET));
    }

    @Test
    @UseLanguage("de")
    void answersTheSpectatorItemInGerman() {
        useSpectatorItem(fighter);
        useSpectatorItem(spectator);

        assertEquals(List.of("Du kannst nicht in den Zuschauermodus wechseln, während du kämpfst"),
                fighter.messages());
        assertEquals(List.of("Du schaust der Arena nicht zu, nutze /tournament spectate"), spectator.messages());
        assertEquals("Der Tribünen-Spawn ist nicht gesetzt, nutze das Zuschauer-Item, um mit dem Fliegen aufzuhören",
                Messages.text(SpectatorMessage.TRIBUNE_SPAWN_NOT_SET));
    }

    @Test
    @UseLanguage("en")
    void reportsTheBorderSelectorInEnglish() {
        assertEquals(List.of("Marked as a border block: 5 64 5", "Border mark removed: 5 64 5"), markTwice());
        assertEquals(List.of("You are not allowed to mark border blocks any more, the border selector mode was "
                + "switched off"), clickWithoutPermission());
    }

    @Test
    @UseLanguage("de")
    void reportsTheBorderSelectorInGerman() {
        assertEquals(List.of("Als Wandblock markiert: 5 64 5", "Wandmarkierung entfernt: 5 64 5"), markTwice());
        assertEquals(List.of("Du darfst keine Wandblöcke mehr markieren, der Wandauswahlmodus wurde ausgeschaltet"),
                clickWithoutPermission());
    }

    private List<String> buildMessages() {
        ArenaBuildListener buildListener = new ArenaBuildListener(harness.getMatchService(),
                harness.getArenaRegistry());
        place(buildListener, spectator, harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y + 1, TRIBUNE_Z));
        place(buildListener, spectator, harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y + 1,
                ARENA_FLOOR_Z));
        return spectator.messages();
    }

    private List<String> breakMessages() {
        ArenaBaselineService baselineService = new ArenaBaselineService(harness.getPlugin(),
                new ArenaBaselineRegistry());
        baselineService.startCapture(harness.getShape(), report -> { });
        harness.runTick();
        ArenaBreakListener breakListener = new ArenaBreakListener(harness.getMatchService(),
                harness.getArenaRegistry(), baselineService);
        breakListener.onBlockBreak(new BlockBreakEvent(harness.createBlock(TRIBUNE_X, ArenaMatchHarness.GROUND_Y,
                TRIBUNE_Z), spectator.player()));
        breakListener.onBlockBreak(new BlockBreakEvent(harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y,
                ARENA_FLOOR_Z), spectator.player()));
        breakListener.onBlockBreak(new BlockBreakEvent(harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y,
                ARENA_FLOOR_Z), fighter.player()));
        return List.of(spectator.actionBars().get(0), spectator.actionBars().get(1), fighter.actionBars().get(0));
    }

    private static void place(ArenaBuildListener buildListener, ArenaMatchHarness.FakePlayer player, Block block) {
        buildListener.onBlockPlace(new BlockPlaceEvent(block, null, block, null, player.player(), true,
                EquipmentSlot.HAND));
    }

    private void rejoin(ArenaMatchHarness.FakePlayer player) {
        PlayerConnectionListener listener = new PlayerConnectionListener(harness.getMatchService(),
                harness.getSpectatorService(), harness.getArenaRegistry(), selectorModes, harness.getKitService(),
                harness.getPlugin().getLogger());
        player.forgetReceivedTexts();
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));
    }

    private void useSpectatorItem(ArenaMatchHarness.FakePlayer player) {
        PlayerInteractListener listener = new PlayerInteractListener(harness.getMatchService(),
                harness.getSpectatorService(), selectorModes, harness.getPlugin().getLogger());
        player.forgetReceivedTexts();
        listener.onPlayerInteract(new PlayerInteractEvent(player.player(), Action.RIGHT_CLICK_AIR,
                new FakeItemStack(FakeSpectatorItems.SPECTATOR_TOGGLE_LABEL), null, BlockFace.UP,
                EquipmentSlot.HAND));
    }

    private List<String> markTwice() {
        BorderSelectorListener listener = new BorderSelectorListener(selectorModes, harness.getBorderService());
        selectorModes.activate(admin.player().getUniqueId());
        Block block = harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y, ARENA_FLOOR_Z);
        listener.onPlayerInteract(rightClick(admin, block));
        listener.onPlayerInteract(rightClick(admin, block));
        return admin.actionBars();
    }

    private List<String> clickWithoutPermission() {
        BorderSelectorListener listener = new BorderSelectorListener(selectorModes, harness.getBorderService());
        selectorModes.activate(spectator.player().getUniqueId());
        listener.onPlayerInteract(rightClick(spectator, harness.createBlock(ARENA_FLOOR_X, ArenaMatchHarness.GROUND_Y,
                ARENA_FLOOR_Z)));
        return spectator.messages();
    }

    private static PlayerInteractEvent rightClick(ArenaMatchHarness.FakePlayer player, Block block) {
        return new PlayerInteractEvent(player.player(), Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP,
                EquipmentSlot.HAND);
    }
}
