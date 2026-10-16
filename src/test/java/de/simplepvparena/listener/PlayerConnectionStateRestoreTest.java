package de.simplepvparena.listener;

import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.kit.FilePlayerStateSnapshotStorage;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeItemStackCodec;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerConnectionStateRestoreTest {

    @TempDir
    Path snapshotDirectory;

    private final List<String> warnings = new ArrayList<>();

    private ArenaMatchHarness harness;
    private KitService kitService;
    private PlayerConnectionListener listener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(this::createKitService);
        harness.forgetTribuneSpawn();
        kitService = harness.getKitService();
        listener = new PlayerConnectionListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), new BorderSelectorModeRegistry(), kitService);
    }

    @Test
    void restoresTheSavedStateOfAPlayerWithTheMarkerWhoJoinsOutsideOfAFightAfterARestart() {
        ArenaMatchHarness.FakePlayer returner = harness.addPlayer("Returner");
        ItemStack[] privateContents = privateContents();
        returner.setContents(privateContents);
        returner.setLevel(30);
        createKitService(harness.getPlugin()).applyKit(returner.player());
        assertEquals(0, returner.level(), "the kit replaced the private state before the restart");

        join(returner);

        assertArrayEquals(privateContents, returner.contents());
        assertEquals(30, returner.level());
        assertFalse(kitService.hasAppliedKit(returner.player()));
        assertFalse(Files.exists(snapshotFileOf(returner)));
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void leavesTheKitAndTheSnapshotOfAnActiveFighterAlone() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        fighter.setContents(privateContents());
        fighter.setLevel(30);
        harness.startMatch(fighter, mate, opponent, opponentMate);
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        ItemStack[] kitContents = fighter.contents();
        int inventoryClearCountBeforeTheJoin = fighter.inventoryClearCount();

        join(fighter);

        assertTrue(kitService.hasAppliedKit(fighter.player()), "an active fighter keeps the marker");
        assertArrayEquals(kitContents, fighter.contents(), "and keeps the kit");
        assertEquals(0, fighter.level());
        assertEquals(inventoryClearCountBeforeTheJoin, fighter.inventoryClearCount());
        assertTrue(Files.isRegularFile(snapshotFileOf(fighter)), "the snapshot stays for the end of the match");
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void doesNotClearAnEliminatedFighterAgainAtTheEndOfTheMatchAfterTheJoinRestoredTheState() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        ItemStack[] privateContents = privateContents();
        fighter.setContents(privateContents);
        fighter.setLevel(30);
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());
        join(fighter);
        ItemStack[] contentsAfterTheJoin = fighter.contents();
        assertArrayEquals(privateContents, contentsAfterTheJoin, "the join restored the private inventory");
        int inventoryClearCountAfterTheJoin = fighter.inventoryClearCount();

        assertTrue(harness.getMatchService().forceResult(match.getMatchId(), match.getFirstTeam().getTeamName()));

        assertArrayEquals(contentsAfterTheJoin, fighter.contents());
        assertEquals(30, fighter.level());
        assertEquals(inventoryClearCountAfterTheJoin, fighter.inventoryClearCount(),
                "the end of the match does not clear the inventory again");
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    private KitService createKitService(Plugin plugin) {
        return new KitService(plugin, new FilePlayerStateSnapshotStorage(snapshotDirectory, new FakeItemStackCodec()),
                warnings::add);
    }

    private Path snapshotFileOf(ArenaMatchHarness.FakePlayer player) {
        return snapshotDirectory.resolve(player.uniqueId() + ".yml");
    }

    private static ItemStack[] privateContents() {
        ItemStack[] contents = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        contents[0] = new FakeItemStack("diamond sword");
        contents[38] = new FakeItemStack("elytra");
        return contents;
    }

    private void join(ArenaMatchHarness.FakePlayer player) {
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));
    }
}
