package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSequenceTest {

    @Test
    void startsWithTheFirstMatchOfTheBracket() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));

        assertEquals("round1-match1", nextMatchId(bracket));
    }

    @Test
    void continuesWithTheNextMatchAfterTheRunningOneIsDecided() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));

        bracket.recordWinner("round1-match1", bracket.findMatch("round1-match1").orElseThrow()
                .getFirstTeamName().orElseThrow());

        assertEquals("round1-match2", nextMatchId(bracket));
    }

    @Test
    void leavesTheFinalToTheAdmin() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));
        decideMatch(bracket, "round1-match1");
        decideMatch(bracket, "round1-match2");

        assertTrue(bracket.getFinalMatch().isPlayable());
        assertTrue(MatchSequence.findNextMatch(bracket).isEmpty());
    }

    @Test
    void skipsAMatchThatIsStillWaitingForItsParticipants() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta",
                "Echo", "Foxtrot", "Golf", "Hotel"));
        decideMatch(bracket, "round1-match1");

        assertEquals("round1-match2", nextMatchId(bracket));
    }

    @Test
    void hasNoNextMatchInABracketThatOnlyHoldsTheFinal() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo"));

        assertTrue(MatchSequence.findNextMatch(bracket).isEmpty());
    }

    @Test
    void hasNoNextMatchWhenEveryMatchIsDecided() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));
        decideMatch(bracket, "round1-match1");
        decideMatch(bracket, "round1-match2");
        decideMatch(bracket, "round2-match1");

        assertTrue(bracket.isComplete());
        assertTrue(MatchSequence.findNextMatch(bracket).isEmpty());
    }

    private void decideMatch(Bracket bracket, String matchId) {
        bracket.recordWinner(matchId, bracket.findMatch(matchId).orElseThrow().getFirstTeamName().orElseThrow());
    }

    private String nextMatchId(Bracket bracket) {
        Optional<BracketMatch> nextMatch = MatchSequence.findNextMatch(bracket);
        assertTrue(nextMatch.isPresent());
        return nextMatch.get().getMatchId();
    }
}
