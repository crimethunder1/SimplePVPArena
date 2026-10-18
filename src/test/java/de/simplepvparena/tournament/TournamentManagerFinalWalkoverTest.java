package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerFinalWalkoverTest {

    private static final List<String> TWO_TEAMS = List.of("Alpha", "Bravo");
    private static final String BOTH_FINALISTS_GONE_REFUSAL = "The final cannot be started, at least one finalist "
            + "is no longer registered";

    @Test
    void reportsTheFinalDecidedByAWalkoverAsASuccessAndEndsTheTournament() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, TWO_TEAMS);
        fixture.startTournament();
        BracketMatch finalMatch = fixture.bracket().getFinalMatch();
        String remainingTeamName = finalMatch.getFirstTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry().removeTeam(finalMatch.getSecondTeamName().orElseThrow()));
        int saveCountBeforeTheFinal = storage.savedTournaments().size();

        TournamentOperationResult finalStart = fixture.tournamentManager().startFinalMatch();

        assertTrue(finalStart.successful(), finalStart.message());
        assertEquals("The final was decided by a walkover, " + remainingTeamName + " wins the tournament",
                finalStart.message());
        assertEquals(TournamentState.FINISHED, fixture.tournamentManager().getState());
        assertEquals(Optional.of(remainingTeamName), fixture.bracket().getChampionTeamName());
        assertEquals(FinalMatchStage.FINISHED, fixture.tournamentManager().getFinalMatchStage());
        assertEquals(Optional.empty(), fixture.runningMatchId());
        assertEquals(1, storage.deleteCount());
        assertTrue(storage.storedTournament().isEmpty());
        assertEquals(saveCountBeforeTheFinal, storage.savedTournaments().size(),
                "a finished tournament is not saved again");
    }

    @Test
    void decidesTheFinalByAWalkoverWithoutBeginningAKitChoice() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withKitChoice(TWO_TEAMS);
        fixture.startTournament();
        BracketMatch finalMatch = fixture.bracket().getFinalMatch();
        String remainingTeamName = finalMatch.getSecondTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry().removeTeam(finalMatch.getFirstTeamName().orElseThrow()));

        TournamentOperationResult finalStart = fixture.tournamentManager().startFinalMatch();

        assertTrue(finalStart.successful(), finalStart.message());
        assertEquals("The final was decided by a walkover, " + remainingTeamName + " wins the tournament",
                finalStart.message());
        assertEquals(TournamentState.FINISHED, fixture.tournamentManager().getState());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
        assertEquals(List.of(), fixture.kitChoicePresenter().shownChoices());
    }

    @Test
    void keepsRefusingTheFinalWhenBothFinalistsAreGone() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage, TWO_TEAMS);
        fixture.startTournament();
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));
        assertTrue(fixture.teamRegistry().removeTeam("Bravo"));

        TournamentOperationResult finalStart = fixture.tournamentManager().startFinalMatch();

        assertFalse(finalStart.successful());
        assertEquals(BOTH_FINALISTS_GONE_REFUSAL, finalStart.message());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertEquals(FinalMatchStage.READY, fixture.tournamentManager().getFinalMatchStage());
        assertFalse(fixture.bracket().getFinalMatch().isDecided());
        assertEquals(0, storage.deleteCount());
        assertTrue(storage.storedTournament().isPresent());
    }
}
