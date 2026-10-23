package de.simplepvparena.listener;

import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerConnectionKitMarkerTest {

    private ArenaMatchHarness harness;
    private KitService kitService;
    private PlayerConnectionListener listener;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(KitService::new);
        harness.forgetTribuneSpawn();
        kitService = harness.getKitService();
        listener = new PlayerConnectionListener(harness.getMatchService(), harness.getSpectatorService(),
                harness.getArenaRegistry(), new BorderSelectorModeRegistry(), kitService,
                harness.getPlugin().getLogger());
    }

    @Test
    void clearsTheKitOfAPlayerWhoJoinsWithTheMarkerOutsideOfAFight() {
        ArenaMatchHarness.FakePlayer returner = harness.addPlayer("Returner");
        kitService.applyKit(returner.player());
        int inventoryClearCountBeforeTheJoin = returner.inventoryClearCount();

        join(returner);

        assertFalse(kitService.hasAppliedKit(returner.player()), "the marker is gone after the join");
        assertEquals(inventoryClearCountBeforeTheJoin + 1, returner.inventoryClearCount(),
                "the kit left over from a crash is cleared");
    }

    @Test
    void clearsTheKitOfAFighterWhoRejoinsAfterBeingEliminated() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.getMatchService().handlePlayerEliminated(fighter.uniqueId());
        assertFalse(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        int inventoryClearCountBeforeTheJoin = fighter.inventoryClearCount();

        join(fighter);

        assertFalse(kitService.hasAppliedKit(fighter.player()));
        assertEquals(inventoryClearCountBeforeTheJoin + 1, fighter.inventoryClearCount());
    }

    @Test
    void leavesTheKitOfAnActiveFighterAlone() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        harness.startMatch(fighter, mate, opponent, opponentMate);
        assertTrue(harness.getMatchService().isActiveFighter(fighter.uniqueId()));
        int inventoryClearCountBeforeTheJoin = fighter.inventoryClearCount();

        join(fighter);

        assertTrue(kitService.hasAppliedKit(fighter.player()), "an active fighter keeps the marker");
        assertEquals(inventoryClearCountBeforeTheJoin, fighter.inventoryClearCount(),
                "and keeps the kit");
    }

    @Test
    void leavesAPlayerWithoutTheMarkerAndWithoutATeamAlone() {
        ArenaMatchHarness.FakePlayer visitor = harness.addPlayer("Visitor");
        ArenaMatchHarness.FakePlayer admin = harness.addPlayer("Admin");
        admin.makeOperator();

        for (ArenaMatchHarness.FakePlayer player : List.of(visitor, admin)) {
            join(player);

            assertFalse(kitService.hasAppliedKit(player.player()));
            assertEquals(0, player.inventoryClearCount(), "the inventory of " + player.player().getName()
                    + " is never cleared");
        }
    }

    @Test
    void leavesATeamMemberWithoutTheMarkerAlone() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        mate.setOnline(false);
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        assertTrue(match.containsPlayer(mate.uniqueId()));
        assertFalse(kitService.hasAppliedKit(mate.player()), "the mate was offline when the round started");
        mate.setOnline(true);

        join(mate);

        assertFalse(kitService.hasAppliedKit(mate.player()));
        assertEquals(0, mate.inventoryClearCount(), "the inventory of the team member is never cleared");
    }

    @Test
    void removesTheMarkerOfEveryParticipantAtTheEndOfTheMatch() {
        ArenaMatchHarness.FakePlayer fighter = harness.addPlayer("Fighter");
        ArenaMatchHarness.FakePlayer mate = harness.addPlayer("Mate");
        ArenaMatchHarness.FakePlayer opponent = harness.addPlayer("Opponent");
        ArenaMatchHarness.FakePlayer opponentMate = harness.addPlayer("OpponentMate");
        List<ArenaMatchHarness.FakePlayer> participants = List.of(fighter, mate, opponent, opponentMate);
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        for (ArenaMatchHarness.FakePlayer participant : participants) {
            assertTrue(kitService.hasAppliedKit(participant.player()));
        }

        assertTrue(harness.getMatchService().forceResult(match.getMatchId(), match.getFirstTeam().getTeamName()));

        for (ArenaMatchHarness.FakePlayer participant : participants) {
            assertFalse(kitService.hasAppliedKit(participant.player()),
                    "the marker of " + participant.player().getName() + " is removed at the end of the match");
        }
    }

    private void join(ArenaMatchHarness.FakePlayer player) {
        listener.onPlayerJoin(new PlayerJoinEvent(player.player(), Component.empty()));
    }
}
