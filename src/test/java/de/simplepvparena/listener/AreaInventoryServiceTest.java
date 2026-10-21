package de.simplepvparena.listener;

import de.simplepvparena.arena.AreaInventoryLookup;
import de.simplepvparena.arena.AreaInventoryService;
import de.simplepvparena.match.Match;
import de.simplepvparena.support.FakeItemStack;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaInventoryServiceTest {

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
    void savesTheWholeInventoryOfAPlayerWhoEntersAndClearsItOnlyAfterTheFileIsWritten() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Visitor");
        player.player().setGameMode(GameMode.CREATIVE);
        AtomicReference<ItemStack[]> contentsWhileWriting = new AtomicReference<>();
        fixture.observeSave(() -> contentsWhileWriting.set(player.contents()));

        fixture.service().reconcile(player.player(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), contentsWhileWriting.get(),
                "the inventory is still complete while the file is written");
        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
        assertArrayEquals(AreaInventoryFixture.ownContents(),
                fixture.findSavedInventory(player).contents().orElseThrow(), "armour and off hand are saved too");
        assertTrue(fixture.service().hasDeposited(player.player()));
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), player.messages());
        assertEquals(17, player.level(), "the level stays untouched");
        assertEquals(GameMode.CREATIVE, player.gameMode(), "the game mode stays untouched");
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void anAdminAndAnOperatorHandInTheirInventoryAsWell() {
        ArenaMatchHarness.FakePlayer admin = fixture.addPlayerOutsideWithItems("Admin");
        admin.grantAdminPermission();
        ArenaMatchHarness.FakePlayer operator = fixture.addPlayerOutsideWithItems("Operator");
        operator.makeOperator();

        fixture.service().reconcile(admin.player(), fixture.onTheTribune());
        fixture.service().reconcile(operator.player(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.emptyContents(), admin.contents());
        assertArrayEquals(AreaInventoryFixture.emptyContents(), operator.contents());
        assertTrue(fixture.service().hasDeposited(admin.player()));
        assertTrue(fixture.service().hasDeposited(operator.player()));
    }

    @Test
    void keepsTheInventoryWhenTheFileCannotBeWrittenAndTriesAgainWithAtMostOneWarningAMinute() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Unlucky");
        fixture.failWritesWith(new IOException("disk full"));

        fixture.service().reconcile(player.player(), fixture.inside());
        fixture.advanceClockMillis(59_000L);
        fixture.service().reconcile(player.player(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents(), "nothing is cleared");
        assertEquals(0, player.inventoryClearCount());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertFalse(Files.exists(fixture.inventoryFileOf(player)));
        assertEquals(List.of(), player.messages());
        assertEquals(List.of("The inventory of Unlucky could not be written to disk (disk full), it stays with the "
                + "player and the next check tries again. This warning repeats at most once a minute"),
                fixture.warnings());

        fixture.advanceClockMillis(1_000L);
        fixture.service().reconcile(player.player(), fixture.inside());
        assertEquals(2, fixture.warnings().size(), "a minute later the warning comes again");

        fixture.failWritesWith(null);
        fixture.service().reconcile(player.player(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
        assertTrue(fixture.service().hasDeposited(player.player()));
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), player.messages());
    }

    @Test
    void theWarningIntervalIsOneMinute() {
        assertEquals(60_000L, AreaInventoryService.WARNING_INTERVAL_MILLIS);
    }

    @Test
    void leavesAPlayerAloneWhoStaysInside() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Stayer");
        fixture.service().reconcile(player.player(), fixture.inside());
        player.setContents(AreaInventoryFixture.contentsTakenInside());

        fixture.service().reconcile(player.player(), fixture.onTheTribune());
        fixture.service().reconcile(player.player(), fixture.inside());

        assertArrayEquals(AreaInventoryFixture.contentsTakenInside(), player.contents());
        assertArrayEquals(AreaInventoryFixture.ownContents(),
                fixture.findSavedInventory(player).contents().orElseThrow(), "the saved inventory is not overwritten");
        assertEquals(List.of(AreaInventoryFixture.KEPT_SAFE), player.messages());
    }

    @Test
    void givesTheOwnInventoryBackOnLeavingAndRemovesEverythingTakenInside() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Leaver");
        fixture.service().reconcile(player.player(), fixture.inside());
        player.setContents(AreaInventoryFixture.contentsTakenInside());
        player.forgetReceivedTexts();

        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertFalse(Files.exists(fixture.inventoryFileOf(player)));
        assertEquals(1, player.playerDataSaveCount(), "the player data is saved before the file is deleted");
        assertEquals(List.of(AreaInventoryFixture.BACK), player.messages());
        assertEquals(17, player.level());
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void givesTheInventoryBackInAnotherWorld() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Traveller");
        fixture.service().reconcile(player.player(), fixture.inside());

        fixture.service().reconcile(player.player(), fixture.inTheLobby());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
    }

    @Test
    void doesNothingForAPlayerOutsideWhoHandedNothingIn() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Bystander");

        fixture.service().reconcile(player.player(), fixture.outside());
        fixture.service().reconcile(player.player(), fixture.inTheLobby());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertEquals(0, player.inventoryClearCount());
        assertEquals(List.of(), player.messages());
    }

    @Test
    void clearsNothingWhenTheSavedFileIsUnreadableAndSetsItAsideForARestoreByHand() throws IOException {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Broken");
        fixture.service().reconcile(player.player(), fixture.inside());
        Files.writeString(fixture.inventoryFileOf(player), "{{{", StandardCharsets.UTF_8);
        player.setContents(AreaInventoryFixture.contentsTakenInside());
        player.forgetReceivedTexts();
        int inventoryClearCountBeforeLeaving = player.inventoryClearCount();

        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.contentsTakenInside(), player.contents(), "nothing is cleared");
        assertEquals(inventoryClearCountBeforeLeaving, player.inventoryClearCount());
        assertFalse(fixture.service().hasDeposited(player.player()), "the marker is removed");
        assertTrue(Files.isRegularFile(fixture.brokenInventoryFileOf(player)));
        assertEquals(List.of("The saved inventory of Broken could not be read, nothing was cleared and nothing "
                + "given back, the file is kept as " + player.uniqueId() + ".yml.broken for a restore by hand"),
                fixture.warnings());
        assertEquals(List.of(AreaInventoryFixture.NOT_GIVEN_BACK), player.messages());

        fixture.service().reconcile(player.player(), fixture.outside());
        assertEquals(1, fixture.warnings().size(), "the warning comes once");
    }

    @Test
    void clearsNothingWhenTheSavedFileIsMissing() throws IOException {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Missing");
        fixture.service().reconcile(player.player(), fixture.inside());
        Files.delete(fixture.inventoryFileOf(player));
        player.setContents(AreaInventoryFixture.contentsTakenInside());
        player.forgetReceivedTexts();

        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.contentsTakenInside(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertEquals(List.of("The inventory of Missing was marked as handed in, but " + player.uniqueId()
                + ".yml does not exist, nothing was cleared and nothing given back"), fixture.warnings());
        assertEquals(List.of(AreaInventoryFixture.NOT_GIVEN_BACK), player.messages());
    }

    @Test
    void keepsTheMarkerAndTheFileWhenTheInventoryCannotBeGivenBackAndTriesAgain() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Stuck");
        fixture.service().reconcile(player.player(), fixture.inside());
        player.failInventoryClearWith(new IllegalStateException("inventory locked"));

        fixture.service().reconcile(player.player(), fixture.outside());
        fixture.service().reconcile(player.player(), fixture.outside());

        assertTrue(fixture.service().hasDeposited(player.player()));
        assertTrue(Files.isRegularFile(fixture.inventoryFileOf(player)));
        assertEquals(List.of("The saved inventory of Stuck could not be given back (inventory locked), the file is "
                + "kept and the next check tries again. This warning repeats at most once a minute"),
                fixture.warnings());

        player.failInventoryClearWith(null);
        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertFalse(Files.exists(fixture.inventoryFileOf(player)));
    }

    @Test
    void keepsTheFileAsACopyWhenThePlayerDataCannotBeSaved() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Unsaved");
        fixture.service().reconcile(player.player(), fixture.inside());
        player.failPlayerDataSaveWith(new IllegalStateException("player data locked"));

        fixture.service().reconcile(player.player(), fixture.outside());

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertTrue(Files.isRegularFile(fixture.inventoryFileOf(player)));
        assertEquals(List.of("The inventory of Unsaved is back, but the player data could not be saved (player "
                + "data locked), " + player.uniqueId() + ".yml is kept as a copy"), fixture.warnings());
    }

    @Test
    void theSwitchTurnedOffGivesEveryInventoryBackAtOnceEvenInsideAndOnGivesItBackToTheArea() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Switched");
        player.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();
        player.setContents(AreaInventoryFixture.contentsTakenInside());
        player.forgetReceivedTexts();

        fixture.service().setEnabled(false);

        assertFalse(fixture.service().isEnabled());
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
        assertEquals(List.of(AreaInventoryFixture.BACK), player.messages());

        fixture.service().reconcile(player.player(), fixture.inside());
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents(), "nobody hands anything in");
        assertFalse(fixture.service().isLockedAt(fixture.inside()));

        fixture.service().setEnabled(true);

        assertArrayEquals(AreaInventoryFixture.emptyContents(), player.contents());
        assertTrue(fixture.service().hasDeposited(player.player()));
    }

    @Test
    void aResetArenaWithoutAreaOfEffectGivesTheInventoryBack() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("ResetWitness");
        player.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();
        assertTrue(fixture.service().hasDeposited(player.player()));

        harness.getArenaRegistry().clearArenaConfiguration();
        fixture.service().reconcileOnlinePlayers();

        assertTrue(harness.getArenaRegistry().findEffectArea().isEmpty());
        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
        assertFalse(fixture.service().hasDeposited(player.player()));
    }

    @Test
    void aSmallerMarginGivesTheInventoryBackToAPlayerWhoIsNowOutside() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("MarginWitness");
        player.moveTo(fixture.onTheTribune());
        fixture.service().reconcileOnlinePlayers();
        assertTrue(fixture.service().hasDeposited(player.player()));

        harness.getArenaRegistry().setEffectMargin(0);
        fixture.service().reconcileOnlinePlayers();

        assertArrayEquals(AreaInventoryFixture.ownContents(), player.contents());
    }

    @Test
    void theCheckOfAllOnlinePlayersRunsEverySecondUntilTheServiceStops() {
        ArenaMatchHarness.FakePlayer player = fixture.addPlayerOutsideWithItems("Teleported");
        fixture.service().start();
        assertTrue(fixture.service().isRunning());
        assertEquals(20L, AreaInventoryService.RECONCILE_PERIOD_TICKS);
        player.moveTo(fixture.inside());

        harness.advanceTicksWithRepeatingTasks(19L);
        assertFalse(fixture.service().hasDeposited(player.player()), "not before the first second");
        harness.advanceTicksWithRepeatingTasks(1L);
        assertTrue(fixture.service().hasDeposited(player.player()), "a teleport of another plugin is caught");

        player.moveTo(fixture.outside());
        fixture.service().stop();
        harness.advanceTicksWithRepeatingTasks(40L);

        assertFalse(fixture.service().isRunning());
        assertTrue(fixture.service().hasDeposited(player.player()), "a stopped service checks nothing");
    }

    @Test
    void theShutdownGivesEveryInventoryBackWithoutAMessageAndTheNextStartTakesItAgain() {
        ArenaMatchHarness.FakePlayer inside = fixture.addPlayerOutsideWithItems("Inside");
        ArenaMatchHarness.FakePlayer outside = fixture.addPlayerOutsideWithItems("Outside");
        inside.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();
        inside.setContents(AreaInventoryFixture.contentsTakenInside());
        inside.forgetReceivedTexts();

        fixture.service().stop();
        fixture.service().giveEveryInventoryBackQuietly();

        assertArrayEquals(AreaInventoryFixture.ownContents(), inside.contents());
        assertFalse(fixture.service().hasDeposited(inside.player()));
        assertEquals(List.of(), inside.messages(), "the shutdown gives it back without a message");
        assertArrayEquals(AreaInventoryFixture.ownContents(), outside.contents());
        assertEquals(0, outside.inventoryClearCount());

        fixture.restartService();
        fixture.service().reconcileOnlinePlayers();

        assertArrayEquals(AreaInventoryFixture.emptyContents(), inside.contents());
        assertTrue(fixture.service().hasDeposited(inside.player()));
    }

    @Test
    void afterACrashTheNextStartGivesTheInventoryBackOutsideAndLeavesItInside() {
        ArenaMatchHarness.FakePlayer leftInside = fixture.addPlayerOutsideWithItems("LeftInside");
        ArenaMatchHarness.FakePlayer nowOutside = fixture.addPlayerOutsideWithItems("NowOutside");
        leftInside.moveTo(fixture.inside());
        nowOutside.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();

        fixture.restartService();
        nowOutside.moveTo(fixture.inTheLobby());
        fixture.service().reconcile(leftInside.player());
        fixture.service().reconcile(nowOutside.player());

        assertTrue(fixture.service().hasDeposited(leftInside.player()));
        assertArrayEquals(AreaInventoryFixture.emptyContents(), leftInside.contents());
        assertTrue(Files.isRegularFile(fixture.inventoryFileOf(leftInside)));
        assertArrayEquals(AreaInventoryFixture.ownContents(), nowOutside.contents());
        assertFalse(fixture.service().hasDeposited(nowOutside.player()));
    }

    @Test
    void leavesAFighterWithTheKitAloneAndGivesTheOwnInventoryBackAfterTheMatch() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        for (ArenaMatchHarness.FakePlayer player : List.of(fighter, mate, opponent, opponentMate)) {
            player.moveTo(fixture.onTheTribune());
        }
        fixture.service().reconcileOnlinePlayers();
        ItemStack[] spectatorContents = AreaInventoryFixture.emptyContents();
        spectatorContents[4] = new FakeItemStack("spectator toggle");
        fighter.setContents(spectatorContents);

        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        assertTrue(fixture.kitService().hasAppliedKit(fighter.player()));
        int inventoryClearCountWithTheKit = fighter.inventoryClearCount();
        fighter.moveTo(fixture.outside());
        fixture.service().reconcile(fighter.player());

        assertEquals(inventoryClearCountWithTheKit, fighter.inventoryClearCount(), "the kit is not touched");
        assertTrue(fixture.service().hasDeposited(fighter.player()));

        assertTrue(harness.getMatchService().forceResult(match.getMatchId(), match.getFirstTeam().getTeamName()));
        assertFalse(fixture.kitService().hasAppliedKit(fighter.player()));
        assertArrayEquals(spectatorContents, fighter.contents(), "the match gives back what was carried inside");
        fighter.moveTo(fixture.outside());
        fixture.service().reconcile(fighter.player());

        assertArrayEquals(AreaInventoryFixture.ownContents(), fighter.contents());
        assertEquals(17, fighter.level());
        assertFalse(fixture.service().hasDeposited(fighter.player()));
        assertEquals(List.of(), fixture.warnings());
    }

    @Test
    void doesNotTakeTheKitOfAFighterWhoNeverHandedAnythingIn() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        int inventoryClearCountWithTheKit = fighter.inventoryClearCount();

        fixture.service().reconcile(fighter.player(), fixture.inside());

        assertFalse(fixture.service().hasDeposited(fighter.player()));
        assertEquals(inventoryClearCountWithTheKit, fighter.inventoryClearCount());
        assertFalse(Files.exists(fixture.inventoryFileOf(fighter)));
    }

    @Test
    void theShutdownLeavesAPlayerWithTheKitAlone() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        fighter.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();
        harness.startMatch(fighter, mate, opponent, opponentMate);
        int inventoryClearCountWithTheKit = fighter.inventoryClearCount();

        fixture.service().giveEveryInventoryBackQuietly();

        assertTrue(fixture.service().hasDeposited(fighter.player()));
        assertEquals(inventoryClearCountWithTheKit, fighter.inventoryClearCount());
        AreaInventoryLookup savedInventory = fixture.findSavedInventory(fighter);
        assertArrayEquals(AreaInventoryFixture.ownContents(), savedInventory.contents().orElseThrow());
    }

    @Test
    void theShutdownAfterTheMatchIsCancelledGivesTheFighterTheOwnInventoryBack() {
        ArenaMatchHarness.FakePlayer fighter = fixture.addPlayerOutsideWithItems("Fighter");
        ArenaMatchHarness.FakePlayer mate = fixture.addPlayerOutsideWithItems("Mate");
        ArenaMatchHarness.FakePlayer opponent = fixture.addPlayerOutsideWithItems("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = fixture.addPlayerOutsideWithItems("OpponentMate");
        fighter.moveTo(fixture.inside());
        fixture.service().reconcileOnlinePlayers();
        harness.startMatch(fighter, mate, opponent, opponentMate);

        harness.getMatchService().cancelActiveMatch();
        fixture.service().giveEveryInventoryBackQuietly();

        assertFalse(fixture.kitService().hasAppliedKit(fighter.player()));
        assertFalse(fixture.service().hasDeposited(fighter.player()));
        assertArrayEquals(AreaInventoryFixture.ownContents(), fighter.contents());
        assertEquals(17, fighter.level());
    }
}
