package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeSpectatorItems;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventorySpectatorItemTest {

    private static final int SPECTATOR_TOGGLE_ITEM_SLOT = 8;
    private static final ItemStack SPECTATOR_TOGGLE_ITEM = new FakeItemStack(FakeSpectatorItems.SPECTATOR_TOGGLE_LABEL);

    @TempDir
    Path dataFolder;

    private AreaInventoryFixture fixture;
    private ArenaMatchHarness harness;

    @BeforeEach
    void setUp() {
        fixture = new AreaInventoryFixture(dataFolder);
        harness = fixture.harness();
    }

    @Test
    void aPlayerWhoEntersGetsTheSpectatorItemAfterHandingInTheInventoryAndLosesItOnLeaving() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Visitor");
        player.keepItemsSetBySlot();

        fixture.service().reconcile(player.player(), fixture.inside());

        ItemStack[] expectedContents = AreaInventoryFixture.emptyContents();
        expectedContents[SPECTATOR_TOGGLE_ITEM_SLOT] = SPECTATOR_TOGGLE_ITEM;
        assertArrayEquals(expectedContents, player.contents());
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), player.messages());

        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void anAdminWhoEntersGetsTheSpectatorItemAsWell() {
        ArenaMatchHarness.FakePlayer admin = fixture.addPlayerOutsideWithItems("Admin");
        admin.makeOperator();
        admin.grantAdminPermission();
        admin.keepItemsSetBySlot();

        fixture.service().reconcile(admin.player(), fixture.inside());

        assertEquals(1, countSpectatorItems(admin));
        assertEquals(SPECTATOR_TOGGLE_ITEM, admin.contents()[SPECTATOR_TOGGLE_ITEM_SLOT]);
    }

    @Test
    void anActiveFighterWhoHandsInTheInventoryGetsNoSpectatorItem() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        fighter.keepItemsSetBySlot();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        fixture.kitService().clearKit(fighter.player());
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));

        fixture.service().reconcile(fighter.player(), fixture.inside());

        assertTrue(fixture.service().hasDeposited(fighter.player()));
        assertArrayEquals(AreaInventoryFixture.emptyContents(), fighter.contents());
    }

    @Test
    void aSpectatorSentToTheTribuneHasTheSpectatorItemOnlyOnce() {
        ArenaMatchHarness.FakePlayer spectator = fixture.addPlayerOutsideWithItems("Spectator");
        spectator.keepItemsSetBySlot();
        AreaInventoryListener listener = new AreaInventoryListener(harness.getPlugin(), fixture.service());

        listener.onPlayerTeleport(new PlayerTeleportEvent(spectator.player(),
                fixture.outside(), fixture.onTheTribune()));
        spectator.moveTo(fixture.onTheTribune());
        assertTrue(harness.getSpectatorService().sendToTribune(spectator.player()));

        assertEquals(1, countSpectatorItems(spectator));
    }

    @Test
    void aSpectatorWhoMovedTheSpectatorItemGetsNoSecondOneAtTheTribune() {
        ArenaMatchHarness.FakePlayer spectator = fixture.addPlayerOutsideWithItems("Spectator");
        spectator.keepItemsSetBySlot();
        fixture.service().reconcile(spectator.player(), fixture.onTheTribune());
        spectator.moveTo(fixture.onTheTribune());
        ItemStack[] movedContents = AreaInventoryFixture.emptyContents();
        movedContents[3] = spectator.contents()[SPECTATOR_TOGGLE_ITEM_SLOT];
        spectator.setContents(movedContents);

        assertTrue(harness.getSpectatorService().sendToTribune(spectator.player()));

        assertArrayEquals(movedContents, spectator.contents());
        assertEquals(1, countSpectatorItems(spectator));
    }

    @Test
    void withTheSwitchOffNobodyGetsTheSpectatorItemOnEntering() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Visitor");
        player.keepItemsSetBySlot();
        fixture.service().setEnabled(false);

        fixture.service().reconcile(player.player(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertEquals(0, countSpectatorItems(player));
    }

    @Test
    void aPlayerWhoAlreadyCarriesTheSpectatorItemGetsNoSecondOne() {
        FakeSpectatorItems spectatorItems = new FakeSpectatorItems();
        SpectatorService spectatorService = new SpectatorService(new ArenaRegistry(), spectatorItems);
        ArenaMatchHarness.FakePlayer player = harness.addPlayer("Carrier");
        player.keepItemsSetBySlot();
        ItemStack[] contents = AreaInventoryFixture.emptyContents();
        contents[2] = SPECTATOR_TOGGLE_ITEM;
        player.setContents(contents);

        spectatorService.giveSpectatorToggleItemIfMissing(player.player());

        assertArrayEquals(contents, player.contents());
        assertEquals(List.of(), spectatorItems.getCreatedItems());
    }

    private static long countSpectatorItems(ArenaMatchHarness.FakePlayer player) {
        return Arrays.stream(player.contents())
                .filter(SPECTATOR_TOGGLE_ITEM::equals)
                .count();
    }
}
