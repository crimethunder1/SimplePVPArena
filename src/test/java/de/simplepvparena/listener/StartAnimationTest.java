package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.message.TournamentMessages;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartAnimationTest {

    private static final long SECOND = ArenaMatchHarness.TICKS_PER_SECOND;
    private static final long ROUND_RESET_DELAY_TICKS = 100L;
    private static final int GATE_X = 2;
    private static final int GATE_Z = 3;
    private static final String GATE_COUNTING = "The gate comes down layer by layer";
    private static final String SUDDEN_DEATH_STARTED = "The time limit of round 1 in match M1 is over, sudden death "
            + "has started";
    private static final Title.Times SECOND_TITLE_TIMES = Title.Times.times(Duration.ZERO, Duration.ofMillis(800L),
            Duration.ofMillis(200L));
    private static final Title.Times RELEASE_TITLE_TIMES = Title.Times.times(Duration.ZERO,
            Duration.ofMillis(1200L), Duration.ofMillis(400L));

    private ArenaMatchHarness harness;
    private MatchService matchService;
    private RecordingRoundStartEffects effects;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        matchService = harness.getMatchService();
        matchService.setCountdownSeconds(3);
        matchService.setRoundResetDelayTicks(ROUND_RESET_DELAY_TICKS);
        effects = harness.getRoundStartEffects();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void withoutAGateTheFightersAreBlindAndReadThreeTwoOneAndThenFight() {
        start();

        assertTrue(matchService.isStartAnimationRunning());
        assertEquals(List.of("Fighter", "Mate", "Opponent", "OpponentMate"), effects.blindPlayerNames());
        assertTrue(effects.events().contains("blind Fighter for 80 ticks"), effects.events().toString());
        assertEquals(List.of("3 / "), fighter.titleTexts());
        assertTrue(matchService.findRoundTimer().isEmpty(), "the time limit waits for the fight");

        harness.advanceTicks(SECOND);
        assertEquals(List.of("3 / ", "2 / "), fighter.titleTexts());
        harness.advanceTicks(SECOND);
        assertEquals(List.of("3 / ", "2 / ", "1 / "), opponent.titleTexts());
        harness.advanceTicks(SECOND - 1);
        assertTrue(matchService.isStartAnimationRunning());
        assertTrue(effects.isBlind(fighter));

        harness.advanceTicks(1);

        assertEquals(List.of("3 / ", "2 / ", "1 / ", "Fight! / "), fighter.titleTexts());
        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        for (ArenaMatchHarness.FakePlayer participant : List.of(fighter, mate, opponent, opponentMate)) {
            assertEquals(1, Collections.frequency(effects.events(), "totem for " + participant.name()),
                    participant.name());
        }
        assertTrue(matchService.findRoundTimer().isPresent());
    }

    @Test
    void theOwnTitlesHaveTheFormAndTheTimesOfTheGateCountdown() {
        start();
        harness.advanceSeconds(3);

        List<Title> titles = fighter.titles();

        assertEquals(4, titles.size());
        for (Title secondTitle : titles.subList(0, 3)) {
            assertEquals(SECOND_TITLE_TIMES, secondTitle.times());
            assertEquals(TournamentMessages.WARNING_COLOUR, secondTitle.title().color());
        }
        assertEquals(RELEASE_TITLE_TIMES, titles.get(3).times());
        assertEquals(TournamentMessages.SUCCESS_COLOUR, titles.get(3).title().color());
    }

    @Test
    void withAGateTheAnimationIsTheLastThreeSecondsOfTheGateCountdown() {
        markGateBlock();
        start();

        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(List.of("10 / " + GATE_COUNTING), fighter.titleTexts());

        harness.advanceTicksWithRepeatingTasks(7 * SECOND - 1);

        assertFalse(matchService.isStartAnimationRunning(), "the first seven seconds of the gate are as before");
        assertEquals(List.of(), effects.blindPlayerNames());
        assertTrue(matchService.findRoundTimer().isEmpty(), "the gate seconds no longer count on the time limit");

        harness.advanceTicksWithRepeatingTasks(1);

        assertTrue(matchService.isStartAnimationRunning());
        assertTrue(effects.events().contains("blind Fighter for 80 ticks"), effects.events().toString());
        assertEquals("3 / " + GATE_COUNTING, last(fighter.titleTexts()));

        harness.advanceTicksWithRepeatingTasks(3 * SECOND);

        assertEquals(List.of("10", "9", "8", "7", "6", "5", "4", "3", "2", "1", "Fight!"), titleHeadlines(fighter),
                "every title still comes from the gate, once");
        assertEquals("Fight! / The gate is open", last(fighter.titleTexts()));
        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(1, Collections.frequency(effects.events(), "totem for Fighter"));
        assertTrue(matchService.findRoundTimer().isPresent());
        assertEquals(Material.AIR, harness.findMaterial(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z));
    }

    @Test
    void aGateShorterThanTheCountdownIsBlindForTheWholeGate() {
        matchService.setCountdownSeconds(5);
        harness.getGateDuration().setSeconds(2);
        markGateBlock();

        start();

        assertTrue(matchService.isStartAnimationRunning());
        assertTrue(effects.events().contains("blind Fighter for 60 ticks"), effects.events().toString());

        harness.advanceTicksWithRepeatingTasks(2 * SECOND);

        assertEquals(List.of("2", "1", "Fight!"), titleHeadlines(fighter));
        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertTrue(matchService.findRoundTimer().isPresent());
    }

    @Test
    void withZeroSecondsTheRoundStartsAsBefore() {
        matchService.setCountdownSeconds(0);

        start();

        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.events());
        assertEquals(List.of(), fighter.titleTexts());
        assertTrue(matchService.findRoundTimer().isPresent(), "the time limit starts with the round");
    }

    @Test
    void withZeroSecondsAndAGateTheGateSecondsStillCountOnTheTimeLimit() {
        matchService.setCountdownSeconds(0);
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        markGateBlock();
        start();

        harness.advanceTicksWithRepeatingTasks(SECOND);

        assertEquals(1, count(fighter, SUDDEN_DEATH_STARTED), "as before, during the gate");
        assertEquals(List.of(), effects.events());
    }

    @Test
    void suddenDeathComesTheCountdownSecondsLaterThanBefore() {
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        start();

        harness.advanceTicksWithRepeatingTasks(4 * SECOND - 1);

        assertEquals(0, count(fighter, SUDDEN_DEATH_STARTED));

        harness.advanceTicksWithRepeatingTasks(1);

        assertEquals(1, count(fighter, SUDDEN_DEATH_STARTED));
    }

    @Test
    void withAGateTheTimeLimitStartsWithFightInsteadOfWithTheGate() {
        matchService.setRoundTimeoutSettings(new RoundTimeoutSettings(1, 0, 2.0D, 1.0D));
        markGateBlock();
        start();

        harness.advanceTicksWithRepeatingTasks(11 * SECOND - 1);

        assertEquals(0, count(fighter, SUDDEN_DEATH_STARTED));

        harness.advanceTicksWithRepeatingTasks(1);

        assertEquals(1, count(fighter, SUDDEN_DEATH_STARTED));
    }

    @Test
    void theFinalWithoutSuddenDeathGetsTheAnimationAndNoTimeLimit() {
        harness.startFinalMatch(fighter, mate, opponent, opponentMate);

        assertTrue(effects.isBlind(fighter));

        harness.advanceSeconds(3);

        assertFalse(effects.isBlind(fighter));
        assertTrue(matchService.findRoundTimer().isEmpty());
    }

    @Test
    void aFighterWhoFallsBeforeTheFightIsNoLongerBlindAndGetsNoTotem() {
        start();
        harness.advanceTicks(SECOND);

        eliminate(fighter);

        assertFalse(effects.isBlind(fighter));
        assertTrue(effects.isBlind(mate));
        assertTrue(effects.events().contains("remove the blindness of Fighter"));

        harness.advanceSeconds(2);

        assertEquals(0, Collections.frequency(effects.events(), "totem for Fighter"));
        assertEquals(1, Collections.frequency(effects.events(), "totem for Mate"));
        assertEquals(1, Collections.frequency(effects.events(), "remove the blindness of Fighter"));
    }

    @Test
    void aFighterWhoQuitsWithoutAWindowIsNoLongerBlind() {
        matchService.setReconnectSeconds(0);
        start();

        disconnect(fighter);

        assertFalse(matchService.isActiveFighter(fighter.uniqueId()));
        assertFalse(effects.isBlind(fighter));
    }

    @Test
    void aFighterWhoLosesTheConnectionIsNoLongerBlindAndIsBlindAgainWhenBackDuringTheCountdown() {
        start();
        harness.advanceTicks(SECOND);

        disconnect(fighter);

        assertFalse(effects.isBlind(fighter), "nothing is left on a player who is away");
        assertTrue(matchService.isWaitingForReconnect(fighter.uniqueId()));

        harness.advanceTicks(SECOND);
        assertTrue(reconnect(fighter));

        assertTrue(effects.isBlind(fighter));
        assertEquals("blind Fighter for 40 ticks", last(effects.events()), "the rest of the countdown and a second");

        harness.advanceTicks(SECOND);

        assertFalse(effects.isBlind(fighter));
        assertEquals(1, Collections.frequency(effects.events(), "totem for Fighter"));
    }

    @Test
    void aFighterWhoIsBackAfterTheFightStartedIsNotBlind() {
        start();
        harness.advanceTicks(SECOND);
        disconnect(fighter);
        harness.advanceSeconds(3);
        effects.forgetEvents();

        assertTrue(reconnect(fighter));

        assertFalse(effects.isBlind(fighter));
        assertEquals(List.of(), effects.events());
    }

    @Test
    void aFighterWhoIsAwayAtTheFightGetsNoTotem() {
        start();
        disconnect(fighter);

        harness.advanceSeconds(3);

        assertEquals(0, Collections.frequency(effects.events(), "totem for Fighter"));
        assertEquals(1, Collections.frequency(effects.events(), "totem for Mate"));
    }

    @Test
    void aRoundThatEndsBeforeTheFightStopsTheCountdownAndTheBlindness() {
        Match match = start();
        harness.advanceTicks(SECOND);

        eliminate(opponent);
        eliminate(opponentMate);

        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(1, harness.getPendingTaskCount(), "only the next round is left");
        int titleCount = fighter.titles().size();

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS - 1);

        assertEquals(titleCount, fighter.titles().size(), "no title of the stopped countdown");
        assertEquals(0, Collections.frequency(effects.events(), "totem for Fighter"));
        assertTrue(matchService.findRoundTimer().isEmpty());
    }

    @Test
    void theNextRoundStartsWithAWholeCountdownAgain() {
        Match match = start();
        harness.advanceSeconds(3);
        eliminate(opponent);
        eliminate(opponentMate);
        fighter.forgetReceivedTexts();

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber());
        assertTrue(matchService.isStartAnimationRunning());
        assertEquals(List.of("Fighter", "Mate", "Opponent", "OpponentMate"), effects.blindPlayerNames());
        assertEquals("3 / ", last(fighter.titleTexts()));
    }

    @Test
    void aCancelledMatchStopsTheCountdownAndLeavesNoTaskAndNoBlindness() {
        start();
        harness.advanceTicks(SECOND);

        matchService.cancelActiveMatch();

        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(0, harness.getPendingTaskCount());
        int titleCount = fighter.titles().size();
        harness.advanceSeconds(5);
        assertEquals(titleCount, fighter.titles().size());
        assertEquals(0, Collections.frequency(effects.events(), "totem for Fighter"));
    }

    @Test
    void aCancelledMatchWithAGateStopsTheGateAndTheAnimationTogether() {
        markGateBlock();
        start();
        harness.advanceTicksWithRepeatingTasks(8 * SECOND);
        assertTrue(effects.isBlind(fighter));

        matchService.cancelActiveMatch();

        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(0, harness.getPendingTaskCount());
        assertEquals(Material.BARRIER, harness.findMaterial(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z));
    }

    @Test
    void aForcedResultBeforeTheFightLeavesNoTaskAndNoBlindness() {
        Match match = start();

        assertTrue(matchService.forceResult(match.getMatchId(), "Beta"));

        assertEquals(MatchState.FINISHED, match.getState());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(0, harness.getPendingTaskCount());
    }

    @Test
    void theEndOfTheMatchBeforeTheFightOfTheSecondRoundLeavesNoTaskAndNoBlindness() {
        Match match = start();
        harness.advanceSeconds(3);
        eliminate(opponent);
        eliminate(opponentMate);
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        assertTrue(effects.isBlind(fighter));

        eliminate(opponent);
        eliminate(opponentMate);

        assertEquals(MatchState.FINISHED, match.getState());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertEquals(0, harness.getPendingTaskCount());
    }

    @Test
    void aSpectatorReadsTheTitlesButIsNeverBlind() {
        ArenaMatchHarness.FakePlayer spectator = harness.addPlayer("Spectator");
        harness.getSpectatorService().sendToTribune(spectator.player());

        start();
        harness.advanceSeconds(3);

        assertEquals(List.of("3 / ", "2 / ", "1 / ", "Fight! / "), spectator.titleTexts());
        assertTrue(effects.events().stream().noneMatch(event -> event.contains("Spectator")),
                effects.events().toString());
    }

    @Test
    void thePauseOfTheTournamentDoesNotStopTheCountdownAndTheTimeLimitStartsPaused() {
        start();

        matchService.pauseRoundTimer();
        harness.advanceSeconds(3);

        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.blindPlayerNames());
        assertTrue(matchService.findRoundTimer().orElseThrow().isPaused());
    }

    @Test
    void aReloadDuringTheCountdownAppliesFromTheNextRound() {
        Match match = start();

        matchService.setCountdownSeconds(0);

        assertTrue(matchService.isStartAnimationRunning());
        harness.advanceSeconds(3);
        assertEquals(1, Collections.frequency(effects.events(), "totem for Fighter"));
        eliminate(opponent);
        eliminate(opponentMate);
        effects.forgetEvents();

        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber());
        assertFalse(matchService.isStartAnimationRunning());
        assertEquals(List.of(), effects.events());
        assertTrue(matchService.findRoundTimer().isPresent());
    }

    private Match start() {
        return harness.startMatch(fighter, mate, opponent, opponentMate);
    }

    private void markGateBlock() {
        harness.setBlock(GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z, Material.BARRIER);
        harness.getGateMarks().mark(ArenaMatchHarness.WORLD_NAME, GATE_X, ArenaMatchHarness.GROUND_Y + 1, GATE_Z,
                Material.BARRIER);
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

    private static List<String> titleHeadlines(ArenaMatchHarness.FakePlayer player) {
        return player.titles().stream().map(title -> TournamentMessages.toPlainText(title.title())).toList();
    }

    private static int count(ArenaMatchHarness.FakePlayer receiver, String line) {
        return Collections.frequency(receiver.messages(), line);
    }

    private static String last(List<String> lines) {
        return lines.get(lines.size() - 1);
    }
}
