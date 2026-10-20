package de.simplepvparena.listener;

import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.support.FakeItemStack;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconnectWindowTest {

    private static final long RECONNECT_TICKS = 60L * ArenaMatchHarness.TICKS_PER_SECOND;
    private static final long ROUND_RESET_DELAY_TICKS = 100L;
    private static final String FIGHTER_LOST = "Fighter lost the connection, 60 seconds to come back";
    private static final String FIGHTER_BACK = "Fighter is back";
    private static final String FIGHTER_NOT_BACK = "Fighter did not come back and is eliminated";
    private static final String CONNECTION_LOST = "lost the connection";
    private static final String HALTED = "Match M1 is halted, no participant is online";

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(KitService::new);
        matchService = harness.getMatchService();
        matchService.setRoundResetDelayTicks(ROUND_RESET_DELAY_TICKS);
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void aFighterWhoLosesTheConnectionStaysAliveInTheRoundAndKeepsTheKit() {
        Match match = start();
        ItemStack[] inventoryAtTheQuit = new ItemStack[ArenaMatchHarness.INVENTORY_SLOT_COUNT];
        inventoryAtTheQuit[0] = new FakeItemStack("kit sword");
        fighter.setContents(inventoryAtTheQuit);
        int inventoryClearCountAtTheQuit = fighter.inventoryClearCount();

        disconnect(fighter);

        assertEquals(MatchState.RUNNING, match.getState());
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()), "the fighter counts as alive");
        assertTrue(match.getRoundParticipants().orElseThrow().getActivePlayers("Alpha").contains(fighter.uniqueId()));
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(1, count(opponent, FIGHTER_LOST));
        assertEquals(1, count(mate, FIGHTER_LOST));
        assertArrayEquals(inventoryAtTheQuit, fighter.contents(), "nothing is cleared while the window is open");
        assertEquals(inventoryClearCountAtTheQuit, fighter.inventoryClearCount());
        assertTrue(harness.getKitService().hasAppliedKit(fighter.player()), "the kit marker stays");
    }

    @Test
    void withZeroSecondsAFighterWhoLeavesIsEliminatedAtOnceAsBefore() {
        matchService.setReconnectSeconds(0);
        Match match = start();

        disconnect(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(0, countContaining(opponent, CONNECTION_LOST));
        assertEquals(MatchState.RUNNING, match.getState(), "the mate still fights");

        disconnect(mate);

        assertEquals("Alpha 0 : 1 Beta", match.getScore().describe());
    }

    @Test
    void theWindowLastsTheConfiguredSecondsAndThenEliminatesTheFighter() {
        Match match = start();
        disconnect(fighter);

        harness.advanceTicks(RECONNECT_TICKS - 1);

        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));

        harness.advanceTicks(1);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(1, count(opponent, FIGHTER_NOT_BACK));
        assertEquals(MatchState.RUNNING, match.getState(), "the mate still fights");
    }

    @Test
    void aFighterWhoComesBackInTimeStaysAFighterAndIsAnnounced() {
        start();
        disconnect(fighter);
        harness.advanceSeconds(30);

        assertTrue(reconnect(fighter));

        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(1, count(opponent, FIGHTER_BACK));
        assertEquals(1, count(fighter, FIGHTER_BACK));

        harness.advanceSeconds(60);

        assertTrue(matchService.isActiveFighter(fighter.uniqueId()), "the deadline is gone");
        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));
    }

    @Test
    void theTeamOfAnAbsentFighterDoesNotLoseWhileTheWindowIsOpen() {
        Match match = start();
        disconnect(fighter);

        eliminate(mate);

        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals("Alpha 0 : 0 Beta", match.getScore().describe());

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals("Alpha 0 : 1 Beta", match.getScore().describe(), "the deadline decides the round");
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        List<String> lines = opponent.messages();
        assertTrue(lines.indexOf(FIGHTER_NOT_BACK) >= 0, lines.toString());
        assertTrue(lines.indexOf(FIGHTER_NOT_BACK) < lines.indexOf("Beta scores a point (Alpha 0 : 1 Beta)"),
                lines.toString());
    }

    @Test
    void theOpponentsCanFallWhileTheFighterIsAway() {
        Match match = start();
        disconnect(fighter);

        eliminate(opponent);
        eliminate(opponentMate);

        assertEquals("Alpha 1 : 0 Beta", match.getScore().describe());
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()), "the deadline runs on between rounds");
    }

    @Test
    void aFighterWhoIsBackBeforeTheNextRoundFightsInIt() {
        Match match = start();
        disconnect(fighter);
        eliminate(opponent);
        eliminate(opponentMate);
        harness.advanceSeconds(2);

        assertFalse(reconnect(fighter), "the round of the window is over");

        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(0, count(opponent, FIGHTER_BACK));

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber());
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
    }

    @Test
    void aFighterStillAwayWhenTheNextRoundStartsMissesItAndTheWindowCloses() {
        Match match = start();
        disconnect(fighter);
        eliminate(opponent);
        eliminate(opponentMate);

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber());
        assertEquals(MatchState.RUNNING, match.getState());
        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));
        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals(2, match.getRoundNumber());
    }

    @Test
    void twoFightersOfOneTeamCanBeAwayAtTheSameTimeEachWithTheirOwnDeadline() {
        Match match = start();
        disconnect(fighter);
        harness.advanceSeconds(10);
        disconnect(mate);

        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertTrue(matchService.isWaitingForReconnect(mate.uniqueId()));
        assertEquals(1, count(opponent, "Mate lost the connection, 60 seconds to come back"));

        harness.advanceSeconds(50);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertTrue(matchService.isActiveFighter(mate.uniqueId()));
        assertEquals(MatchState.RUNNING, match.getState());

        harness.advanceSeconds(10);

        assertEquals(1, count(opponent, "Mate did not come back and is eliminated"));
        assertEquals("Alpha 0 : 1 Beta", match.getScore().describe());
    }

    @Test
    void aFighterWhoLeavesBetweenTheRoundsGetsNoWindow() {
        Match match = start();
        eliminate(opponent);
        eliminate(opponentMate);
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());

        disconnect(fighter);

        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(0, countContaining(opponent, CONNECTION_LOST));

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber());
        assertFalse(matchService.isActiveFighter(fighter.uniqueId()), "missing in the next round as before");
    }

    @Test
    void theMatchIsHaltedOnceEveryoneIsAwayAndNoWindowIsLeft() {
        ArenaMatchHarness.FakePlayer spectator = addSpectator();
        Match match = start();
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            disconnect(participant);
        }

        assertEquals(MatchState.RUNNING, match.getState(), "four open windows keep the round going");
        assertEquals(0, countContaining(spectator, HALTED));

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals("Alpha 0 : 1 Beta", match.getScore().describe(), "the first deadlines decide the round");
        assertEquals(1, count(spectator, FIGHTER_NOT_BACK));
        assertEquals(1, count(spectator, "Mate did not come back and is eliminated"));
        assertEquals(0, count(spectator, "Opponent did not come back and is eliminated"),
                "a deadline that runs out between the rounds eliminates nobody");
        assertNoWindowIsOpen();
        assertEquals(0, countContaining(spectator, HALTED));

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(1, countContaining(spectator, HALTED));
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertTrue(matchService.hasActiveMatch());
    }

    @Test
    void aFighterWhoLeavesWhileTheMatchIsHaltedGetsNoWindow() {
        ArenaMatchHarness.FakePlayer spectator = addSpectator();
        Match match = start();
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            disconnect(participant);
        }
        harness.advanceTicks(RECONNECT_TICKS + ROUND_RESET_DELAY_TICKS);
        assertEquals(1, countContaining(spectator, HALTED));
        assertFalse(reconnect(fighter));
        spectator.forgetReceivedTexts();

        disconnect(fighter);

        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(0, countContaining(spectator, CONNECTION_LOST));
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
    }

    @Test
    void aSpectatorAnAdminAndAPlayerOutsideOfTheMatchGetNoWindow() {
        ArenaMatchHarness.FakePlayer spectator = addSpectator();
        ArenaMatchHarness.FakePlayer admin = harness.addPlayer("Admin");
        admin.makeOperator();
        ArenaMatchHarness.FakePlayer visitor = harness.addPlayer("Visitor");
        Match match = start();

        for (ArenaMatchHarness.FakePlayer player : List.of(spectator, admin, visitor)) {
            disconnect(player);

            assertFalse(matchService.isWaitingForReconnect(player.uniqueId()), player.name());
        }

        assertEquals(0, countContaining(opponent, CONNECTION_LOST));
        assertEquals(MatchState.RUNNING, match.getState());
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            assertTrue(matchService.isActiveFighter(participant.uniqueId()), participant.name());
        }
    }

    @Test
    void anEliminatedFighterGetsNoWindow() {
        start();
        eliminate(fighter);

        disconnect(fighter);

        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(0, countContaining(opponent, CONNECTION_LOST));
    }

    @Test
    void aFighterWhoComesBackAndLeavesAgainInTheSameRoundIsEliminatedAtOnce() {
        start();
        disconnect(fighter);
        harness.advanceSeconds(50);
        assertTrue(reconnect(fighter));
        harness.advanceSeconds(5);

        disconnect(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(matchService.isWaitingForReconnect(fighter.uniqueId()));
        assertEquals(1, count(opponent, FIGHTER_LOST));

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));
    }

    @Test
    void aReloadKeepsTheDeadlineOfAnOpenWindowAndAppliesToTheNextLoss() {
        start();
        disconnect(fighter);

        matchService.setReconnectSeconds(10);
        disconnect(mate);

        assertEquals(1, count(opponent, "Mate lost the connection, 10 seconds to come back"));

        harness.advanceSeconds(10);

        assertFalse(matchService.isActiveFighter(mate.uniqueId()));
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()), "the window keeps its sixty seconds");

        harness.advanceSeconds(50);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
    }

    @Test
    void suddenDeathSkipsAFighterWhoIsAway() {
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        List<String> damagedPlayers = new ArrayList<>();
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            participant.reactToDamageWith(() -> damagedPlayers.add(participant.name()));
        }
        start();
        disconnect(fighter);

        for (int second = 0; second < 3; second++) {
            harness.runRepeatingTasks();
        }

        assertFalse(damagedPlayers.contains("Fighter"), damagedPlayers.toString());
        assertTrue(damagedPlayers.containsAll(List.of("Mate", "Opponent", "OpponentMate")),
                damagedPlayers.toString());
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));
    }

    @Test
    void theEndOfTheMatchClosesEveryWindow() {
        Match match = start();
        eliminate(opponent);
        eliminate(opponentMate);
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        disconnect(fighter);

        eliminate(opponent);
        eliminate(opponentMate);

        assertEquals(MatchState.FINISHED, match.getState());
        assertFalse(matchService.hasActiveMatch());
        assertNoWindowIsOpen();

        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));
    }

    @Test
    void aForcedResultClosesEveryWindow() {
        Match match = start();
        disconnect(fighter);

        assertTrue(matchService.forceResult(match.getMatchId(), "Beta"));

        assertNoWindowIsOpen();
        harness.advanceTicks(RECONNECT_TICKS);
        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));
    }

    @Test
    void aCancelledMatchClosesEveryWindowAndNothingReachesTheNextMatch() {
        start();
        disconnect(fighter);
        disconnect(opponent);

        matchService.cancelActiveMatch();

        assertNoWindowIsOpen();

        fighter.setOnline(true);
        opponent.setOnline(true);
        Match nextMatch = start();
        harness.advanceTicks(RECONNECT_TICKS);

        assertEquals(MatchState.RUNNING, nextMatch.getState());
        assertTrue(matchService.isActiveFighter(fighter.uniqueId()));
        assertTrue(matchService.isActiveFighter(opponent.uniqueId()));
        assertEquals(0, count(opponent, FIGHTER_NOT_BACK));
    }

    private Match start() {
        return harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    private ArenaMatchHarness.FakePlayer addSpectator() {
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        harness.getSpectatorService().sendToTribune(spectator.player());
        return spectator;
    }

    private void disconnect(ArenaMatchHarness.FakePlayer player) {
        matchService.handlePlayerDisconnected(player.player());
        player.setOnline(false);
    }

    private boolean reconnect(ArenaMatchHarness.FakePlayer player) {
        player.setOnline(true);
        return matchService.handlePlayerReturned(player.player());
    }

    private void eliminate(ArenaMatchHarness.FakePlayer player) {
        matchService.handlePlayerEliminated(player.uniqueId());
    }

    private void assertNoWindowIsOpen() {
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            assertFalse(matchService.isWaitingForReconnect(participant.uniqueId()), participant.name());
        }
    }

    private static int count(ArenaMatchHarness.FakePlayer receiver, String line) {
        return Collections.frequency(receiver.messages(), line);
    }

    private static int countContaining(ArenaMatchHarness.FakePlayer receiver, String part) {
        return (int) receiver.messages().stream().filter(message -> message.contains(part)).count();
    }
}
