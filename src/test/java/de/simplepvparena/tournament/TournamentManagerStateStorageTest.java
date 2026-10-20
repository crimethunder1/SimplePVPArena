package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerStateStorageTest {

    private static final List<String> FOUR_TEAMS = List.of("Alpha", "Bravo", "Charlie", "Delta");

    @Test
    void savesTheBracketWhenTheTournamentStarts() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage,
                List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot"));

        fixture.startTournament();

        assertEquals(2, fixture.bracket().getTeamNamesWithFirstRoundBye().size());
        assertEquals(1, storage.savedTournaments().size());
        SavedTournament savedTournament = storage.lastSavedTournament();
        assertEquals(TournamentState.RUNNING, savedTournament.state());
        assertEquals(fixture.bracket().describeFirstRound(), savedTournament.firstRound());
        assertEquals(List.of(), savedTournament.results(), "the byes follow from the empty slots");
        assertFalse(savedTournament.startedAt().isAfter(savedTournament.savedAt()));
    }

    @Test
    void savesAPlayedResultAndTheWalkoverItLeadsTo() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        fixture.startTournament();
        String removedTeamName = fixture.bracketMatch("round1-match2").getFirstTeamName().orElseThrow();
        String opponentTeamName = fixture.bracketMatch("round1-match2").getSecondTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry().removeTeam(removedTeamName));

        String winnerTeamName = fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertEquals(3, storage.savedTournaments().size(), "the start, the played match and the walkover");
        assertEquals(List.of(new SavedMatchResult("round1-match1", winnerTeamName,
                        Optional.of(MatchDecisionKind.PLAYED))),
                storage.savedTournaments().get(1).results());
        assertEquals(List.of(new SavedMatchResult("round1-match1", winnerTeamName,
                                Optional.of(MatchDecisionKind.PLAYED)),
                        new SavedMatchResult("round1-match2", opponentTeamName,
                                Optional.of(MatchDecisionKind.WALKOVER))),
                storage.lastSavedTournament().results());
    }

    @Test
    void savesAForcedResultOfAWaitingAndOfARunningMatch() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        fixture.startTournament();
        String waitingWinnerTeamName = fixture.bracketMatch("round1-match2").getSecondTeamName().orElseThrow();

        assertTrue(fixture.tournamentManager().forceMatchResult("round1-match2", waitingWinnerTeamName)
                .successful());
        assertEquals(List.of(new SavedMatchResult("round1-match2", waitingWinnerTeamName,
                Optional.of(MatchDecisionKind.FORCED))), storage.lastSavedTournament().results());

        String runningWinnerTeamName = fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(List.of(new SavedMatchResult("round1-match2", waitingWinnerTeamName,
                                Optional.of(MatchDecisionKind.FORCED)),
                        new SavedMatchResult("round1-match1", runningWinnerTeamName,
                                Optional.of(MatchDecisionKind.FORCED))),
                storage.lastSavedTournament().results());
    }

    @Test
    void savesTheStateWhenTheTournamentIsPausedAndContinued() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        fixture.startTournament();

        assertTrue(fixture.tournamentManager().togglePause().successful());
        assertEquals(TournamentState.PAUSED, storage.lastSavedTournament().state());

        assertTrue(fixture.tournamentManager().togglePause().successful());
        assertEquals(TournamentState.RUNNING, storage.lastSavedTournament().state());
        assertEquals(3, storage.savedTournaments().size());
    }

    @Test
    void deletesTheSavedStateWhenTheTournamentIsCancelled() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        fixture.startTournament();

        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertEquals(1, storage.deleteCount());
        assertTrue(storage.storedTournament().isEmpty());
    }

    @Test
    void deletesTheSavedStateWhenTheTournamentIsOver() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, List.of("Alpha", "Bravo"));
        fixture.startTournament();
        assertTrue(fixture.tournamentManager().startFinalMatch().successful());
        int saveCountBeforeTheFinal = storage.savedTournaments().size();

        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(TournamentState.FINISHED, fixture.tournamentManager().getState());
        assertEquals(1, storage.deleteCount());
        assertTrue(storage.storedTournament().isEmpty());
        assertEquals(saveCountBeforeTheFinal, storage.savedTournaments().size(),
                "a finished tournament is not saved again");
    }

    @Test
    void overwritesTheSavedStateWithANewStart() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, List.of("Alpha", "Bravo"));
        storage.store(new SavedTournament(Instant.EPOCH, Instant.EPOCH, TournamentState.PAUSED,
                List.of(new SavedFirstRoundMatch("round1-match1", Optional.of("Yankee"), Optional.of("Zulu"))),
                List.of()));

        fixture.startTournament();

        assertEquals(fixture.bracket().describeFirstRound(), storage.storedTournament().orElseThrow().firstRound());
    }

    @Test
    void carriesOnAndWarnsWhenTheStateCannotBeSavedAndTriesAgainWithTheNextDecision() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, FOUR_TEAMS);
        storage.failSaves(true);

        fixture.startTournament();

        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertEquals(Optional.of("round1-match1"), fixture.runningMatchId(), "the failed save holds nothing back");
        assertEquals(List.of("The tournament state could not be saved, the tournament carries on and the next "
                + "decision tries again: " + InMemoryTournamentStateStorage.SAVE_FAILURE),
                fixture.adminMessagesContaining("could not be saved"));

        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(Optional.of("round1-match2"), fixture.runningMatchId(), "the next match starts all the same");
        assertEquals(2, storage.failedSaveCount());
        assertEquals(2, fixture.adminMessagesContaining("could not be saved").size());

        storage.failSaves(false);
        fixture.forceRunningMatchForTheFirstTeam();

        assertEquals(2, storage.lastSavedTournament().results().size(),
                "the next save writes the whole state, the decisions of the failed saves included");
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
    }

    @Test
    void writesNothingWithoutAStorageAndRestoresNothing() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(FOUR_TEAMS);

        assertFalse(fixture.tournamentManager().restoreSavedTournament());
        fixture.startTournament();
        fixture.forceRunningMatchForTheFirstTeam();
        assertTrue(fixture.tournamentManager().togglePause().successful());
        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        assertEquals(TournamentState.CANCELLED, fixture.tournamentManager().getState());
        assertTrue(fixture.adminMessagesContaining("saved").isEmpty(), fixture.adminMessages().toString());
        assertTrue(fixture.adminMessagesContaining("restored").isEmpty(), fixture.adminMessages().toString());
    }

    @Test
    void theStorageThatDoesNothingKeepsNothing() throws Exception {
        Bracket bracket = Bracket.createSingleElimination(FOUR_TEAMS);

        TournamentStateStorage.NONE.save(SavedTournament.capture(bracket, TournamentState.RUNNING, Instant.EPOCH,
                Instant.EPOCH, Map.of()));

        assertEquals(TournamentStateLookup.Outcome.MISSING, TournamentStateStorage.NONE.find().outcome());
        TournamentStateStorage.NONE.delete();
    }
}
