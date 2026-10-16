package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        assertEquals(List.of("alpha"), List.copyOf(bracket.getTeamNamesWithFirstRoundBye()));
        assertEquals("alpha", bracket.findMatch("round1-match1").orElseThrow().getWinnerTeamName().orElseThrow());
        assertEquals("alpha", bracket.findMatch("round2-match1").orElseThrow().getFirstTeamName().orElseThrow());
        assertTrue(bracket.findMatch("round1-match2").orElseThrow().isPlayable());
    }

    @Test
    void distributesByesAcrossBothHalvesOfTheFirstRound() {
        Bracket bracket = Bracket.createSingleElimination(
                List.of("alpha", "bravo", "charlie", "delta", "echo", "foxtrot"));

        assertEquals(3, bracket.getRoundCount());
        assertEquals(List.of(1, 8, 4, 5, 2, 7, 3, 6), Bracket.buildSeedingOrder(8));
        assertEquals(Set.of("alpha", "bravo"), bracket.getTeamNamesWithFirstRoundBye());
        assertEquals("alpha", bracket.findMatch("round1-match1").orElseThrow().getWinnerTeamName().orElseThrow(),
                "match 1 holds the seeds 1 and 8, seed 8 stays empty, so seed 1 advances without opponent");
        assertEquals("bravo", bracket.findMatch("round1-match3").orElseThrow().getWinnerTeamName().orElseThrow(),
                "match 3 holds the seeds 2 and 7, seed 7 stays empty, so seed 2 advances without opponent");
        assertEquals(2, bracket.getPlayableMatches().size());
        assertTrue(bracket.findMatch("round1-match2").orElseThrow().isPlayable(), "seed 4 against seed 5");
        assertTrue(bracket.findMatch("round1-match4").orElseThrow().isPlayable(), "seed 3 against seed 6");
        assertTrue(bracket.findMatch("round2-match1").orElseThrow().getSecondTeamName().isEmpty(),
                "the bye in the upper half meets the winner of a played match, not another bye");
        assertTrue(bracket.findMatch("round2-match2").orElseThrow().getSecondTeamName().isEmpty(),
                "the bye in the lower half meets the winner of a played match, not another bye");
    }

    @Test
    void listsTheFirstRoundByesInTheOrderOfTheirMatches() {
        Bracket bracket = Bracket.createSingleElimination(
                List.of("foxtrot", "echo", "delta", "charlie", "bravo", "alpha"));

        assertEquals(List.of("foxtrot", "echo"), List.copyOf(bracket.getTeamNamesWithFirstRoundBye()),
                "foxtrot advances from match 1, echo from match 3");
        assertThrows(UnsupportedOperationException.class,
                () -> bracket.getTeamNamesWithFirstRoundBye().add("golf"));
    }

    @Test
    void identifiesTheSingleMatchOfTheLastRoundAsTheFinal() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

        assertEquals("round2-match1", bracket.getFinalMatch().getMatchId());
        assertTrue(bracket.isFinalMatch(bracket.findMatch("round2-match1").orElseThrow()));
        assertFalse(bracket.isFinalMatch(bracket.findMatch("round1-match1").orElseThrow()));
        assertFalse(bracket.isFinalMatch(bracket.findMatch("round1-match2").orElseThrow()));
    }

    @Test
    void identifiesTheOnlyMatchOfATwoTeamBracketAsTheFinal() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo"));

        assertEquals(1, bracket.getRoundCount());
        assertTrue(bracket.isFinalMatch(bracket.findMatch("round1-match1").orElseThrow()));
    }

    @Test
    void identifiesTheFinalOfABracketWithByesByItsLastRound() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta", "echo"));

        assertEquals(3, bracket.getRoundCount());
        assertEquals("round3-match1", bracket.getFinalMatch().getMatchId());
        assertFalse(bracket.isFinalMatch(bracket.findMatch("round2-match2").orElseThrow()));
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
