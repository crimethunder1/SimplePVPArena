package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.team.Team;
import de.simplepvparena.tournament.AutoMatchState;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.TournamentOperationResult;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchTournamentTest {

    private static final String AUTO_MATCH_RUNNING = "An auto match is running, start the tournament when it is over";
    private static final String TOURNAMENT_STARTING = "Auto match cancelled: a tournament is starting";

    @Test
    void aTournamentDoesNotStartWhileAnAutoMatchRuns() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        TournamentOperationResult start = harness.tournamentManager().startTournament(false);

        assertFalse(start.successful());
        assertEquals(AUTO_MATCH_RUNNING, start.message());
        assertEquals(TournamentState.IDLE, harness.tournamentManager().getState());
        assertTrue(harness.tournamentManager().getBracket().isEmpty());
        assertEquals(AutoMatchState.RUNNING, harness.state());
        assertEquals("auto-1", harness.runningMatchId().orElseThrow());
    }

    @Test
    void aTournamentDoesNotStartWhileTheArenaIsCleanedAfterAnAutoMatchButDuringThePause() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();
        harness.winMatch("Alpha");

        TournamentOperationResult refusedStart = harness.tournamentManager().startTournament(false);

        assertEquals(AUTO_MATCH_RUNNING, refusedStart.message());
        assertFalse(refusedStart.successful());

        harness.finishCleaning();
        harness.startTournament();
        harness.advanceSeconds(120);

        assertEquals(TournamentState.RUNNING, harness.tournamentManager().getState());
        assertEquals(AutoMatchState.IDLE, harness.state(), "the pause ends and the tournament has priority");
    }

    @Test
    void aTournamentStartCancelsTheAnnouncementAndStartsAsUsual() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        ArenaMatchHarness.FakePlayer bystander = harness.addPlayerInTheArea("Bystander");
        harness.switchOn();
        harness.advanceSeconds(1);
        harness.forgetReceivedTexts();
        bystander.forgetReceivedTexts();

        harness.startTournament();

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertEquals(TournamentState.RUNNING, harness.tournamentManager().getState());
        assertTrue(harness.tournamentManager().getBracket().isPresent());
        assertEquals(TOURNAMENT_STARTING, harness.messagesOf("Alpha").getFirst());
        assertTrue(bystander.messages().contains(TOURNAMENT_STARTING), bystander.messages().toString());

        harness.advanceSeconds(120);

        assertEquals(AutoMatchState.IDLE, harness.state());
        assertFalse(harness.matchService().hasActiveMatch(), "the final waits for an admin, no auto match starts");
    }

    @Test
    void aRefusedTournamentStartKeepsTheAnnouncement() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOn();
        harness.advanceSeconds(1);
        harness.forgetReceivedTexts();

        TournamentOperationResult start = harness.tournamentManager().startTournament(false);

        assertFalse(start.successful());
        assertEquals(AutoMatchState.ANNOUNCED, harness.state());
        assertEquals(List.of(), harness.messagesOf("Alpha"));
    }

    @Test
    void anAutoMatchNeverTouchesTheBracketOrTheSavedTournament() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.startTournament();
        harness.switchOn();
        assertTrue(harness.tournamentManager().startFinalMatch().successful());
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "admin"));
        harness.winMatch("Alpha");
        harness.finishCleaning();
        int storageWritesOfTheTournament = harness.tournamentStorageWriteCount();
        List<String> bracketBefore = describeBracket(harness);

        harness.advanceUntilTheAutoMatchRuns();

        assertEquals("Tournament state: FINISHED | rounds: 1 | running match: auto-1 | final: FINISHED | champion: "
                + "Alpha", harness.tournamentManager().describeStatus());
        assertEquals(List.of(), harness.tournamentManager().findWaitingMatches());

        harness.winMatch("Bravo");

        assertEquals(TournamentState.FINISHED, harness.tournamentManager().getState());
        assertEquals(Optional.of("Alpha"), harness.tournamentManager().getBracket().orElseThrow()
                .getChampionTeamName());
        assertEquals(bracketBefore, describeBracket(harness));
        assertEquals(storageWritesOfTheTournament, harness.tournamentStorageWriteCount());
        assertEquals(AutoMatchState.COOLDOWN, harness.state());
    }

    @Test
    void aTournamentMatchStillWritesItsWinnerIntoTheBracketWhileTheModeIsOn() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie", "Delta");
        harness.switchOn();
        harness.startTournament();
        assertTrue(harness.tournamentManager().getKitChoiceService().choose(AutoMatchHarness.DEFAULT_KIT_NAME,
                "admin"));
        Match tournamentMatch = harness.runningMatch();
        assertFalse(tournamentMatch.isAutoMatch());
        int storageWritesBefore = harness.tournamentStorageWriteCount();

        harness.winMatch(tournamentMatch.getFirstTeam().getTeamName());

        BracketMatch bracketMatch = harness.tournamentManager().getBracket().orElseThrow()
                .findMatch(tournamentMatch.getMatchId()).orElseThrow();
        assertEquals(Optional.of(tournamentMatch.getFirstTeam().getTeamName()), bracketMatch.getWinnerTeamName());
        assertTrue(harness.tournamentStorageWriteCount() > storageWritesBefore);
        assertTrue(harness.isCleaning());
        assertEquals(AutoMatchState.IDLE, harness.state());
    }

    @Test
    void theTeamsOfAnAnnouncedAutoMatchCannotBeChanged() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo", "Charlie");
        harness.switchOn();
        harness.advanceSeconds(1);

        assertEquals(Optional.of("Team Alpha is about to play an auto match, it cannot be changed before the match "
                + "is over"), harness.tournamentManager().findTeamChangeRejection("alpha"));
        assertEquals(Optional.of("Team Bravo is about to play an auto match, it cannot be changed before the match "
                + "is over"), harness.tournamentManager().findTeamRenameRejection("Bravo"));
        assertEquals(Optional.empty(), harness.tournamentManager().findTeamChangeRejection("Charlie"));

        harness.advanceSeconds(AutoMatchHarness.START_DELAY_SECONDS);

        assertEquals(Optional.of("Team Alpha is in match auto-1, it cannot be changed before the match is over"),
                harness.tournamentManager().findTeamChangeRejection("Alpha"));

        harness.winMatch("Alpha");

        assertEquals(Optional.empty(), harness.tournamentManager().findTeamChangeRejection("Alpha"));
    }

    @Test
    void aCancelledAnnouncementFreesTheTeamsAgain() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceSeconds(1);

        harness.switchOff();

        assertEquals(Optional.empty(), harness.tournamentManager().findTeamChangeRejection("Alpha"));
    }

    @Test
    void anAdminForcesTheResultOfAnAutoMatchWithoutATournament() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        TournamentOperationResult decision = harness.tournamentManager().forceMatchResult("auto-1", "bravo");

        assertTrue(decision.successful(), decision.message());
        assertEquals("Match auto-1 was decided for Bravo", decision.message());
        assertFalse(harness.matchService().hasActiveMatch());
        assertEquals(AutoMatchState.COOLDOWN, harness.state());
        assertTrue(harness.tournamentManager().getBracket().isEmpty());
        assertEquals(TournamentState.IDLE, harness.tournamentManager().getState());
    }

    @Test
    void anOtherMatchWithoutATournamentStillCannotBeForced() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        Team alpha = harness.teamRegistry().findTeam("Alpha").orElseThrow();
        Team bravo = harness.teamRegistry().findTeam("Bravo").orElseThrow();
        harness.matchService().startMatch(Match.regularMatch("M1", alpha, bravo,
                harness.arena().getArenaRegistry().getArena()));

        TournamentOperationResult decision = harness.tournamentManager().forceMatchResult("M1", "Alpha");

        assertFalse(decision.successful());
        assertEquals("No tournament is in progress", decision.message());
        assertTrue(harness.matchService().hasActiveMatch());
    }

    private static List<String> describeBracket(AutoMatchHarness harness) {
        return harness.tournamentManager().getBracket().orElseThrow().getRounds().stream()
                .flatMap(List::stream)
                .map(match -> match.getMatchId() + " " + match.getFirstTeamName() + " " + match.getSecondTeamName()
                        + " " + match.getWinnerTeamName())
                .toList();
    }
}
