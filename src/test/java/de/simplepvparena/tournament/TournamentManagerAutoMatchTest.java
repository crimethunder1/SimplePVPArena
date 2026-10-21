package de.simplepvparena.tournament;

import de.simplepvparena.match.Match;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentManagerAutoMatchTest {

    @Test
    void theTournamentIgnoresTheEndOfAnAutoMatch() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage,
                List.of("Alpha", "Bravo", "Charlie", "Delta"));
        fixture.startTournament();
        Match runningMatch = fixture.matchService().getActiveMatch().orElseThrow();
        int savedTournamentCount = storage.savedTournaments().size();
        int adminMessageCount = fixture.adminMessages().size();
        Match autoMatch = Match.autoMatch("auto-1", runningMatch.getFirstTeam(), runningMatch.getSecondTeam(),
                runningMatch.getArena());

        fixture.tournamentManager().onMatchCompleted(autoMatch, runningMatch.getFirstTeam().getTeamName());

        assertTrue(fixture.bracket().getAllMatches().stream().noneMatch(BracketMatch::isDecided));
        assertEquals(savedTournamentCount, storage.savedTournaments().size());
        assertEquals(adminMessageCount, fixture.adminMessages().size(), "no cleaning notice for an auto match");
        assertFalse(fixture.isArenaBeingCleaned());
        assertFalse(fixture.tournamentManager().isWaitingForArenaCleaning());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertEquals(runningMatch, fixture.matchService().getActiveMatch().orElseThrow());
    }

    @Test
    void theEndOfATournamentMatchStillReachesTheBracket() {
        InMemoryTournamentStateStorage storage = new InMemoryTournamentStateStorage();
        TournamentManagerFixture fixture = TournamentManagerFixture.withStorage(storage,
                List.of("Alpha", "Bravo", "Charlie", "Delta"));
        fixture.startTournament();
        int savedTournamentCount = storage.savedTournaments().size();

        String winnerTeamName = fixture.endRunningMatchAsPlayedByTheFirstTeam();

        assertEquals(winnerTeamName, fixture.bracketMatch("round1-match1").getWinnerTeamName().orElseThrow());
        assertTrue(storage.savedTournaments().size() > savedTournamentCount);
    }

    @Test
    void anAutoMatchCarriesItsMarkAndAMatchOfTheTournamentDoesNot() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(List.of("Alpha", "Bravo",
                "Charlie", "Delta"));
        fixture.startTournament();
        Match runningMatch = fixture.matchService().getActiveMatch().orElseThrow();

        Match autoMatch = Match.autoMatch("auto-7", runningMatch.getFirstTeam(), runningMatch.getSecondTeam(),
                runningMatch.getArena());

        assertFalse(runningMatch.isAutoMatch());
        assertTrue(autoMatch.isAutoMatch());
        assertFalse(autoMatch.isFinalMatch());
        assertEquals("auto-7", autoMatch.getMatchId());
    }
}
