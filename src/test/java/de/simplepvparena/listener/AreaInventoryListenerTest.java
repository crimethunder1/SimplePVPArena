package de.simplepvparena.listener;

import de.simplepvparena.support.FakeItemStack;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventoryListenerTest {

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;
    private AreaInventoryListener listener;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
        listener = new AreaInventoryListener(harness.getPlugin(), fixture.service());
    }

    @Test
    void walkingIntoTheAreaHandsTheInventoryInAndWalkingOutGivesItBack() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Walker");

        move(player, fixture.outside(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), player.messages());

        move(player, fixture.inside(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE, AreaInventoryFixture.BACK), player.messages());
    }

    @Test
    void aMovementWithinTheSameBlockIsNotChecked() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Turner");
        Location blockCorner = harness.at(5.1D, 5.1D);
        Location sameBlock = harness.at(5.9D, 5.9D);

        move(player, blockCorner, sameBlock);

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
    }

    @Test
    void aTeleportIsJudgedByItsDestination() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Teleporter");

        teleport(player, fixture.outside(), fixture.onTheTribune());
        assertTrue(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());

        teleport(player, fixture.onTheTribune(), fixture.outside());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void aTeleportIntoAnotherWorldGivesTheInventoryBack() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Portal");
        teleport(player, fixture.outside(), fixture.inside());

        teleport(player, fixture.inside(), fixture.inTheLobby());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void aChangedWorldIsCheckedAtTheNewPlace() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("WorldChanger");
        player.moveTo(fixture.inside());
        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(player.player(), fixture.inTheLobby().getWorld()));
        assertTrue(fixture.service().hasDeposited(player.player()));

        player.moveTo(fixture.inTheLobby());
        listener.onPlayerChangedWorld(new PlayerChangedWorldEvent(player.player(), harness.getWorld()));

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void aRespawnIsCheckedOneTickLaterAtTheRespawnPlace() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Respawner");
        player.moveTo(fixture.inside());

        listener.onPlayerRespawn(respawnEvent(player));
        assertFalse(fixture.service().hasDeposited(player.player()), "not before the next tick");
        harness.runTick();

        assertTrue(fixture.service().hasDeposited(player.player()));
    }

    @Test
    void aRespawnOfAPlayerWhoLeftBeforeTheNextTickIsNotChecked() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Quitter");
        player.moveTo(fixture.inside());

        listener.onPlayerRespawn(respawnEvent(player));
        player.setOnline(false);
        harness.runTick();

        assertFalse(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void anAdminInCreativeModeLosesTheItemsTakenInsideAndGetsTheOwnBack() {
        ArenaMatchHarness.FakePlayer admin = fixture.addPlayerOutsideWithItems("CreativeAdmin");
        admin.makeOperator();
        admin.grantAdminPermission();
        admin.player().setGameMode(GameMode.CREATIVE);

        move(admin, fixture.outside(), fixture.inside());
        assertArrayEquals(AreaInventoryFixture.emptyContents(), admin.contents(), "admins are not exempt");
        admin.setContents(AreaInventoryFixture.contentsTakenInside());
        move(admin, fixture.inside(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), admin.contents());
        assertEquals(GameMode.CREATIVE, admin.gameMode());
    }

    @Test
    void itemsHandedOutByThePluginInsideArriveAndAreGoneAfterLeaving() {
        ArenaMatchHarness.FakePlayer spectator = fixture.addPlayerOutsideWithItems("Spectator");
        spectator.keepItemsSetBySlot();
        teleport(spectator, fixture.outside(), fixture.onTheTribune());

        assertTrue(harness.getSpectatorService().sendToTribune(spectator.player()));
        assertTrue(Arrays.asList(spectator.contents()).contains(new FakeItemStack("spectator toggle")),
                "the spectator item arrives as usual");
        move(spectator, fixture.onTheTribune(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), spectator.contents());
    }

    @Test
    void locksDroppingInsideTheAreaForEveryoneIncludingAdmins() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Dropper");
        ArenaMatchHarness.FakePlayer admin = fixture.addPlayerOutsideWithItems("DroppingAdmin");
        admin.makeOperator();
        admin.grantAdminPermission();
        player.moveTo(fixture.onTheTribune());
        admin.moveTo(fixture.inside());

        assertTrue(isDropCancelled(player));
        assertTrue(isDropCancelled(admin));
        assertEquals(List.of(AreaInventoryFixture.DROP_LOCKED), player.actionBars());
        assertEquals(List.of(AreaInventoryFixture.DROP_LOCKED), admin.actionBars());
    }

    @Test
    void letsAPlayerDropOutsideOrWithTheSwitchOff() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("FreeDropper");

        assertFalse(isDropCancelled(player), "outside");
        player.moveTo(fixture.inTheLobby());
        assertFalse(isDropCancelled(player), "in another world");

        player.moveTo(fixture.inside());
        fixture.service().setEnabled(false);
        assertFalse(isDropCancelled(player), "the switch is off");
        assertEquals(List.of(), player.actionBars());
    }

    @Test
    void locksTheEnderChestInsideTheArea() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Hoarder");
        player.moveTo(fixture.inside());

        assertTrue(isOpeningCancelled(player, InventoryType.ENDER_CHEST));
        assertEquals(List.of(AreaInventoryFixture.ENDER_CHEST_LOCKED), player.actionBars());
        assertFalse(isOpeningCancelled(player, InventoryType.CHEST), "the chests of the arena stay usable");
    }

    @Test
    void opensTheEnderChestOutsideOrWithTheSwitchOff() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Owner");

        assertFalse(isOpeningCancelled(player, InventoryType.ENDER_CHEST));

        player.moveTo(fixture.inside());
        fixture.service().setEnabled(false);
        assertFalse(isOpeningCancelled(player, InventoryType.ENDER_CHEST));
        assertEquals(List.of(), player.actionBars());
    }

    @Test
    void aPlayerWhoDiesInsideDropsNothingAndKeepsTheExperienceDropAsBefore() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Fallen");
        move(player, fixture.outside(), fixture.inside());
        player.setContents(AreaInventoryFixture.contentsTakenInside());

        PlayerDeathEvent event = die(player);

        assertEquals(List.of(), event.getDrops());
        assertEquals(7, event.getDroppedExp());
        assertFalse(event.getKeepInventory(), "the items taken inside are lost");
        assertArrayEquals(AreaInventoryFixture.ownContents(),
                fixture.findSavedInventory(player).contents().orElseThrow(), "the own inventory waits in its file");
    }

    @Test
    void aPlayerWhoseInventoryCouldNotBeSavedKeepsItOnADeathInside() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Unsaved");
        fixture.failWritesWith(new IOException("disk full"));
        move(player, fixture.outside(), fixture.inside());

        PlayerDeathEvent event = die(player);

        assertEquals(List.of(), event.getDrops());
        assertTrue(event.getKeepInventory(), "the own inventory stays with the player");
    }

    @Test
    void aDeathOutsideOrWithTheSwitchOffDropsAsUsual() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Outsider");

        assertEquals(2, die(player).getDrops().size());

        player.moveTo(fixture.inside());
        fixture.service().setEnabled(false);
        assertEquals(2, die(player).getDrops().size());
    }

    @Test
    void leavesTheDeathOfAFighterToTheMatch() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        fighter.moveTo(fixture.inside());

        PlayerDeathEvent event = die(fighter);

        assertEquals(2, event.getDrops().size(), "the death listener of the match clears the drops of a fighter");
        assertFalse(event.getKeepInventory());
    }

    @Test
    void handlesTheJoinAfterTheJoinStepsAndJudgesMovementsByTheirFinalOutcome() throws NoSuchMethodException {
        assertEquals(EventPriority.HIGHEST, priorityOf(AreaInventoryListener.class, "onPlayerJoin",
                org.bukkit.event.player.PlayerJoinEvent.class));
        assertEquals(EventPriority.NORMAL, priorityOf(PlayerConnectionListener.class, "onPlayerJoin",
                org.bukkit.event.player.PlayerJoinEvent.class));
        assertTrue(EventPriority.HIGHEST.getSlot() > EventPriority.NORMAL.getSlot());
        assertEquals(EventPriority.MONITOR, priorityOf(AreaInventoryListener.class, "onPlayerMove",
                PlayerMoveEvent.class));
        assertEquals(EventPriority.MONITOR, priorityOf(AreaInventoryListener.class, "onPlayerTeleport",
                PlayerTeleportEvent.class));
        assertTrue(handlerOf(AreaInventoryListener.class, "onPlayerMove", PlayerMoveEvent.class).ignoreCancelled());
        assertTrue(handlerOf(AreaInventoryListener.class, "onPlayerTeleport", PlayerTeleportEvent.class)
                .ignoreCancelled());
        assertTrue(priorityOf(AreaInventoryListener.class, "onPlayerDropItem", PlayerDropItemEvent.class).getSlot()
                < priorityOf(ArenaItemRuleListener.class, "onPlayerDropItem", PlayerDropItemEvent.class).getSlot(),
                "the lock with its message comes before the silent rule of the fight area");
    }

    private void move(ArenaMatchHarness.FakePlayer player, Location origin, Location destination) {
        player.moveTo(destination);
        listener.onPlayerMove(new PlayerMoveEvent(player.player(), origin, destination));
    }

    private void teleport(ArenaMatchHarness.FakePlayer player, Location origin, Location destination) {
        listener.onPlayerTeleport(new PlayerTeleportEvent(player.player(), origin, destination,
                PlayerTeleportEvent.TeleportCause.COMMAND));
        player.moveTo(destination);
    }

    private PlayerRespawnEvent respawnEvent(ArenaMatchHarness.FakePlayer player) {
        return new PlayerRespawnEvent(player.player(), player.location(), false, false, false,
                PlayerRespawnEvent.RespawnReason.DEATH);
    }

    private boolean isDropCancelled(ArenaMatchHarness.FakePlayer player) {
        Location location = player.location();
        Item item = ArenaMatchHarness.proxy(Item.class, (method, arguments) -> switch (method.getName()) {
            case "getLocation" -> location.clone();
            default -> throw new UnsupportedOperationException("Item." + method.getName());
        });
        PlayerDropItemEvent event = new PlayerDropItemEvent(player.player(), item);
        listener.onPlayerDropItem(event);
        return event.isCancelled();
    }

    private boolean isOpeningCancelled(ArenaMatchHarness.FakePlayer player, InventoryType inventoryType) {
        Inventory inventory = ArenaMatchHarness.proxy(Inventory.class, (method, arguments) -> switch (method.getName()) {
            case "getType" -> inventoryType;
            default -> throw new UnsupportedOperationException("Inventory." + method.getName());
        });
        InventoryView view = ArenaMatchHarness.proxy(InventoryView.class, (method, arguments) -> switch (method.getName()) {
            case "getTopInventory" -> inventory;
            case "getPlayer" -> player.player();
            default -> throw new UnsupportedOperationException("InventoryView." + method.getName());
        });
        InventoryOpenEvent event = new InventoryOpenEvent(view);
        listener.onInventoryOpen(event);
        return event.isCancelled();
    }

    private PlayerDeathEvent die(ArenaMatchHarness.FakePlayer player) {
        DamageSource damageSource = ArenaMatchHarness.proxy(DamageSource.class, (method, arguments) -> {
            throw new UnsupportedOperationException("DamageSource." + method.getName());
        });
        List<ItemStack> drops = new ArrayList<>(List.of(new FakeItemStack("dropped sword"),
                new FakeItemStack("dropped apple")));
        PlayerDeathEvent event = new PlayerDeathEvent(player.player(), damageSource, drops, 7, Component.empty(),
                true);
        listener.onPlayerDeath(event);
        return event;
    }

    private static EventPriority priorityOf(Class<?> listenerType, String methodName, Class<?> eventType)
            throws NoSuchMethodException {
        return handlerOf(listenerType, methodName, eventType).priority();
    }

    private static EventHandler handlerOf(Class<?> listenerType, String methodName, Class<?> eventType)
            throws NoSuchMethodException {
        return listenerType.getMethod(methodName, eventType).getAnnotation(EventHandler.class);
    }
}
