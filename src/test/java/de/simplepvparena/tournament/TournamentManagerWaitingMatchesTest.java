package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerWaitingMatchesTest {

    private static final List<String> TWO_TEAMS = List.of("Alpha", "Bravo");
    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");
    private static final List<String> EIGHT_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo",
            "Foxtrot", "Golf", "Hotel");

    private static final String BOTH_TEAMS_GONE_TEXT = "Both teams of match";

    @Test
    void findsNoWaitingMatchWithoutABracket() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);

        assertEquals(List.of(), fixture.tournamentManager().findWaitingMatches());

        fixture.startTournament();
        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertEquals(List.of(), fixture.tournamentManager().findWaitingMatches());
    }

    @Test
    void leavesTheRunningMatchOut() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);
        fixture.startTournament();

        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId());
        assertEquals(List.of("round1-match2"), waitingMatchIds(fixture));
    }

    @Test
    void listsTheWaitingMatchesByRoundAndPositionAfterDecisions() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(EIGHT_TEAMS);
        fixture.startTournament();
        assertEquals(List.of("round1-match2", "round1-match3", "round1-match4"), waitingMatchIds(fixture));

        decideForTheFirstTeam(fixture, "round1-match4");
        assertEquals(List.of("round1-match2", "round1-match3"), waitingMatchIds(fixture));

        decideForTheFirstTeam(fixture, "round1-match3");
        assertEquals(List.of("round1-match2", "round2-match2"), waitingMatchIds(fixture));

        fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId());
        assertEquals(List.of("round2-match2"), waitingMatchIds(fixture));

        fixture.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of("round2-match1"), fixture.runningMatchId());
        assertEquals(List.of("round2-match2"), waitingMatchIds(fixture));
    }

    @Test
    void listsTheReadyFinalAsWaiting() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);
        fixture.startTournament();

        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(List.of("round1-match1"), waitingMatchIds(fixture));
    }

    @Test
    void listsTheMatchThatRanBeforeTheRestartAsWaiting() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        before.startTournament();
        before.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of("round1-match2"), before.runningMatchId());

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals(List.of("round1-match2"), waitingMatchIds(after));
    }

    @Test
    void warnsTheAdminsOnceWhenBothTeamsOfTheNextMatchAreGone() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(EIGHT_TEAMS);
        fixture.startTournament();
        removeBothTeamsOf(fixture, "round1-match2");

        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(List.of(bothTeamsGoneWarning("round1-match2")),
                fixture.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT));
        assertEquals(Optional.empty(), fixture.runningMatchId(), "the tournament waits for the admin");
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());

        assertTrue(fixture.tournamentManager().togglePause().successful());
        assertTrue(fixture.tournamentManager().togglePause().successful());
        decideForTheFirstTeam(fixture, "round1-match3");

        assertEquals(1, fixture.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT).size(),
                "neither the continue nor the decision of another match repeats the warning: "
                        + fixture.adminMessages());
        assertEquals(Optional.empty(), fixture.runningMatchId());

        decideForTheFirstTeam(fixture, "round1-match2");

        assertEquals(Optional.of("round1-match4"), fixture.runningMatchId());
        assertEquals(1, fixture.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT).size(), fixture.adminMessages()
                .toString());
    }

    @Test
    void warnsForEveryMatchWhoseTeamsAreBothGone() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(EIGHT_TEAMS);
        fixture.startTournament();
        removeBothTeamsOf(fixture, "round1-match2");
        removeBothTeamsOf(fixture, "round1-match3");

        fixture.forceRunningMatchForTheFirstTeam();
        decideForTheFirstTeam(fixture, "round1-match2");

        assertEquals(List.of(bothTeamsGoneWarning("round1-match2"), bothTeamsGoneWarning("round1-match3")),
                fixture.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT));
        assertEquals(Optional.empty(), fixture.runningMatchId());

        decideForTheFirstTeam(fixture, "round1-match3");

        assertEquals(Optional.of("round1-match4"), fixture.runningMatchId());
        assertEquals(2, fixture.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT).size(), fixture.adminMessages()
                .toString());
    }

    @Test
    void warnsOnceWhenBothFinalistsAreGoneAndTheFinalIsStarted() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(TWO_TEAMS);
        fixture.startTournament();
        removeBothTeamsOf(fixture, "round1-match1");

        assertFalse(fixture.tournamentManager().startFinalMatch().successful());
        assertFalse(fixture.tournamentManager().startFinalMatch().successful());

        assertEquals(List.of(bothTeamsGoneWarning("round1-match1")),
                fixture.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT));
        assertEquals(List.of("round1-match1"), waitingMatchIds(fixture));

        decideForTheFirstTeam(fixture, "round1-match1");

        assertEquals(TournamentState.FINISHED, fixture.tournamentManager().getState());
    }

    @Test
    void warnsWhenBothTeamsOfAnOpenMatchWereRemovedWhileTheServerWasOff() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, EIGHT_TEAMS);
        before.startTournament();
        before.forceRunningMatchForTheFirstTeam();
        BracketMatch openMatch = before.bracketMatch("round1-match2");
        List<String> removedTeamNames = List.of(openMatch.getFirstTeamName().orElseThrow(),
                openMatch.getSecondTeamName().orElseThrow());
        List<String> remainingTeamNames = EIGHT_TEAMS.stream()
                .filter(teamName -> !removedTeamNames.contains(teamName))
                .toList();

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, remainingTeamNames);
        assertTrue(after.tournamentManager().restoreSavedTournament());
        assertEquals(List.of(), after.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT),
                "the restore itself starts no match");

        assertTrue(after.tournamentManager().togglePause().successful());

        assertEquals(List.of(bothTeamsGoneWarning("round1-match2")),
                after.adminMessagesContaining(BOTH_TEAMS_GONE_TEXT));
        assertEquals(Optional.empty(), after.runningMatchId());
        assertEquals(List.of("round1-match2", "round1-match3", "round1-match4"), waitingMatchIds(after));

        decideForTheFirstTeam(after, "round1-match2");

        assertEquals(Optional.of("round1-match3"), after.runningMatchId());
    }

    private static List<String> waitingMatchIds(TournamentManagerFixture fixture) {
        return fixture.tournamentManager().findWaitingMatches().stream().map(BracketMatch::getMatchId).toList();
    }

    private static void decideForTheFirstTeam(TournamentManagerFixture fixture, String matchId) {
        String winnerTeamName = fixture.bracketMatch(matchId).getFirstTeamName().orElseThrow();
        TournamentOperationResult decision = fixture.tournamentManager().forceMatchResult(matchId, winnerTeamName);
        assertTrue(decision.successful(), decision.message());
        assertEquals(Optional.of(winnerTeamName), fixture.bracketMatch(matchId).getWinnerTeamName());
    }

    private static void removeBothTeamsOf(TournamentManagerFixture fixture, String matchId) {
        BracketMatch bracketMatch = fixture.bracketMatch(matchId);
        assertTrue(fixture.teamRegistry().removeTeam(bracketMatch.getFirstTeamName().orElseThrow()));
        assertTrue(fixture.teamRegistry().removeTeam(bracketMatch.getSecondTeamName().orElseThrow()));
    }

    private static String bothTeamsGoneWarning(String matchId) {
        return "Both teams of match " + matchId + " are no longer registered, the tournament waits for you. "
                + "Decide the match in the match menu of /tournament under \"Decide a waiting match\"";
    }
}
