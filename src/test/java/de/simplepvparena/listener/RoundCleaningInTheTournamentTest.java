package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.TournamentOperationResult;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundCleaningInTheTournamentTest {

    private static final String ARENA_BEING_CLEANED = "The arena is still being cleaned. Try again in a moment.";

    @Test
    void aTournamentMatchIsCleanedBetweenTheRoundsAndOnceMoreAfterTheDecidingPoint() {
        AutoMatchHarness harness = startTournamentMatch();
        Match match = harness.runningMatch();
        String winner = harness.runningTeamNames().get(0);

        harness.winRound(winner);

        assertEquals(1, harness.startedRoundCleaningCount());
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertTrue(harness.isCleaning());
        harness.finishCleaning();
        harness.arena().runTick();
        assertEquals(2, match.getRoundNumber());

        harness.winRound(winner);

        assertTrue(match.isFinished());
        assertEquals(1, harness.startedRoundCleaningCount(), "no round cleaning after the deciding point");
        assertEquals(1, harness.startedCleaningCount(), "the cleaning after the match runs once as before");
        assertTrue(harness.tournamentManager().isWaitingForArenaCleaning());
    }

    @Test
    void forcingTheResultAndRestartingTheMatchAreRefusedDuringTheCleaning() {
        AutoMatchHarness harness = startTournamentMatch();
        Match match = harness.runningMatch();
        String winner = harness.runningTeamNames().get(0);
        harness.winRound(winner);

        TournamentOperationResult forced = harness.tournamentManager().forceMatchResult(match.getMatchId(), winner);
        TournamentOperationResult restarted = harness.tournamentManager().restartRunningMatch();

        assertFalse(forced.successful());
        assertEquals(ARENA_BEING_CLEANED, forced.message());
        assertFalse(restarted.successful());
        assertEquals(ARENA_BEING_CLEANED, restarted.message());
        assertEquals(Optional.of(ARENA_BEING_CLEANED), harness.tournamentManager().findRestartRejection());
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState(), "the match goes on");

        harness.finishCleaning();
        harness.arena().runTick();

        assertEquals(Optional.empty(), harness.tournamentManager().findRestartRejection());
        assertTrue(harness.tournamentManager().forceMatchResult(match.getMatchId(), winner).successful());
    }

    @Test
    void cancellingTheTournamentDuringTheCleaningEndsTheMatchAndTheCleaningRunsToItsEnd() {
        AutoMatchHarness harness = startTournamentMatch();
        Match match = harness.runningMatch();
        harness.winRound(harness.runningTeamNames().get(0));

        assertTrue(harness.tournamentManager().cancelTournament().successful());

        assertEquals(MatchState.CANCELLED, match.getState());
        assertTrue(harness.isCleaning(), "the cleaning cannot be stopped and keeps the arena locked");
        TournamentOperationResult refusedStart = harness.tournamentManager().startTournament(false);
        assertFalse(refusedStart.successful());
        assertEquals(ARENA_BEING_CLEANED, refusedStart.message());

        harness.finishCleaning();
        harness.advanceSeconds(10);

        assertFalse(harness.matchService().hasActiveMatch());
        assertEquals(1, match.getRoundNumber());
        assertEquals(TournamentState.CANCELLED, harness.tournamentManager().getState());
    }

    @Test
    void pausingTheTournamentLetsTheCleaningAndTheMatchGoOn() {
        AutoMatchHarness harness = startTournamentMatch();
        Match match = harness.runningMatch();
        harness.winRound(harness.runningTeamNames().get(0));

        assertTrue(harness.tournamentManager().togglePause().successful());

        assertEquals(TournamentState.PAUSED, harness.tournamentManager().getState());
        assertTrue(harness.isCleaning());
        harness.finishCleaning();
        harness.arena().runTick();
        assertEquals(2, match.getRoundNumber(), "a pause never stopped a running match");
        assertTrue(harness.matchService().isRoundTimerPaused());
    }

    @Test
    void resumingTheTournamentDuringTheCleaningBetweenTheRoundsStartsNothingBeforeTheEndOfTheCleaning() {
        AutoMatchHarness harness = startTournamentMatch();
        Match match = harness.runningMatch();
        harness.winRound(harness.runningTeamNames().get(0));
        assertTrue(harness.tournamentManager().togglePause().successful());
        assertTrue(harness.matchService().isCleaningBetweenRounds());

        TournamentOperationResult resume = harness.tournamentManager().togglePause();

        assertTrue(resume.successful(), resume.message());
        assertEquals(TournamentState.RUNNING, harness.tournamentManager().getState());
        assertFalse(harness.matchService().isRoundTimerPaused());
        harness.advanceSeconds(10);
        assertEquals(Optional.of(match), harness.matchService().getActiveMatch(), "no other match starts");
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        assertEquals(1, match.getRoundNumber(), "no round begins before the end of the cleaning");

        harness.finishCleaning();
        harness.arena().runTick();

        assertFalse(harness.matchService().isCleaningBetweenRounds());
        assertEquals(2, match.getRoundNumber());
        assertEquals(MatchState.RUNNING, match.getState());
    }

    @Test
    void resumingTheTournamentWithoutACleaningWorksAsBefore() {
        AutoMatchHarness harness = startTournamentMatch();
        Match match = harness.runningMatch();
        assertTrue(harness.tournamentManager().togglePause().successful());

        TournamentOperationResult resume = harness.tournamentManager().togglePause();

        assertTrue(resume.successful(), resume.message());
        assertEquals(TournamentState.RUNNING, harness.tournamentManager().getState());
        assertFalse(harness.matchService().isRoundTimerPaused());
        assertEquals(Optional.of(match), harness.matchService().getActiveMatch());
        assertEquals(MatchState.RUNNING, match.getState());
    }

    @Test
    void resumingIsStillRefusedWhenTheMatchWasCancelledDuringTheCleaningBetweenTheRounds() {
        AutoMatchHarness harness = startTournamentMatch();
        harness.winRound(harness.runningTeamNames().get(0));
        assertTrue(harness.tournamentManager().togglePause().successful());
        harness.matchService().cancelActiveMatch();

        TournamentOperationResult resume = harness.tournamentManager().togglePause();

        assertFalse(resume.successful());
        assertEquals(ARENA_BEING_CLEANED, resume.message());
        assertEquals(TournamentState.PAUSED, harness.tournamentManager().getState());
        assertFalse(harness.matchService().hasActiveMatch(), "no match starts into the running cleaning");
    }

    @Test
    void anAutoMatchIsCleanedBetweenTheRoundsAsWell() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.cleanBetweenRounds();
        harness.switchOnWithPause(0);
        harness.advanceUntilTheAutoMatchRuns();
        Match match = harness.runningMatch();

        harness.winRound("Alpha");

        assertEquals(1, harness.startedRoundCleaningCount());
        assertEquals(AutoMatchState.RUNNING, harness.state());
        TournamentOperationResult forced = harness.tournamentManager().forceMatchResult(match.getMatchId(), "Alpha");
        assertEquals(ARENA_BEING_CLEANED, forced.message());
        harness.advanceSeconds(5);
        assertEquals(AutoMatchState.RUNNING, harness.state(), "the auto match keeps the arena during the cleaning");

        harness.finishCleaning();
        harness.arena().runTick();
        harness.winRound("Alpha");

        assertTrue(match.isFinished());
        assertEquals(1, harness.startedRoundCleaningCount());
        assertEquals(1, harness.startedCleaningCount());
        assertEquals(AutoMatchState.COOLDOWN, harness.state());
    }

    @Test
    void withoutABaselineTheTournamentMatchGoesOnAsBeforeWithOneNotice() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        harness.cleanBetweenRounds();
        harness.skipEveryCleaning();
        harness.startTournament();
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "Admin"));
        harness.forgetReceivedTexts();
        Match match = harness.runningMatch();
        List<String> teamNames = harness.runningTeamNames();

        harness.winRound(teamNames.get(0));
        harness.winRound(teamNames.get(1));

        assertEquals(3, match.getRoundNumber());
        assertEquals(0, harness.startedRoundCleaningCount());
        assertEquals(List.of("The arena is not cleaned between the rounds, no baseline is stored"),
                harness.adminMessages());
    }

    private static AutoMatchHarness startTournamentMatch() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        harness.cleanBetweenRounds();
        harness.startTournament();
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "Admin"), "the kit choice lets the first match begin");
        assertTrue(harness.matchService().hasActiveMatch());
        return harness;
    }
}
