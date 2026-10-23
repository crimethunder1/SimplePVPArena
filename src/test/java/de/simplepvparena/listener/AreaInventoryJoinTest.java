package de.simplepvparena.listener;

import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.kit.FilePlayerStateSnapshotStorage;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeItemStackCodec;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventoryJoinTest {

    private static final double TRIBUNE_COORDINATE = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;
    private PlayerConnectionListener connectionListener;
    private AreaInventoryListener areaInventoryListener;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
        connectionListener = createConnectionListener();
        areaInventoryListener = new AreaInventoryListener(harness.getPlugin(), fixture.service());
    }

    @Test
    void aJoinOutsideAfterACrashGivesTheInventoryBack() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Returner");
        player.moveTo(fixture.inside());
        fixture.service().reconcile(player.player());
        player.setContents(AreaInventoryFixture.contentsTakenInside());
        player.setOnline(false);
        restartAfterACrash();
        harness.getArenaRegistry().setTeleportOnJoin(false);
        player.moveTo(fixture.inTheLobby());

        join(player);

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertFalse(Files.exists(fixture.inventoryFileOf(player)));
    }

    @Test
    void aJoinInsideAfterACrashLeavesTheInventoryHandedIn() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Stayer");
        player.moveTo(fixture.inside());
        fixture.service().reconcile(player.player());
        player.setOnline(false);
        restartAfterACrash();
        harness.getArenaRegistry().setTeleportOnJoin(false);
        player.forgetReceivedTexts();

        join(player);

        assertTrue(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
        assertTrue(Files.isRegularFile(fixture.inventoryFileOf(player)));
        assertEquals(List.of(), player.messages());
    }

    @Test
    void aJoinTeleportToTheTribuneHandsTheInventoryInAfterwards() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Newcomer");
        assertTrue(harness.getArenaRegistry().isTeleportOnJoin());
        assertTrue(harness.getArenaRegistry().isArenaFullyConfigured());

        join(player);

        assertEquals(TRIBUNE_COORDINATE, player.location().getX());
        assertTrue(fixture.service().hasDeposited(player.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(),
                fixture.findSavedInventory(player).contents().orElseThrow());
    }

    @Test
    void aJoinWithTheSwitchOffGivesTheInventoryBackEvenInside() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("SwitchedOff");
        player.moveTo(fixture.inside());
        fixture.service().reconcile(player.player());
        player.setOnline(false);
        restartAfterACrash();
        fixture.service().setEnabled(false);

        join(player);

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
    }

    @Test
    void aJoinAfterACrashDuringAMatchFirstRestoresTheMatchStateAndThenGivesTheOwnInventoryBack() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("CrashedFighter");
        ItemStack[] carriedInside = carriedInside();
        crashDuringAMatch(fighter, carriedInside);
        harness.forgetTribuneSpawn();
        fighter.moveTo(fixture.inTheLobby());

        join(fighter);

        assertFalse(fixture.kitService().hasAppliedKit(fighter.player()), "the match state is restored");
        assertFalse(Files.exists(fixture.matchSnapshotFileOf(fighter)));
        assertFalse(fixture.service().hasDeposited(fighter.player()), "and then the area inventory");
        assertFalse(Files.exists(fixture.inventoryFileOf(fighter)));
        assertArrayEquals(AreaInventoryFixture.ownContents(), fighter.contents());
        assertEquals(17, fighter.level(), "the level of the match snapshot is back");
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void aJoinAfterACrashDuringAMatchSentToTheTribuneKeepsTheOwnInventoryHandedIn() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("TribuneFighter");
        ItemStack[] carriedInside = carriedInside();
        crashDuringAMatch(fighter, carriedInside);
        fighter.moveTo(fixture.inside());

        join(fighter);

        assertEquals(TRIBUNE_COORDINATE, fighter.location().getX());
        assertFalse(fixture.kitService().hasAppliedKit(fighter.player()));
        assertArrayEquals(carriedInside, fighter.contents(), "the match gives back what was carried inside");
        assertTrue(fixture.service().hasDeposited(fighter.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(),
                fixture.findSavedInventory(fighter).contents().orElseThrow());

        fixture.service().reconcile(fighter.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), fighter.contents());
    }

    @Test
    void aFighterWhoComesBackWithinTheReconnectWindowIsNotTouched() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        for (ArenaMatchHarness.FakePlayer player : List.of(fighter, mate, opponent, opponentMate)) {
            player.moveTo(fixture.onTheTribune());
        }
        fixture.service().reconcileOnlinePlayers();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        ItemStack[] inventoryOfTheFight = AreaInventoryFixture.emptyContents();
        inventoryOfTheFight[0] = new FakeItemStack("kit sword");
        fighter.setContents(inventoryOfTheFight);
        int inventoryClearCount = fighter.inventoryClearCount();

        quit(fighter);
        harness.advanceSeconds(30);
        join(fighter);

        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        assertArrayEquals(inventoryOfTheFight, fighter.contents());
        assertEquals(inventoryClearCount, fighter.inventoryClearCount());
        assertTrue(fixture.service().hasDeposited(fighter.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(),
                fixture.findSavedInventory(fighter).contents().orElseThrow());
    }

    private void crashDuringAMatch(ArenaMatchHarness.FakePlayer fighter, ItemStack[] carriedInside) {
        fighter.moveTo(fixture.onTheTribune());
        fixture.service().reconcile(fighter.player());
        fighter.setContents(carriedInside);
        new KitService(harness.getPlugin(), new FilePlayerStateSnapshotStorage(
                dataFolder.resolve(FilePlayerStateSnapshotStorage.DIRECTORY_NAME), new FakeItemStackCodec()),
                fixture.warnings()::add).applyKit(fighter.player());
        assertTrue(fixture.kitService().hasAppliedKit(fighter.player()));
        assertTrue(fixture.service().hasDeposited(fighter.player()));
        assertTrue(Files.isRegularFile(fixture.matchSnapshotFileOf(fighter)));
        fighter.setOnline(false);
        restartAfterACrash();
    }

    private static ItemStack[] carriedInside() {
        ItemStack[] carriedInside = AreaInventoryFixture.emptyContents();
        carriedInside[4] = new FakeItemStack("spectator toggle");
        return carriedInside;
    }

    private void restartAfterACrash() {
        fixture.restartService();
        areaInventoryListener = new AreaInventoryListener(harness.getPlugin(), fixture.service());
    }

    private PlayerConnectionListener createConnectionListener() {
        return new PlayerConnectionListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), new BorderSelectorModeRegistry(), harness.getKitService(),
                harness.getPlugin().getLogger());
    }

    private void quit(ArenaMatchHarness.FakePlayer player) {
        connectionListener.onPlayerQuit(new PlayerQuitEvent(player.player(), Component.empty(),
                PlayerQuitEvent.QuitReason.DISCONNECTED));
        player.setOnline(false);
    }

    private void join(ArenaMatchHarness.FakePlayer player) {
        player.setOnline(true);
        PlayerJoinEvent event = new PlayerJoinEvent(player.player(), Component.empty());
        connectionListener.onPlayerJoin(event);
        areaInventoryListener.onPlayerJoin(event);
    }
}
