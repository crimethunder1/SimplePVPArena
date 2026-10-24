package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.match.RoundCleaning;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.UseLanguage;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundCleaningTest {

    private static final long ROUND_RESET_DELAY_TICKS = 100L;
    private static final double TRIBUNE_COORDINATE = ArenaMatchHarness.WALL_MAXIMUM + 5.5D;
    private static final String WITHOUT_BASELINE = "The arena is not cleaned between the rounds, no baseline is stored";
    private static final String ALREADY_RUNNING = "The arena is already being cleaned";

    private final ArenaMatchHarness harness = new ArenaMatchHarness();
    private final RecordingRoundCleaning roundCleaning = new RecordingRoundCleaning();
    private final List<String> adminMessages = new ArrayList<>();
    private final List<String> completedMatches = new ArrayList<>();
    private final ArenaMatchHarness.FakePlayer alphaFighter = harness.addPlayer("AlphaFighter");
    private final ArenaMatchHarness.FakePlayer alphaMate = harness.addPlayer("AlphaMate");
    private final ArenaMatchHarness.FakePlayer betaFighter = harness.addPlayer("BetaFighter");
    private final ArenaMatchHarness.FakePlayer betaMate = harness.addPlayer("BetaMate");

    RoundCleaningTest() {
        MatchService matchService = harness.getMatchService();
        matchService.setRoundCleaning(roundCleaning);
        matchService.setAdminNotifier(adminMessages::add);
        matchService.setMatchCompletionHandler((match, winnerTeamName) ->
                completedMatches.add(match.getMatchId() + " " + winnerTeamName));
    }

    @Test
    void cleansTheArenaBetweenTwoRoundsAndStartsTheNextRoundOnlyAfterTheCleaningAndThePause() {
        Match match = startMatch();
        int clearsBeforeThePoint = alphaFighter.inventoryClearCount();

        betaWinsTheRound();

        assertEquals(1, roundCleaning.startCount);
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS * 5);
        assertEquals(1, match.getRoundNumber(), "no round begins while the arena is cleaned");

        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS - 1);
        assertEquals(1, match.getRoundNumber(), "the pause between the rounds runs after the cleaning");
        assertEquals(clearsBeforeThePoint, alphaFighter.inventoryClearCount(), "the kit is kept until the round");

        harness.advanceTicks(1);
        assertEquals(2, match.getRoundNumber());
        assertEquals(MatchState.RUNNING, match.getState());
        assertEquals(2.5D, alphaFighter.location().getX(), "the next round starts at the spawns as before");
        assertEquals(8.5D, betaFighter.location().getX());
        assertEquals(GameMode.SURVIVAL, alphaFighter.gameMode(), "and with the kit as before");
        assertTrue(alphaFighter.inventoryClearCount() > clearsBeforeThePoint);
        assertEquals(List.of(), adminMessages);
    }

    @Test
    void bringsTheLivingAndTheEliminatedFightersOfBothTeamsToTheTribuneWithoutTakingTheirKit() {
        startMatch();
        ArenaMatchHarness.FakePlayer bystander = harness.addPlayer("Bystander");
        Location insideTheFightArea = harness.at(5.5D, 5.5D);
        bystander.moveTo(insideTheFightArea);
        int clearsBeforeThePoint = alphaFighter.inventoryClearCount();

        betaWinsTheRound();

        for (ArenaMatchHarness.FakePlayer fighter : List.of(alphaFighter, alphaMate, betaFighter, betaMate)) {
            assertEquals(TRIBUNE_COORDINATE, fighter.location().getX(), fighter.name() + " is on the tribune");
            assertEquals(TRIBUNE_COORDINATE, fighter.location().getZ(), fighter.name() + " is on the tribune");
            assertEquals(GameMode.ADVENTURE, fighter.gameMode(), fighter.name());
        }
        assertEquals(clearsBeforeThePoint, alphaFighter.inventoryClearCount(), "nobody loses the kit");
        assertEquals(insideTheFightArea, bystander.location(), "a spectator in the fight area stays where it is");
    }

    @Test
    void aFighterWhoIsOfflineIsNotMovedAndIsMissingInTheNextRound() {
        Match match = startMatch();
        alphaMate.setOnline(false);
        harness.getMatchService().handlePlayerDisconnected(alphaMate.player());
        Location lastLocation = alphaMate.location();

        betaWinsTheRound();
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(lastLocation, alphaMate.location());
        assertEquals(2, match.getRoundNumber());
        assertFalse(match.getRoundParticipants().orElseThrow().isActive(alphaMate.uniqueId()));
    }

    @Test
    void cleansTwiceInAMatchOfThreeRoundsAndNotAfterTheDecidingPoint() {
        startMatch();

        betaWinsTheRound();
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        alphaWinsTheRound();
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        alphaWinsTheRound();

        assertEquals(2, roundCleaning.startCount, "the point that decides the match starts no round cleaning");
        assertFalse(roundCleaning.isCleaning());
        assertEquals(List.of("M1 Alpha"), completedMatches, "the match ends through the completion handler");
        assertFalse(harness.getMatchService().hasActiveMatch());
    }

    @Test
    void aMatchDecidedTwoToNothingCleansOnceBetweenTheRounds() {
        startMatch();

        alphaWinsTheRound();
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        alphaWinsTheRound();

        assertEquals(1, roundCleaning.startCount);
        assertEquals(List.of("M1 Alpha"), completedMatches);
    }

    @Test
    void withoutABaselineTheRoundsGoOnAsBeforeAndTheAdminsAreToldOncePerMatch() {
        roundCleaning.baseline = false;
        Match match = startMatch();

        betaWinsTheRound();

        assertEquals(0, roundCleaning.startCount);
        assertEquals(2.5D, alphaFighter.location().getX(), "nobody is moved to the tribune");
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        assertEquals(2, match.getRoundNumber(), "the next round starts after the usual pause");

        alphaWinsTheRound();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(3, match.getRoundNumber());
        assertEquals(List.of(WITHOUT_BASELINE), adminMessages);
    }

    @Test
    void theNoticeComesAgainForTheNextMatch() {
        roundCleaning.baseline = false;
        startMatch();
        alphaWinsTheRound();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        alphaWinsTheRound();

        startMatch();
        alphaWinsTheRound();

        assertEquals(List.of(WITHOUT_BASELINE, WITHOUT_BASELINE), adminMessages);
    }

    @Test
    void aCleaningThatDoesNotStartLetsTheRoundsGoOnAsBeforeWithOneNoticeAndItsReason() {
        roundCleaning.refusal = ALREADY_RUNNING + TournamentMessages.LINE_BREAK + "Wait a moment";
        Match match = startMatch();

        betaWinsTheRound();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        alphaWinsTheRound();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(3, match.getRoundNumber());
        assertEquals(0, roundCleaning.startCount);
        assertEquals(List.of("The arena is not cleaned between the rounds: " + ALREADY_RUNNING), adminMessages);
    }

    @Test
    void switchedOffTheRoundsGoOnAsBeforeAndTheSwitchCountsFromTheNextEndOfARound() {
        Match match = startMatch();
        harness.getMatchService().setCleanBetweenRounds(false);

        betaWinsTheRound();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(0, roundCleaning.startCount);
        assertEquals(2, match.getRoundNumber());
        assertEquals(List.of(), adminMessages, "a switched off cleaning is no problem worth a notice");

        harness.getMatchService().setCleanBetweenRounds(true);
        alphaWinsTheRound();

        assertEquals(1, roundCleaning.startCount);
    }

    @Test
    void withoutARoundCleaningTheRoundsGoOnAsBefore() {
        ArenaMatchHarness plainHarness = new ArenaMatchHarness();
        ArenaMatchHarness.FakePlayer first = plainHarness.addPlayer("First");
        ArenaMatchHarness.FakePlayer firstMate = plainHarness.addPlayer("FirstMate");
        Match match = plainHarness.startMatch(first, firstMate, plainHarness.addPlayer("Second"),
                plainHarness.addPlayer("SecondMate"));

        plainHarness.getMatchService().handlePlayerEliminated(first.uniqueId());
        plainHarness.getMatchService().handlePlayerEliminated(firstMate.uniqueId());

        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertEquals(2.5D, first.location().getX(), "nobody is moved to the tribune");
        plainHarness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        assertEquals(2, match.getRoundNumber());
    }

    @Test
    void cancellingTheMatchDuringTheCleaningEndsItAndTheEndOfTheCleaningStartsNoRound() {
        Match match = startMatch();
        betaWinsTheRound();

        harness.getMatchService().cancelActiveMatch();
        assertEquals(MatchState.CANCELLED, match.getState());
        assertTrue(roundCleaning.isCleaning(), "the cleaning itself runs to its end");
        int stepsBeforeTheEnd = harness.getRoundStartSteps().size();

        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS * 2);

        assertEquals(1, match.getRoundNumber());
        assertFalse(harness.getMatchService().hasActiveMatch());
        assertEquals(stepsBeforeTheEnd, harness.getRoundStartSteps().size(), "no round step follows");
    }

    @Test
    void theEndOfTheCleaningOfACancelledMatchLeavesTheNextMatchAlone() {
        Match cancelledMatch = startMatch();
        betaWinsTheRound();
        harness.getMatchService().cancelActiveMatch();
        RecordingRoundCleaning.Run cleaningOfTheCancelledMatch = roundCleaning.takeRun();
        Match nextMatch = startMatch();
        betaWinsTheRound();
        assertEquals(MatchState.BETWEEN_ROUNDS, nextMatch.getState());

        cleaningOfTheCancelledMatch.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS * 2);

        assertEquals(1, nextMatch.getRoundNumber(), "the next match waits for its own cleaning");
        assertEquals(1, cancelledMatch.getRoundNumber());
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        assertEquals(2, nextMatch.getRoundNumber());
    }

    @Test
    void theRoundTimerAndSuddenDeathDoNotRunDuringTheCleaning() {
        startMatch();
        assertTrue(harness.getMatchService().findRoundTimer().isPresent());

        betaWinsTheRound();
        alphaFighter.forgetReceivedTexts();
        harness.advanceTicksWithRepeatingTasks(20L * 600L);

        assertTrue(harness.getMatchService().findRoundTimer().isEmpty());
        assertEquals(List.of(), alphaFighter.actionBars(), "no sudden death warning during the cleaning");
        assertEquals(20.0D, alphaFighter.health());
    }

    @Test
    void aReconnectWindowThatRunsOutDuringTheCleaningWorksAsBefore() {
        Match match = startMatch();
        alphaMate.setOnline(false);
        harness.getMatchService().handlePlayerDisconnected(alphaMate.player());
        assertTrue(harness.getMatchService().isWaitingForReconnect(alphaMate.uniqueId()));

        betaWinsTheRound();
        alphaFighter.forgetReceivedTexts();
        harness.advanceSeconds(harness.getMatchService().getReconnectSeconds() + 1L);

        assertFalse(harness.getMatchService().isWaitingForReconnect(alphaMate.uniqueId()));
        assertFalse(alphaFighter.messages().stream().anyMatch(message -> message.contains("did not come back")),
                alphaFighter.messages().toString());
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        assertEquals(2, match.getRoundNumber());
        assertFalse(match.getRoundParticipants().orElseThrow().isActive(alphaMate.uniqueId()));
    }

    @Test
    void aFighterWhoComesBackDuringTheCleaningFightsInTheNextRound() {
        Match match = startMatch();
        alphaMate.setOnline(false);
        harness.getMatchService().handlePlayerDisconnected(alphaMate.player());
        betaWinsTheRound();

        alphaMate.setOnline(true);
        harness.getMatchService().handlePlayerReturned(alphaMate.player());
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertTrue(match.getRoundParticipants().orElseThrow().isActive(alphaMate.uniqueId()));
    }

    @Test
    void aFighterWhoLeavesDuringTheCleaningIsMissingInTheNextRound() {
        Match match = startMatch();
        betaWinsTheRound();

        betaMate.setOnline(false);
        harness.getMatchService().handlePlayerDisconnected(betaMate.player());
        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber());
        assertFalse(match.getRoundParticipants().orElseThrow().isActive(betaMate.uniqueId()));
        assertTrue(match.getRoundParticipants().orElseThrow().isActive(betaFighter.uniqueId()));
        assertFalse(harness.getMatchService().isWaitingForReconnect(betaMate.uniqueId()));
    }

    @Test
    void aDrawStartsNoCleaning() {
        Match match = startMatch();
        betaWinsTheRound();
        for (ArenaMatchHarness.FakePlayer fighter : List.of(alphaFighter, alphaMate, betaFighter, betaMate)) {
            fighter.setOnline(false);
        }

        roundCleaning.finish();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);

        assertEquals(2, match.getRoundNumber(), "the second round begins without anybody online and is a draw");
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertEquals(1, roundCleaning.startCount, "the draw scores no point and starts no cleaning");
        assertFalse(roundCleaning.isCleaning());
    }

    @Test
    @UseLanguage("de")
    void writesTheNoticesInGerman() {
        roundCleaning.baseline = false;
        startMatch();
        alphaWinsTheRound();
        harness.advanceTicks(ROUND_RESET_DELAY_TICKS);
        alphaWinsTheRound();

        roundCleaning.baseline = true;
        roundCleaning.refusal = "Die Arena wird schon gereinigt";
        startMatch();
        alphaWinsTheRound();

        assertEquals(List.of("Die Arena wird zwischen den Runden nicht gereinigt, es ist kein Ausgangszustand "
                + "gespeichert", "Die Arena wird zwischen den Runden nicht gereinigt: Die Arena wird schon gereinigt"),
                adminMessages);
    }

    private Match startMatch() {
        return harness.startMatch(alphaFighter, alphaMate, betaFighter, betaMate);
    }

    private void alphaWinsTheRound() {
        harness.getMatchService().handlePlayerEliminated(betaFighter.uniqueId());
        harness.getMatchService().handlePlayerEliminated(betaMate.uniqueId());
    }

    private void betaWinsTheRound() {
        harness.getMatchService().handlePlayerEliminated(alphaFighter.uniqueId());
        harness.getMatchService().handlePlayerEliminated(alphaMate.uniqueId());
    }

    private static final class RecordingRoundCleaning implements RoundCleaning {

        private boolean baseline = true;
        private String refusal;
        private int startCount;
        private Run running;

        @Override
        public boolean hasBaseline() {
            return baseline;
        }

        @Override
        public Optional<String> startCleaningBetweenRounds(Consumer<String> completionHandler) {
            if (refusal != null) {
                return Optional.of(refusal);
            }
            startCount++;
            running = new Run(completionHandler);
            return Optional.empty();
        }

        private boolean isCleaning() {
            return running != null;
        }

        private Run takeRun() {
            Run run = running;
            running = null;
            return run;
        }

        private void finish() {
            assertTrue(running != null, "no cleaning is running");
            takeRun().finish();
        }

        private record Run(Consumer<String> completionHandler) {

            private void finish() {
                completionHandler.accept("The arena is clean");
            }
        }
    }
}
