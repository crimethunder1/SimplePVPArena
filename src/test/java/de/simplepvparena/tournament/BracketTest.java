package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketTest {

    @Test
    void createsTwoRoundsForFourTeams() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

        assertEquals(2, bracket.getRoundCount());
        assertEquals(2, bracket.getPlayableMatches().size());
        assertTrue(bracket.getTeamNamesWithFirstRoundBye().isEmpty());
    }

    @Test
    void createsThreeRoundsForEightTeams() {
        Bracket bracket = Bracket.createSingleElimination(
                List.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel"));

        assertEquals(3, bracket.getRoundCount());
        assertEquals(4, bracket.getPlayableMatches().size());
    }

    @Test
    void advancesWinnerIntoTheNextRound() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

        bracket.recordWinner("round1-match1", winnerCandidate(bracket, "round1-match1"));
        assertFalse(bracket.findMatch("round2-match1").orElseThrow().isPlayable());

        bracket.recordWinner("round1-match2", winnerCandidate(bracket, "round1-match2"));
        BracketMatch finalMatch = bracket.findMatch("round2-match1").orElseThrow();
        assertTrue(finalMatch.isPlayable());
        assertTrue(finalMatch.hasBothParticipants());
    }

    @Test
    void producesChampionAfterTheFinalMatch() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "bravo");
        assertFalse(bracket.isComplete());

        bracket.recordWinner("round2-match1", "alpha");
        assertTrue(bracket.isComplete());
        assertEquals("alpha", bracket.getChampionTeamName().orElseThrow());
    }

    @Test
    void advancesTeamsWithoutOpponentAutomatically() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie"));

        assertEquals(2, bracket.getRoundCount());
        assertEquals(1, bracket.getPlayableMatches().size());
        assertEquals(List.of("bravo"), List.copyOf(bracket.getTeamNamesWithFirstRoundBye()));
        assertEquals("bravo", bracket.findMatch("round1-match2").orElseThrow().getWinnerTeamName().orElseThrow());
        assertEquals("bravo", bracket.findMatch("round2-match1").orElseThrow().getSecondTeamName().orElseThrow());
    }

    @Test
    void distributesByesAcrossAllFirstRoundMatches() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot"));

        assertEquals(3, bracket.getRoundCount());
        assertEquals(2, bracket.getTeamNamesWithFirstRoundBye().size());
        assertEquals(3, bracket.getPlayableMatches().size());
        assertTrue(bracket.findMatch("round2-match2").orElseThrow().isPlayable());
    }

    @Test
    void rejectsWinnerThatDoesNotTakePartInTheMatch() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

        assertThrows(IllegalArgumentException.class, () -> bracket.recordWinner("round1-match1", "echo"));
    }

    @Test
    void rejectsWinnerForAnAlreadyDecidedMatch() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));
        bracket.recordWinner("round1-match1", "alpha");

        assertThrows(IllegalStateException.class, () -> bracket.recordWinner("round1-match1", "charlie"));
    }

    @Test
    void rejectsTooFewTeams() {
        assertThrows(IllegalArgumentException.class, () -> Bracket.createSingleElimination(List.of("alpha")));
    }

    @Test
    void rejectsDuplicateTeamNames() {
        assertThrows(IllegalArgumentException.class,
                () -> Bracket.createSingleElimination(List.of("alpha", "alpha", "bravo", "charlie")));
    }

    private String winnerCandidate(Bracket bracket, String matchId) {
        return bracket.findMatch(matchId).orElseThrow().getFirstTeamName().orElseThrow();
    }
}
