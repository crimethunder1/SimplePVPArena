package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerRevokeResultTest {

    private static final List<String> THREE_TEAMS = List.of("Alpha", "Bravo", "Charlie");
    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");
    private static final List<String> EIGHT_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo",
            "Foxtrot", "Golf", "Hotel");

    private static final String NO_TOURNAMENT = "No tournament is in progress";
    private static final String REVOKED_NOTICE_TEXT = "was taken back";

    @Test
    void rejectsTheTakingBackWithoutATournament() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);

        assertRejected(fixture, "round1-match1", NO_TOURNAMENT);
        assertEquals(List.of(), fixture.tournamentManager().findRevocableMatches());

        fixture.startTournament();
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertRejected(fixture, "round1-match1", NO_TOURNAMENT);
        assertEquals(List.of(), fixture.tournamentManager().findRevocableMatches());
    }

    @Test
    void rejectsAnUnknownMatch() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        assertRejected(fixture, "round9-match1", "Unknown match round9-match1");
    }

    @Test
    void rejectsAMatchThatIsNotDecided() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        assertRejected(fixture, "round1-match1", "Match round1-match1 is not decided");
        assertRejected(fixture, "round1-match2", "Match round1-match2 is not decided");
    }

    @Test
    void rejectsTheFinal() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        assertRejected(fixture, "round2-match1", "The result of the final cannot be taken back");
    }

    @Test
    void rejectsABye() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(THREE_TEAMS);
        assertTrue(fixture.tournamentManager().startTournament(true).successful());
        BracketMatch bye = fixture.bracket().getRounds().get(0).stream()
                .filter(BracketMatch::hasExactlyOneParticipant)
                .findFirst()
                .orElseThrow();

        assertRejected(fixture, bye.getMatchId(), "A bye cannot be taken back");
        assertTrue(bye.isDecided());
    }

    @Test
    void rejectsAResultWhoseFollowingMatchIsDecided() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(EIGHT_TEAMS);
        fixture.startTournament();
        String winnerTeamName = fixture.forceRunningMatchForTheFirstTeam();
        fixture.forceRunningMatchForTheFirstTeam();
        decideForTheFirstTeam(fixture, "round2-match1");

        assertRejected(fixture, "round1-match1", "The following match round2-match1 has already been played");
        assertEquals(Optional.of(winnerTeamName), fixture.bracketMatch("round1-match1").getWinnerTeamName());
    }

    @Test
    void rejectsAResultWhoseFollowingMatchIsRunning() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        fixture.forceRunningMatchForTheFirstTeam();
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());

        assertRejected(fixture, "round1-match1", "The following match round2-match1 is running");
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.of("round2-match1"), fixture.runningMatchId());
    }

    @Test
    void rejectsAResultWhoseFollowingMatchWaitsForItsKit() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(FOUR_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.kitChoiceService().choose("Knight", "KT"));
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        assertEquals("round2-match1", fixture.kitChoiceService().getPending().orElseThrow().matchId());

        assertRejected(fixture, "round1-match2", "A kit is being chosen for the following match round2-match1");
        assertTrue(fixture.bracketMatch("round1-match2").isDecided());
    }

    @Test
    void takesBackAResultWhileAnotherMatchRunsAndPlaysTheMatchAgainAfterwards() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        String winnerTeamName = fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());

        TournamentOperationResult result = fixture.tournamentManager().revokeMatchResult("round1-match1");

        assertTrue(result.successful(), result.message());
        assertEquals("Match round1-match1 is open again", result.message());
        assertEquals(List.of("The result of match round1-match1 was taken back, " + winnerTeamName
                + " no longer advances"), fixture.adminMessagesContaining(REVOKED_NOTICE_TEXT));
        BracketMatch match = fixture.bracketMatch("round1-match1");
        assertFalse(match.isDecided());
        assertTrue(match.isPlayable());
        assertEquals(Optional.empty(), fixture.bracketMatch("round2-match1").getFirstTeamName());
        assertEquals(Optional.empty(), fixture.tournamentManager().findDecisionKind("round1-match1"));
        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());

        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
    }

    @Test
    void startsTheTakenBackMatchAtOnceAndSendsTheReadyFinalBackToPending() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        decideForTheFirstTeam(fixture, "round1-match2");
        fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.empty(), fixture.runningMatchId());

        assertTrue(fixture.tournamentManager().revokeMatchResult("round1-match2").successful());

        assertEquals(FinalMatchStage.PENDING, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());
        assertEquals(Optional.empty(), fixture.bracketMatch("round2-match1").getSecondTeamName());
    }

    @Test
    void startsNoMatchWhileThePausedTournamentWaitsAndStartsTheMatchWhenItContinues() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        decideForTheFirstTeam(fixture, "round1-match2");
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertTrue(fixture.tournamentManager().revokeMatchResult("round1-match2").successful());

        assertEquals(TournamentState.PAUSED, fixture.tournamentManager().getState());
        assertEquals(FinalMatchStage.PENDING, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertTrue(fixture.bracketMatch("round1-match2").isPlayable());

        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());
    }

    @Test
    void takesBackAWalkoverAndDeclaresItAgainWhenTheTournamentContinues() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();
        BracketMatch secondMatch = fixture.bracketMatch("round1-match2");
        String remainingTeamName = secondMatch.getFirstTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry().removeTeam(secondMatch.getSecondTeamName().orElseThrow()));
        fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of(MatchDecisionKind.WALKOVER),
                fixture.tournamentManager().findDecisionKind("round1-match2"));
        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertTrue(fixture.tournamentManager().revokeMatchResult("round1-match2").successful());

        assertFalse(secondMatch.isDecided());
        assertEquals(Optional.empty(), fixture.tournamentManager().findDecisionKind("round1-match2"));

        assertTrue(fixture.tournamentManager().togglePause().successful());

        assertEquals(Optional.of(remainingTeamName), secondMatch.getWinnerTeamName());
        assertEquals(Optional.of(MatchDecisionKind.WALKOVER),
                fixture.tournamentManager().findDecisionKind("round1-match2"));
    }

    @Test
    void listsTheMatchesWhoseResultCanBeTakenBackByRoundAndPosition() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(EIGHT_TEAMS);
        fixture.startTournament();
        fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(List.of("round1-match1"), revocableMatchIds(fixture));
        fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(List.of("round1-match1", "round1-match2"), revocableMatchIds(fixture));

        decideForTheFirstTeam(fixture, "round2-match1");
        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(Optional.of("round1-match4"), fixture.runningMatchId());
        assertEquals(List.of("round1-match3", "round2-match1"), revocableMatchIds(fixture));
    }

    @Test
    void savesTheTournamentWithoutTheTakenBackResultAndRestoresItAfterARestart() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        before.startTournament();
        before.forceRunningMatchForTheFirstTeam();
        String secondWinnerTeamName = before.forceRunningMatchForTheFirstTeam();

        assertTrue(before.tournamentManager().revokeMatchResult("round1-match1").successful());

        SavedTournament savedTournament = storage.lastSavedTournament();
        assertEquals(TournamentState.RUNNING, savedTournament.state());
        assertEquals(List.of(new SavedMatchResult("round1-match2", secondWinnerTeamName,
                Optional.of(MatchDecisionKind.FORCED))), savedTournament.results());

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertTrue(after.bracketMatch("round1-match1").isPlayable());
        assertEquals(Optional.empty(), after.bracketMatch("round2-match1").getFirstTeamName());
        assertEquals(Optional.of(secondWinnerTeamName), after.bracketMatch("round2-match1").getSecondTeamName());
        assertEquals(FinalMatchStage.PENDING, after.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.of(MatchDecisionKind.FORCED),
                after.tournamentManager().findDecisionKind("round1-match2"));
        assertEquals(List.of("round1-match2"), revocableMatchIds(after));
    }

    private static void assertRejected(TournamentManagerFixture fixture, String matchId, String expectedMessage) {
        assertEquals(Optional.of(expectedMessage), fixture.tournamentManager().findRevokeRejection(matchId));
        int noticeCount = fixture.adminMessagesContaining(REVOKED_NOTICE_TEXT).size();

        TournamentOperationResult result = fixture.tournamentManager().revokeMatchResult(matchId);

        assertFalse(result.successful());
        assertEquals(expectedMessage, result.message());
        assertEquals(noticeCount, fixture.adminMessagesContaining(REVOKED_NOTICE_TEXT).size());
    }

    private static void decideForTheFirstTeam(TournamentManagerFixture fixture, String matchId) {
        String winnerTeamName = fixture.bracketMatch(matchId).getFirstTeamName().orElseThrow();
        TournamentOperationResult decision = fixture.tournamentManager().forceMatchResult(matchId, winnerTeamName);
        assertTrue(decision.successful(), decision.message());
    }

    private static List<String> revocableMatchIds(TournamentManagerFixture fixture) {
        return fixture.tournamentManager().findRevocableMatches().stream().map(BracketMatch::getMatchId).toList();
    }
}
