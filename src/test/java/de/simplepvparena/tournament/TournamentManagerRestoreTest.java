package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerRestoreTest {

    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");
    private static final List<String> EIGHT_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo",
            "Foxtrot", "Golf", "Hotel");

    @Test
    void restoresTheBracketPausedAndStartsNoMatchByItself() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        before.startTournament();
        String winnerTeamName = before.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of("round1-match2"), before.runningMatchId());
        assertEquals(TournamentState.RUNNING, storage.storedTournament().orElseThrow().state());

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals(TournamentState.PAUSED, after.tournamentManager().getState());
        assertEquals(Optional.empty(), after.runningMatchId(), "a restored tournament starts no match by itself");
        assertTrue(after.matchService().isRoundTimerPaused());
        assertEquals(Optional.of(winnerTeamName), after.bracketMatch("round1-match1").getWinnerTeamName());
        assertTrue(after.bracketMatch("round1-match2").isPlayable(),
                "the match that ran before the restart is not decided");
        for (BracketMatch beforeMatch : before.bracket().getAllMatches()) {
            BracketMatch afterMatch = after.bracketMatch(beforeMatch.getMatchId());
            assertEquals(beforeMatch.getFirstTeamName(), afterMatch.getFirstTeamName());
            assertEquals(beforeMatch.getSecondTeamName(), afterMatch.getSecondTeamName());
            assertEquals(beforeMatch.getWinnerTeamName(), afterMatch.getWinnerTeamName());
        }
        assertEquals(FinalMatchStage.PENDING, after.tournamentManager().getFinalMatchStage());
        List<String> restoreMessages = after.adminMessagesContaining("was restored");
        assertEquals(1, restoreMessages.size(), after.adminMessages().toString());
        assertTrue(restoreMessages.get(0).contains("matches decided: 1"), restoreMessages.get(0));
        assertTrue(restoreMessages.get(0).contains("rounds: 2"), restoreMessages.get(0));
        assertTrue(restoreMessages.get(0).contains("paused"), restoreMessages.get(0));
        assertTrue(restoreMessages.get(0).contains(InMemoryTournamentStateStorage.FILE_NAME), restoreMessages.get(0));

        TournamentOperationResult resume = after.tournamentManager().togglePause();

        assertTrue(resume.successful(), resume.message());
        assertEquals(TournamentState.RUNNING, after.tournamentManager().getState());
        assertFalse(after.matchService().isRoundTimerPaused());
        assertEquals(Optional.of("round1-match2"), after.runningMatchId());
    }

    @Test
    void restoresAReadyFinalAndLeavesItsStartToTheAdmin() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        before.startTournament();
        before.forceRunningMatchForTheFirstTeam();
        before.endRunningMatchAsPlayedByTheFirstTeam();
        assertEquals(FinalMatchStage.READY, before.tournamentManager().getFinalMatchStage());

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals(FinalMatchStage.READY, after.tournamentManager().getFinalMatchStage());
        assertEquals(1, after.adminMessagesContaining("is ready").size(), after.adminMessages().toString());
        assertFalse(after.tournamentManager().startFinalMatch().successful(), "the final waits for the continue");

        assertTrue(after.tournamentManager().togglePause().successful());

        assertEquals(Optional.empty(), after.runningMatchId(), "the final is started by an admin");
        assertTrue(after.tournamentManager().startFinalMatch().successful());
        assertEquals(Optional.of("round2-match1"), after.runningMatchId());
        assertEquals(List.of(Optional.of(MatchDecisionKind.FORCED), Optional.of(MatchDecisionKind.PLAYED)),
                storage.lastSavedTournament().results().stream().map(SavedMatchResult::decidedBy).toList(),
                "the kinds of the restored decisions are saved again");
    }

    @Test
    void warnsOnceAboutARemovedTeamAndHandsItsOpenMatchToTheOpponent() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(storage, EIGHT_TEAMS);
        before.startTournament();
        String removedTeamName = before.forceRunningMatchForTheFirstTeam();
        String opponentTeamName = before.forceRunningMatchForTheFirstTeam();
        before.forceRunningMatchForTheFirstTeam();
        before.forceRunningMatchForTheFirstTeam();
        assertEquals(Optional.of("round2-match1"), before.runningMatchId());
        List<String> remainingTeamNames = EIGHT_TEAMS.stream()
                .filter(teamName -> !teamName.equals(removedTeamName))
                .toList();

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(storage, remainingTeamNames);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        List<String> removedTeamWarnings = after.adminMessagesContaining("no longer registered");
        assertEquals(1, removedTeamWarnings.size(), after.adminMessages().toString());
        assertTrue(removedTeamWarnings.get(0).contains(removedTeamName), removedTeamWarnings.get(0));
        assertEquals(Optional.of(removedTeamName), after.bracketMatch("round1-match1").getWinnerTeamName(),
                "a decided match keeps the name of the removed team");

        assertTrue(after.tournamentManager().togglePause().successful());

        assertEquals(Optional.of(opponentTeamName), after.bracketMatch("round2-match1").getWinnerTeamName());
        assertEquals(Optional.of(MatchDecisionKind.WALKOVER),
                storage.lastSavedTournament().results().stream()
                        .filter(result -> result.matchId().equals("round2-match1"))
                        .findFirst().orElseThrow().decidedBy());
        assertEquals(Optional.of("round2-match2"), after.runningMatchId());
        assertEquals(1, after.adminMessagesContaining("no longer registered").size(),
                "the warning is not repeated when the tournament continues");
    }

    @Test
    void movesABrokenFileAsideAndStaysIdle(@TempDir Path pluginFolder) throws IOException {
        Path stateFile = pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME);
        Files.writeString(stateFile, "format-version: 7\n", StandardCharsets.UTF_8);
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(
                new FileTournamentStateStorage(stateFile), FOUR_TEAMS);

        assertFalse(fixture.tournamentManager().restoreSavedTournament());

        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertTrue(fixture.tournamentManager().getBracket().isEmpty());
        assertFalse(Files.exists(stateFile));
        assertTrue(Files.isRegularFile(pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME + ".broken")));
        assertEquals(List.of("The saved tournament in tournament.yml.broken could not be read: unknown "
                + "format-version 7. No tournament was restored"), fixture.adminMessages());
    }

    @Test
    void restoresFromTheFileAfterARestart(@TempDir Path pluginFolder) {
        Path stateFile = pluginFolder.resolve(FileTournamentStateStorage.FILE_NAME);
        TournamentManagerFixture before = TournamentManagerFixture.withStorage(
                new FileTournamentStateStorage(stateFile), EIGHT_TEAMS);
        before.startTournament();
        String firstWinnerTeamName = before.forceRunningMatchForTheFirstTeam();
        String secondWinnerTeamName = before.forceRunningMatchForTheFirstTeam();

        TournamentManagerFixture after = TournamentManagerFixture.withStorage(
                new FileTournamentStateStorage(stateFile), EIGHT_TEAMS);
        assertTrue(after.tournamentManager().restoreSavedTournament());

        assertEquals(TournamentState.PAUSED, after.tournamentManager().getState());
        assertEquals(Optional.of(firstWinnerTeamName), after.bracketMatch("round1-match1").getWinnerTeamName());
        assertEquals(Optional.of(secondWinnerTeamName), after.bracketMatch("round1-match2").getWinnerTeamName());
        assertTrue(after.bracketMatch("round2-match1").isPlayable());
        assertTrue(after.tournamentManager().togglePause().successful());
        assertEquals(Optional.of("round1-match3"), after.runningMatchId());
    }

    @Test
    void doesNotRestoreAStateThatDoesNotFitTogether() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        storage.store(new SavedTournament(Instant.EPOCH, Instant.EPOCH, TournamentState.RUNNING,
                List.of(new SavedFirstRoundMatch("round1-match1", Optional.of("Alpha"), Optional.of("Bravo")),
                        new SavedFirstRoundMatch("round1-match2", Optional.of("Charlie"), Optional.of("Delta"))),
                List.of(new SavedMatchResult("round1-match1", "Delta", Optional.empty()))));
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);

        assertFalse(fixture.tournamentManager().restoreSavedTournament());

        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertEquals(List.of("The saved tournament in memory.yml does not fit together: Delta does not take part "
                + "in match round1-match1. No tournament was restored"), fixture.adminMessages());
    }

    @Test
    void restoresNothingWhileATournamentIsInProgressOrAMatchStandsOnTheArena() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture saving = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        saving.startTournament();
        saving.forceRunningMatchForTheFirstTeam();

        TournamentManagerFixture running = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        running.startTournament();
        Bracket runningBracket = running.bracket();
        assertFalse(running.tournamentManager().restoreSavedTournament());
        assertEquals(TournamentState.RUNNING, running.tournamentManager().getState());
        assertSame(runningBracket, running.bracket(), "the running bracket is left alone");

        InMemoryTournamentStateStorage otherStorage = new InMemoryTournamentStateStorage();
        otherStorage.store(storage.savedTournaments().get(1));
        TournamentManagerFixture occupied = TournamentManagerFixture.withStorage(otherStorage, FOUR_TEAMS);
        occupied.startMatchOutsideTheTournament("Alpha", "Bravo");
        assertFalse(occupied.tournamentManager().restoreSavedTournament());
        assertEquals(TournamentState.IDLE, occupied.tournamentManager().getState());
        assertTrue(occupied.adminMessages().isEmpty(), occupied.adminMessages().toString());
    }

    @Test
    void doesNothingWhenNoStateIsSaved() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(new InMemoryTournamentStateStorage(),
                FOUR_TEAMS);

        assertFalse(fixture.tournamentManager().restoreSavedTournament());

        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertTrue(fixture.adminMessages().isEmpty(), fixture.adminMessages().toString());
    }
}
