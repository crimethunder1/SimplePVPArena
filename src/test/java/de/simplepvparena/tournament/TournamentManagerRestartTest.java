package de.simplepvparena.tournament;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.team.TeamRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerRestartTest {

    private static final List<String> TWO_TEAMS = List.of("Alpha", "Bravo");
    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");
    private static final String FIRST_MATCH_ID = "round1-match1";
    private static final String RESTARTED_NOTICE = "Match round1-match1 was restarted";

    @Test
    void stopsTheRunningMatchAndBeginsItAgainWithTheSameTeamsAndAFreshScore() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        Match oldMatch = fixture.matchService().getActiveMatch().orElseThrow();
        oldMatch.getScore().awardPoint(oldMatch.getFirstTeam().getTeamName());
        assertEquals(1, oldMatch.getScore().getPoints(oldMatch.getFirstTeam().getTeamName()));

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertTrue(restart.successful(), restart.message());
        assertEquals("Match round1-match1 was stopped and begins again", restart.message());
        assertEquals(MatchState.CANCELLED, oldMatch.getState());
        Match newMatch = fixture.matchService().getActiveMatch().orElseThrow();
        assertNotSame(oldMatch, newMatch);
        assertEquals(FIRST_MATCH_ID, newMatch.getMatchId());
        assertSame(oldMatch.getFirstTeam(), newMatch.getFirstTeam());
        assertSame(oldMatch.getSecondTeam(), newMatch.getSecondTeam());
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getFirstTeam().getTeamName()));
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getSecondTeam().getTeamName()));
        assertFalse(newMatch.getScore().isDecided());
        assertEquals(1, newMatch.getRoundNumber());
        assertFalse(newMatch.isFinalMatch());
        assertFalse(fixture.bracketMatch(FIRST_MATCH_ID).isDecided());
        assertTrue(fixture.bracketMatch(FIRST_MATCH_ID).getWinnerTeamName().isEmpty());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
    }

    @Test
    void tellsTheAdminsAndWarnsAboutASkippedCleaningLikeAfterAnyOtherMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        fixture.adminMessages().clear();

        fixture.tournamentManager().restartRunningMatch();

        assertEquals(List.of(RESTARTED_NOTICE, "The arena was not cleaned after match round1-match1: "
                + TournamentManagerFixture.SKIPPED_CLEANING_REASON
                + ". The next match starts in the arena as it is now"), fixture.adminMessages());
        assertEquals(1, fixture.logMessagesContaining("The arena is not cleaned automatically after this match")
                .size());
    }

    @Test
    void waitsForTheCleaningAndThenBeginsTheMatchAgainWithANewKitChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoiceAndArenaCleaning(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));
        Match oldMatch = fixture.matchService().getActiveMatch().orElseThrow();
        assertSame(fixture.kitDefinitionOf("Knight"), fixture.activeKitDefinition());

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertTrue(restart.successful(), restart.message());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertTrue(fixture.isArenaBeingCleaned());
        assertTrue(fixture.tournamentManager().isWaitingForArenaCleaning());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());

        fixture.finishArenaCleaning();

        assertFalse(fixture.tournamentManager().isWaitingForArenaCleaning());
        PendingKitChoice pending = fixture.kitChoiceService().getPending().orElseThrow();
        assertEquals(FIRST_MATCH_ID, pending.matchId());
        assertEquals(oldMatch.getFirstTeam().getTeamName(), pending.firstTeamName());
        assertEquals(oldMatch.getSecondTeam().getTeamName(), pending.secondTeamName());
        assertEquals(2, fixture.kitChoicePresenter().shownChoices().size());

        assertTrue(fixture.kitChoiceService().choose("Archer", "KT"));

        Match newMatch = fixture.matchService().getActiveMatch().orElseThrow();
        assertNotSame(oldMatch, newMatch);
        assertEquals(FIRST_MATCH_ID, newMatch.getMatchId());
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getFirstTeam().getTeamName()));
        assertEquals(0, newMatch.getScore().getPoints(newMatch.getSecondTeam().getTeamName()));
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertFalse(fixture.bracketMatch(FIRST_MATCH_ID).isDecided());
    }

    @Test
    void stopsTheFinalAndOffersItsStartAgainWithoutStartingItByItself() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        Match oldFinal = fixture.matchService().getActiveMatch().orElseThrow();
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        fixture.adminMessages().clear();

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertTrue(restart.successful(), restart.message());
        assertEquals("The final was stopped, start it again with \"Start the final\" in the match menu of "
                + "/tournament", restart.message());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertFalse(fixture.bracket().getFinalMatch().isDecided());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertEquals(List.of(RESTARTED_NOTICE, "The arena was not cleaned after match round1-match1: "
                + TournamentManagerFixture.SKIPPED_CLEANING_REASON
                + ". The next match starts in the arena as it is now"), fixture.adminMessages());

        TournamentOperationResult start = fixture.tournamentManager().startFinalMatch();

        assertTrue(start.successful(), start.message());
        Match newFinal = fixture.matchService().getActiveMatch().orElseThrow();
        assertNotSame(oldFinal, newFinal);
        assertTrue(newFinal.isFinalMatch());
        assertEquals(0, newFinal.getScore().getPoints(newFinal.getFirstTeam().getTeamName()));
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
    }

    @Test
    void waitsForTheCleaningBeforeTheFinalCanBeStartedAgain() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoiceAndArenaCleaning(TWO_TEAMS);
        fixture.startTournament();
        fixture.tournamentManager().startFinalMatch();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        fixture.tournamentManager().restartRunningMatch();

        assertTrue(fixture.isArenaBeingCleaned());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        TournamentOperationResult earlyStart = fixture.tournamentManager().startFinalMatch();
        assertFalse(earlyStart.successful());
        assertTrue(earlyStart.message().startsWith("The arena is still being cleaned after the last match"),
                earlyStart.message());

        fixture.finishArenaCleaning();

        assertEquals(Optional.empty(), fixture.runningMatchId(), "the final never starts by itself");
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        assertTrue(fixture.tournamentManager().isKitChoicePending());
    }

    @Test
    void stopsTheMatchOfAPausedTournamentAndBeginsItWhenTheTournamentContinues() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().togglePause().successful());
        Match oldMatch = fixture.matchService().getActiveMatch().orElseThrow();

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertTrue(restart.successful(), restart.message());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(TournamentState.PAUSED, fixture.tournamentManager().getState());

        assertTrue(fixture.tournamentManager().togglePause().successful());

        Match newMatch = fixture.matchService().getActiveMatch().orElseThrow();
        assertNotSame(oldMatch, newMatch);
        assertEquals(FIRST_MATCH_ID, newMatch.getMatchId());
    }

    @Test
    void keepsAPausedTournamentWaitingAfterTheCleaningUntilItContinues() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoiceAndArenaCleaning(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));
        assertTrue(fixture.tournamentManager().togglePause().successful());

        fixture.tournamentManager().restartRunningMatch();
        fixture.finishArenaCleaning();

        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertFalse(fixture.tournamentManager().isKitChoicePending());

        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertEquals(FIRST_MATCH_ID, fixture.kitChoiceService().getPending().orElseThrow().matchId());
    }

    @Test
    void savesNothingBecauseNeitherTheBracketNorTheStateChanges() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        fixture.startTournament();
        int saveCount = storage.savedTournaments().size();

        fixture.tournamentManager().restartRunningMatch();

        assertEquals(saveCount, storage.savedTournaments().size());
        assertEquals(0, storage.deleteCount());
        assertTrue(storage.storedTournament().orElseThrow().results().stream()
                .noneMatch(result -> result.matchId().equals(FIRST_MATCH_ID)));
    }

    @Test
    void refusesWithoutARunningMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);
        fixture.startTournament();

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertFalse(restart.successful());
        assertEquals("No match is running", restart.message());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
    }

    @Test
    void refusesWithoutATournamentAndWithoutAMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertFalse(restart.successful());
        assertEquals("No match is running", restart.message());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
    }

    @Test
    void refusesAMatchThatRunsWithoutATournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);
        fixture.startMatchOutsideTheTournament("Alpha", "Bravo");

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertFalse(restart.successful());
        assertEquals("No tournament is in progress", restart.message());
        assertEquals(Optional.of("dry-run"), fixture.runningMatchId());
    }

    @Test
    void refusesWhileAKitIsBeingChosen() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertFalse(restart.successful());
        assertEquals("A kit is being chosen for match round1-match1, wait for it or pause the tournament",
                restart.message());
        assertTrue(fixture.tournamentManager().isKitChoicePending());
        assertEquals(1, fixture.kitChoicePresenter().shownChoices().size());
    }

    @Test
    void refusesAnAutoMatchAndLeavesItRunning() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);
        TeamRegistry teamRegistry = fixture.teamRegistry();
        Match autoMatch = Match.autoMatch("auto-1", teamRegistry.findTeam("Alpha").orElseThrow(),
                teamRegistry.findTeam("Bravo").orElseThrow(), new ArenaRegistry().getArena());
        fixture.matchService().startMatch(autoMatch);
        fixture.adminMessages().clear();

        TournamentOperationResult restart = fixture.tournamentManager().restartRunningMatch();

        assertFalse(restart.successful());
        assertEquals("An auto match cannot be restarted", restart.message());
        assertSame(autoMatch, fixture.matchService().getActiveMatch().orElseThrow());
        assertEquals(List.of(), fixture.adminMessages());
    }

    @Test
    void namesTheSameRejectionsForTheMenu() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        assertEquals(Optional.of("No match is running"), fixture.tournamentManager().findRestartRejection());

        fixture.startTournament();

        assertEquals(Optional.empty(), fixture.tournamentManager().findRestartRejection());
    }
}
