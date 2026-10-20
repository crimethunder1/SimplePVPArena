package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.kit.FilePlayerStateSnapshotStorage;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeItemStackCodec;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerConnectionReconnectTest {

    private static final long RECONNECT_TICKS = 60L * ArenaMatchHarness.TICKS_PER_SECOND;
    private static final double TRIBUNE_COORDINATE = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final String REJOINED = "You rejoined during match M1, you take part again from the next round on";
    private static final String FIGHTER_BACK = "Fighter is back";

    @TempDir
    Path snapshotDirectory;

    private final List<String> warnings = new ArrayList<>();

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private KitService kitService;
    private PlayerConnectionListener listener;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(this::createKitService);
        matchService = harness.getMatchService();
        kitService = harness.getKitService();
        listener = new PlayerConnectionListener(matchService, harness.getSpectatorService(),
                harness.getArenaRegistry(), new BorderSelectorModeRegistry(), kitService);
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
        fighter.setContents(privateContents());
        fighter.setLevel(30);
    }

    @Test
    void aFighterWhoComesBackInTimeKeepsPlaceKitAndInventoryAndIsAnnounced() {
        assertTrue(harness.getArenaRegistry().isTeleportOnJoin(), "joining players are sent to the tribune");
        assertTrue(harness.getArenaRegistry().isArenaFullyConfigured());
        start();
        Location placeOfTheQuit = harness.at(4.5D, 6.5D);
        fighter.moveTo(placeOfTheQuit);
        ItemStack[] inventoryOfTheFight = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        inventoryOfTheFight[0] = new FakeItemStack("kit sword");
        fighter.setContents(inventoryOfTheFight);
        int inventoryClearCount = fighter.inventoryClearCount();
        int roundStartStepCount = harness.getRoundStartSteps().size();

        quit(fighter);
        harness.advanceSeconds(30);
        join(fighter);

        assertEquals(placeOfTheQuit, fighter.location(), "no teleport, not even to the tribune");
        assertEquals(roundStartStepCount, harness.getRoundStartSteps().size(), "no teleport and no new kit");
        assertArrayEquals(inventoryOfTheFight, fighter.contents());
        assertEquals(inventoryClearCount, fighter.inventoryClearCount());
        assertEquals(0, fighter.level(), "the level of the kit stays");
        assertTrue(kitService.hasAppliedKit(fighter.player()));
        assertTrue(Files.isRegularFile(snapshotFileOf(fighter)), "the saved state waits for the end of the match");
        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(fighter.uniqueId()));
        assertEquals(0, count(fighter, REJOINED));
        assertEquals(1, count(opponent, FIGHTER_BACK));
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void aFighterWhoComesBackAfterTheDeadlineIsRestoredAndSentToTheTribune() {
        start();
        fighter.moveTo(harness.at(4.5D, 6.5D));

        quit(fighter);
        harness.advanceTicks(RECONNECT_TICKS);
        join(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertArrayEquals(privateContents(), fighter.contents(), "the inventory from before the match is back");
        assertEquals(30, fighter.level());
        assertFalse(kitService.hasAppliedKit(fighter.player()));
        assertFalse(Files.exists(snapshotFileOf(fighter)));
        assertAtTheTribune(fighter);
        assertEquals(1, count(fighter, REJOINED));
        assertEquals(0, count(opponent, FIGHTER_BACK));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void aFighterWhoComesBackAfterTheRoundEndedJoinsTheTribuneAndTheNextRound() {
        Match match = start();
        quit(fighter);
        matchService.handlePlayerEliminated(opponent.uniqueId());
        matchService.handlePlayerEliminated(opponentMate.uniqueId());

        join(fighter);

        assertAtTheTribune(fighter);
        assertEquals(1, count(fighter, REJOINED));
        assertEquals(0, count(opponent, FIGHTER_BACK));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));

        harness.advanceTicks(100L);

        assertEquals(2, match.getRoundNumber());
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
    }

    @Test
    void aFighterWhoseMatchEndedWhileAwayIsRestoredOnTheJoin() {
        Match match = start();
        quit(fighter);
        assertTrue(matchService.forceResult(match.getMatchId(), "Beta"));
        assertTrue(kitService.hasAppliedKit(fighter.player()), "the end of the match cannot reach an offline player");

        join(fighter);

        assertArrayEquals(privateContents(), fighter.contents());
        assertEquals(30, fighter.level());
        assertFalse(kitService.hasAppliedKit(fighter.player()));
        assertAtTheTribune(fighter);
        assertEquals(0, count(fighter, REJOINED), "there is no match any more");
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void withZeroSecondsTheQuitEliminatesAtOnceAndTheJoinIsAsBefore() {
        matchService.setReconnectSeconds(0);
        start();

        quit(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));

        join(fighter);

        assertArrayEquals(privateContents(), fighter.contents());
        assertAtTheTribune(fighter);
        assertEquals(1, count(fighter, REJOINED));
        assertEquals(0, count(opponent, FIGHTER_BACK));
    }

    @Test
    void otherJoiningPlayersAreStillSentToTheTribuneWhileAWindowIsOpen() {
        start();
        quit(fighter);
        ArenaMatchHarness.FakePlayer visitor = harness.addPlayer("Visitor");
        visitor.moveTo(harness.at(300.5D, 40.5D));

        join(visitor);

        assertAtTheTribune(visitor);
        assertEquals(Optional.of(SpectatorMode.TRIBUNE),
                harness.getSpectatorService().findSpectatorMode(visitor.uniqueId()));
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
    }

    @Test
    void aSpectatorWhoQuitsLosesTheTribuneAssignmentAndGetsNoWindow() {
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        harness.getSpectatorService().sendToTribune(spectator.player());
        start();

        quit(spectator);

        assertEquals(Optional.empty(), harness.getSpectatorService().findSpectatorMode(spectator.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(spectator.uniqueId()));
    }

    private Match start() {
        return harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    private void quit(ArenaMatchHarness.FakePlayer player) {
        listener.onPlayerQuit(new PlayerQuitEvent(player.player(), Component.empty(),
                PlayerQuitEvent.QuitReason.DISCONNECTED));
        player.setOnline(false);
    }

    private void join(ArenaMatchHarness.FakePlayer player) {
        player.setOnline(true);
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));
    }

    private void assertAtTheTribune(ArenaMatchHarness.FakePlayer player) {
        assertEquals(TRIBUNE_COORDINATE, player.location().getX());
        assertEquals(TRIBUNE_COORDINATE, player.location().getZ());
    }

    private KitService createKitService(Plugin plugin) {
        return new KitService(plugin, new FilePlayerStateSnapshotStorage(snapshotDirectory, new FakeItemStackCodec()),
                warnings::add);
    }

    private Path snapshotFileOf(ArenaMatchHarness.FakePlayer player) {
        return snapshotDirectory.resolve(player.uniqueId() + ".yml");
    }

    private static int count(ArenaMatchHarness.FakePlayer receiver, String line) {
        return Collections.frequency(receiver.messages(), line);
    }

    private static ItemStack[] privateContents() {
        ItemStack[] contents = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        contents[0] = new FakeItemStack("diamond sword");
        contents[38] = new FakeItemStack("elytra");
        return contents;
    }
}
