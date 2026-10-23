package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketFirstRoundByeTest {

    @Test
    void recognizesExactlyTheFirstRoundMatchesWithASingleTeamAsByes() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie"));

        List<String> byeMatchIds = bracket.getAllMatches().stream()
                .filter(BracketMatch::isFirstRoundBye)
                .map(BracketMatch::getMatchId)
                .toList();

        assertEquals(1, byeMatchIds.size(), byeMatchIds.toString());
        BracketMatch bye = bracket.findMatch(byeMatchIds.getFirst()).orElseThrow();
        assertEquals(0, bye.getRoundIndex());
        assertTrue(bye.hasExactlyOneParticipant());
    }

    @Test
    void aFollowingMatchWithOneTeamIsNoBye() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));
        bracket.recordWinner("round1-match1", "alpha");

        BracketMatch finalMatch = bracket.getFinalMatch();

        assertTrue(finalMatch.hasExactlyOneParticipant());
        assertFalse(finalMatch.isFirstRoundBye());
        assertFalse(bracket.findMatch("round1-match1").orElseThrow().isFirstRoundBye());
    }

    @Test
    void theTournamentRefusesToTakeBackTheResultOfEveryMatchThatTheBracketViewShowsAsBye() {
        TournamentManagerFixture fixture = TournamentManagerFixture.withoutStorage(List.of());
        for (String teamName : List.of("Alpha", "Bravo", "Charlie")) {
            fixture.registerTeamOf(teamName, 2);
        }
        assertTrue(fixture.tournamentManager().startTournament(true).successful());
        Bracket bracket = fixture.tournamentManager().getBracket().orElseThrow();

        List<String> rejections = bracket.getAllMatches().stream()
                .filter(BracketMatch::isFirstRoundBye)
                .map(match -> fixture.tournamentManager().findRevokeRejection(match.getMatchId()).orElseThrow())
                .toList();

        assertEquals(List.of("A bye cannot be taken back"), rejections);
    }
}
