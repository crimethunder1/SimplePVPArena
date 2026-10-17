package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerKitChoiceTest {

    private static final List<String> TWO_TEAMS = List.of("Alpha", "Bravo");
    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");
    private static final String FIRST_MATCH_REJECTION = "A kit is being chosen for match round1-match1, wait for "
            + "it or pause the tournament";

    @Test
    void startsTheFirstMatchOnlyAfterAnAdminChoseTheKit() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);

        fixture.startTournament();

        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertTrue(fixture.tournamentManager().isKitChoicePending());
        PendingKitChoice pending = fixture.kitChoiceService().getPending().orElseThrow();
        assertEquals("round1-match1", pending.matchId());
        BracketMatch firstMatch = fixture.bracketMatch("round1-match1");
        assertEquals(firstMatch.getFirstTeamName().orElseThrow() + " against "
                + firstMatch.getSecondTeamName().orElseThrow(), pending.teamsDescription());
        assertEquals("Archer", pending.defaultKitName());
        assertEquals(List.of(pending), fixture.kitChoicePresenter().shownChoices());

        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertSame(fixture.kitDefinitionOf("Knight"), fixture.activeKitDefinition());
        assertEquals(Optional.of("Archer"), fixture.kitLibrary().getDefaultKitName());
    }

    @Test
    void startsTheMatchWithTheDefaultKitWhenNobodyChoosesInTime() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();

        fixture.kitChoiceScheduler().advanceSeconds(29);
        assertEquals(Optional.empty(), fixture.runningMatchId());

        fixture.kitChoiceScheduler().advanceSeconds(1);

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals(1, fixture.adminMessagesContaining("10 seconds left").size(), fixture.adminMessages().toString());
    }

    @Test
    void refusesToDecideAWaitingMatchWhileTheKitIsChosen() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        String secondMatchTeamName = fixture.bracketMatch("round1-match2").getFirstTeamName().orElseThrow();
        String firstMatchTeamName = fixture.bracketMatch("round1-match1").getFirstTeamName().orElseThrow();

        TournamentOperationResult waitingDecision = fixture.tournamentManager().forceMatchResult("round1-match2",
                secondMatchTeamName);
        TournamentOperationResult choosingDecision = fixture.tournamentManager().forceMatchResult("round1-match1",
                firstMatchTeamName);

        assertFalse(waitingDecision.successful());
        assertEquals(FIRST_MATCH_REJECTION, waitingDecision.message());
        assertFalse(choosingDecision.successful());
        assertEquals(FIRST_MATCH_REJECTION, choosingDecision.message());
        assertFalse(fixture.bracketMatch("round1-match2").isDecided());
        assertFalse(fixture.bracketMatch("round1-match1").isDecided());
        assertTrue(fixture.tournamentManager().isKitChoicePending());
    }

    @Test
    void refusesToStartTheFinalWhileTheKitOfAnotherMatchIsChosen() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();

        TournamentOperationResult finalStart = fixture.tournamentManager().startFinalMatch();

        assertFalse(finalStart.successful());
        assertEquals(FIRST_MATCH_REJECTION, finalStart.message());
    }

    @Test
    void choosesTheKitBeforeTheFinalAndMarksItRunningOnlyWhenItStarts() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.startTournament();
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertFalse(fixture.tournamentManager().isKitChoicePending());

        assertTrue(fixture.tournamentManager().startFinalMatch().successful());

        assertTrue(fixture.tournamentManager().isKitChoicePending());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        TournamentOperationResult secondStart = fixture.tournamentManager().startFinalMatch();
        assertFalse(secondStart.successful());
        assertEquals("A kit is being chosen for match round1-match1, wait for it or pause the tournament",
                secondStart.message());

        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertTrue(fixture.matchService().getActiveMatch().orElseThrow().isFinalMatch());
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        assertSame(fixture.kitDefinitionOf("Knight"), fixture.activeKitDefinition());
    }

    @Test
    void reportsThatTheKitOfTheFinalIsBeingChosenInsteadOfAStartedFinal() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.startTournament();

        TournamentOperationResult finalStart = fixture.tournamentManager().startFinalMatch();

        assertTrue(finalStart.successful());
        assertEquals("The kit for the final is being chosen", finalStart.message());
        assertEquals(Optional.empty(), fixture.runningMatchId());
    }

    @Test
    void reportsThatTheFinalHasStartedWhenItStartsWithoutAChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.kitChoicePresenter().letEveryAdminLeave();
        fixture.startTournament();

        TournamentOperationResult finalStart = fixture.tournamentManager().startFinalMatch();

        assertTrue(finalStart.successful());
        assertEquals("The final has started", finalStart.message());
        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
    }

    @Test
    void declaresAWalkoverWhenATeamIsRemovedDuringTheChoiceAndChoosesTheKitOfTheNextMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch firstMatch = fixture.bracketMatch("round1-match1");
        String removedTeamName = firstMatch.getFirstTeamName().orElseThrow();
        String remainingTeamName = firstMatch.getSecondTeamName().orElseThrow();

        assertTrue(fixture.teamRegistry().removeTeam(removedTeamName));
        fixture.kitChoiceScheduler().advanceSeconds(30);

        assertEquals(Optional.of(remainingTeamName), fixture.bracketMatch("round1-match1").getWinnerTeamName());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals("round1-match2", fixture.kitChoiceService().getPending().orElseThrow().matchId());
        assertEquals(2, fixture.kitChoicePresenter().shownChoices().size());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());

        fixture.kitChoiceScheduler().advanceSeconds(30);

        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());
    }

    @Test
    void dropsTheChosenKitWhenATeamWasRemovedBeforeTheChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        String removedTeamName = fixture.bracketMatch("round1-match1").getSecondTeamName().orElseThrow();
        String remainingTeamName = fixture.bracketMatch("round1-match1").getFirstTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry().removeTeam(removedTeamName));

        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        assertEquals(Optional.of(remainingTeamName), fixture.bracketMatch("round1-match1").getWinnerTeamName());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals("round1-match2", fixture.kitChoiceService().getPending().orElseThrow().matchId());
    }

    @Test
    void tellsTheAdminsAndStartsNoMatchWhenBothTeamsAreRemovedDuringTheChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch firstMatch = fixture.bracketMatch("round1-match1");
        assertTrue(fixture.teamRegistry().removeTeam(firstMatch.getFirstTeamName().orElseThrow()));
        assertTrue(fixture.teamRegistry().removeTeam(firstMatch.getSecondTeamName().orElseThrow()));

        fixture.kitChoiceScheduler().advanceSeconds(30);

        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertFalse(fixture.bracketMatch("round1-match1").isDecided());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertEquals(List.of("Both teams of match round1-match1 are no longer registered, the tournament waits for "
                + "you. Decide the match in the match menu of /tournament under \"Decide a waiting match\""),
                fixture.adminMessagesContaining("Both teams"));
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
    }

    @Test
    void endsTheFinalByWalkoverWhenAFinalistIsRemovedDuringItsChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        BracketMatch finalMatch = fixture.bracketMatch("round1-match1");
        String removedTeamName = finalMatch.getFirstTeamName().orElseThrow();
        String remainingTeamName = finalMatch.getSecondTeamName().orElseThrow();

        assertTrue(fixture.teamRegistry().removeTeam(removedTeamName));
        fixture.kitChoiceScheduler().advanceSeconds(30);

        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(TournamentState.FINISHED, fixture.tournamentManager().getState());
        assertEquals(Optional.of(remainingTeamName), fixture.bracket().getChampionTeamName());
        assertEquals(FinalMatchStage.FINISHED, fixture.tournamentManager().getFinalMatchStage());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
    }

    @Test
    void leavesTheFinalReadyWhenBothFinalistsAreRemovedDuringItsChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));
        assertTrue(fixture.teamRegistry().removeTeam("Bravo"));

        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(1, fixture.adminMessagesContaining("Both teams of match round1-match1").size());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
    }

    @Test
    void cancelsTheChoiceWhenTheTournamentIsPausedAndBeginsANewOneWhenItContinues() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();

        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertEquals(List.of(KitChoiceService.CANCELLED_MESSAGE), fixture.kitChoicePresenter().closeMessages());
        fixture.kitChoiceScheduler().advanceSeconds(60);
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(0, fixture.kitChoiceScheduler().pendingTaskCount());

        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertTrue(fixture.tournamentManager().isKitChoicePending());
        assertEquals("round1-match1", fixture.kitChoiceService().getPending().orElseThrow().matchId());
        assertEquals(2, fixture.kitChoicePresenter().shownChoices().size());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        fixture.kitChoiceScheduler().advanceSeconds(30);
        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
    }

    @Test
    void keepsTheFinalStartableWhenTheTournamentIsPausedDuringItsKitChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());

        assertTrue(fixture.tournamentManager().togglePause().successful());
        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        assertTrue(fixture.tournamentManager().isKitChoicePending());
        fixture.kitChoiceScheduler().advanceSeconds(30);
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
    }

    @Test
    void cancelsTheChoiceWithTheTournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();

        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertEquals(List.of(KitChoiceService.CANCELLED_MESSAGE), fixture.kitChoicePresenter().closeMessages());
        fixture.kitChoiceScheduler().advanceSeconds(60);
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
    }

    @Test
    void makesTheDefaultKitActiveAgainWhenTheTournamentIsCancelledDuringAMatchWithAChosenKit() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals(Optional.of("Archer"), fixture.kitLibrary().getDefaultKitName());
    }

    @Test
    void makesTheDefaultKitActiveAgainWhenTheMatchIsOverAndChoosesTheKitOfTheNextMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals("round1-match2", fixture.kitChoiceService().getPending().orElseThrow().matchId());
    }

    @Test
    void keepsTheChosenKitForTheWholeMatchWhenAnotherKitIsSetAsTheDefaultKitDuringIt() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Archer", "KT"));

        fixture.kitLibrary().setDefaultKit("Knight");

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertEquals(Optional.of("Knight"), fixture.kitLibrary().getDefaultKitName());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());

        fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertSame(fixture.kitDefinitionOf("Knight"), fixture.activeKitDefinition());
    }

    @Test
    void decidesTheRunningMatchAfterTheChoiceAndMakesTheDefaultKitActiveAgain() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));

        String winnerTeamName = fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(Optional.of(winnerTeamName), fixture.bracketMatch("round1-match1").getWinnerTeamName());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals("round1-match2", fixture.kitChoiceService().getPending().orElseThrow().matchId());
    }

    @Test
    void startsTheMatchAtOnceWithTheDefaultKitWhenNoAdminIsOnline() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.kitChoicePresenter().letEveryAdminLeave();

        fixture.startTournament();

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals(List.of(), fixture.kitChoicePresenter().shownChoices());
        assertEquals(1, fixture.kitChoiceScheduler().logLines().size());
    }

    @Test
    void startsTheMatchAtOnceWithASingleKit() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.kitLibrary().deleteKit("Knight");

        fixture.startTournament();

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertSame(fixture.kitDefinitionOf("Archer"), fixture.activeKitDefinition());
        assertEquals(List.of(), fixture.kitChoicePresenter().shownChoices());
    }

    @Test
    void startsTheMatchAtOnceWithTheFormerConstructor() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);

        fixture.startTournament();

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertEquals(List.of(), fixture.kitChoicePresenter().shownChoices());
    }

    @Test
    void cancelsAnOpenChoiceWhenTheTournamentIsRestored() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorageAndKitChoice(storage, FOUR_TEAMS);
        before.startTournament();
        assertTrue(before.kitChoiceService().choose("Knight", "KT"));
        TournamentManagerFixture after = TournamentManagerFixture.withStorageAndKitChoice(storage, FOUR_TEAMS);
        int[] matchStartCount = new int[1];
        after.kitChoiceService().begin("round1-match1", "Alpha against Bravo", () -> matchStartCount[0]++);
        after.kitLibrary().activate("Knight");

        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertFalse(after.tournamentManager().isKitChoicePending());
        assertEquals(List.of(KitChoiceService.CANCELLED_MESSAGE), after.kitChoicePresenter().closeMessages());
        assertSame(after.kitDefinitionOf("Archer"), after.activeKitDefinition());
        after.kitChoiceScheduler().advanceSeconds(60);
        assertEquals(0, matchStartCount[0]);
        assertEquals(Optional.empty(), after.runningMatchId());
        assertEquals(TournamentState.PAUSED, after.tournamentManager().getState());
    }
}
