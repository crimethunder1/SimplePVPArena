package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketRevokeTest {

    private static final List<String> FOUR_TEAMS = List.of("alpha", "bravo", "charlie", "delta");
    private static final List<String> EIGHT_TEAMS = List.of("alpha", "bravo", "charlie", "delta", "echo",
            "foxtrot", "golf", "hotel");
    private static final Instant STARTED_AT = Instant.parse("2026-10-22T18:00:00Z");
    private static final Instant SAVED_AT = Instant.parse("2026-10-22T18:30:00Z");

    @Test
    void takesBackAFirstRoundResultOfFourTeamsAndEmptiesTheFollowingMatch() {
        Bracket bracket = Bracket.createSingleElimination(FOUR_TEAMS);
        bracket.recordWinner("round1-match1", "alpha");

        bracket.revokeWinner("round1-match1");

        BracketMatch match = bracket.findMatch("round1-match1").orElseThrow();
        assertFalse(match.isDecided());
        assertTrue(match.isPlayable());
        assertEquals(Optional.of("alpha"), match.getFirstTeamName());
        assertEquals(Optional.of("delta"), match.getSecondTeamName());
        BracketMatch finalMatch = bracket.getFinalMatch();
        assertEquals(Optional.empty(), finalMatch.getFirstTeamName());
        assertEquals(Optional.empty(), finalMatch.getSecondTeamName());
        assertEquals(List.of(), bracket.getMatchesInDecisionOrder());
        assertEquals(List.of("round1-match1", "round1-match2"), playableMatchIds(bracket));
    }

    @Test
    void keepsTheWinnerOfTheOtherHalfInTheFinalOfFourTeams() {
        Bracket bracket = Bracket.createSingleElimination(FOUR_TEAMS);
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "charlie");
        assertTrue(bracket.getFinalMatch().isPlayable());

        bracket.revokeWinner("round1-match2");

        BracketMatch finalMatch = bracket.getFinalMatch();
        assertEquals(Optional.of("alpha"), finalMatch.getFirstTeamName());
        assertEquals(Optional.empty(), finalMatch.getSecondTeamName());
        assertFalse(finalMatch.isPlayable());
        assertEquals(List.of("round1-match1"), decisionOrder(bracket));
        assertEquals(List.of("round1-match2"), playableMatchIds(bracket));
    }

    @Test
    void takesBackAFirstRoundResultOfEightTeamsAndLeavesTheOtherMatchesAlone() {
        Bracket bracket = Bracket.createSingleElimination(EIGHT_TEAMS);
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "echo");
        bracket.recordWinner("round1-match3", "golf");
        bracket.recordWinner("round1-match4", "charlie");

        bracket.revokeWinner("round1-match3");

        BracketMatch secondSemifinal = bracket.findMatch("round2-match2").orElseThrow();
        assertEquals(Optional.empty(), secondSemifinal.getFirstTeamName());
        assertEquals(Optional.of("charlie"), secondSemifinal.getSecondTeamName());
        BracketMatch firstSemifinal = bracket.findMatch("round2-match1").orElseThrow();
        assertEquals(Optional.of("alpha"), firstSemifinal.getFirstTeamName());
        assertEquals(Optional.of("echo"), firstSemifinal.getSecondTeamName());
        assertEquals(List.of("round1-match1", "round1-match2", "round1-match4"), decisionOrder(bracket));
        assertEquals(List.of("round1-match3", "round2-match1"), playableMatchIds(bracket));
    }

    @Test
    void decidesATakenBackMatchAgainForTheOtherTeam() {
        Bracket bracket = Bracket.createSingleElimination(EIGHT_TEAMS);
        bracket.recordWinner("round1-match3", "golf");
        bracket.recordWinner("round1-match4", "charlie");
        bracket.revokeWinner("round1-match3");

        bracket.recordWinner("round1-match3", "bravo");

        BracketMatch secondSemifinal = bracket.findMatch("round2-match2").orElseThrow();
        assertEquals(Optional.of("bravo"), secondSemifinal.getFirstTeamName());
        assertEquals(Optional.of("charlie"), secondSemifinal.getSecondTeamName());
        assertEquals(List.of("round1-match4", "round1-match3"), decisionOrder(bracket));
    }

    @Test
    void takesBackASecondRoundResultBeforeTheFinal() {
        Bracket bracket = Bracket.createSingleElimination(EIGHT_TEAMS);
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "echo");
        bracket.recordWinner("round2-match1", "echo");

        bracket.revokeWinner("round2-match1");

        assertTrue(bracket.findMatch("round2-match1").orElseThrow().isPlayable());
        assertEquals(Optional.empty(), bracket.getFinalMatch().getFirstTeamName());
        assertEquals(List.of("round1-match1", "round1-match2"), decisionOrder(bracket));
    }

    @Test
    void findsTheFollowingMatchOfEveryRoundAndNoneForTheFinal() {
        Bracket bracket = Bracket.createSingleElimination(EIGHT_TEAMS);

        assertEquals("round2-match1", followUpMatchId(bracket, "round1-match2"));
        assertEquals("round2-match2", followUpMatchId(bracket, "round1-match3"));
        assertEquals("round3-match1", followUpMatchId(bracket, "round2-match2"));
        assertEquals(Optional.empty(), bracket.findFollowUpMatch(bracket.getFinalMatch()));
    }

    @Test
    void rejectsAnUnknownMatch() {
        Bracket bracket = Bracket.createSingleElimination(FOUR_TEAMS);

        assertThrows(IllegalArgumentException.class, () -> bracket.revokeWinner("round9-match1"));
    }

    @Test
    void rejectsAMatchThatIsNotDecided() {
        Bracket bracket = Bracket.createSingleElimination(FOUR_TEAMS);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> bracket.revokeWinner("round1-match1"));

        assertEquals("match round1-match1 is not decided", failure.getMessage());
    }

    @Test
    void rejectsABye() {
        Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> bracket.revokeWinner("round1-match1"));

        assertEquals("match round1-match1 is decided by a bye", failure.getMessage());
        assertEquals(Optional.of("alpha"), bracket.getFinalMatch().getFirstTeamName());
    }

    @Test
    void rejectsAResultWhoseFollowingMatchIsDecidedAndChangesNothing() {
        Bracket bracket = Bracket.createSingleElimination(EIGHT_TEAMS);
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "echo");
        bracket.recordWinner("round2-match1", "echo");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> bracket.revokeWinner("round1-match2"));

        assertEquals("the following match round2-match1 is already decided", failure.getMessage());
        assertEquals(Optional.of("echo"), bracket.findMatch("round1-match2").orElseThrow().getWinnerTeamName());
        assertEquals(Optional.of("echo"), bracket.findMatch("round2-match1").orElseThrow().getSecondTeamName());
        assertEquals(List.of("round1-match1", "round1-match2", "round2-match1"), decisionOrder(bracket));
    }

    @Test
    void capturesNoResultForATakenBackMatchAndRestoresTheSameBracket() {
        Bracket bracket = Bracket.createSingleElimination(EIGHT_TEAMS);
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "echo");
        bracket.recordWinner("round1-match4", "charlie");
        bracket.revokeWinner("round1-match1");

        SavedTournament savedTournament = SavedTournament.capture(bracket, TournamentState.RUNNING, STARTED_AT,
                SAVED_AT, Map.of("round1-match1", MatchDecisionKind.FORCED, "round1-match2",
                        MatchDecisionKind.PLAYED, "round1-match4", MatchDecisionKind.WALKOVER));
        Bracket restored = savedTournament.restoreBracket();

        assertEquals(List.of(
                new SavedMatchResult("round1-match2", "echo", Optional.of(MatchDecisionKind.PLAYED)),
                new SavedMatchResult("round1-match4", "charlie", Optional.of(MatchDecisionKind.WALKOVER))),
                savedTournament.results());
        assertTrue(restored.findMatch("round1-match1").orElseThrow().isPlayable());
        assertEquals(Optional.empty(), restored.findMatch("round2-match1").orElseThrow().getFirstTeamName());
        assertEquals(Optional.of("echo"), restored.findMatch("round2-match1").orElseThrow().getSecondTeamName());
        assertEquals(decisionOrder(bracket), decisionOrder(restored));
        assertEquals(playableMatchIds(bracket), playableMatchIds(restored));
    }

    private static String followUpMatchId(Bracket bracket, String matchId) {
        return bracket.findFollowUpMatch(bracket.findMatch(matchId).orElseThrow()).orElseThrow().getMatchId();
    }

    private static List<String> decisionOrder(Bracket bracket) {
        return bracket.getMatchesInDecisionOrder().stream().map(BracketMatch::getMatchId).toList();
    }

    private static List<String> playableMatchIds(Bracket bracket) {
        return bracket.getPlayableMatches().stream().map(BracketMatch::getMatchId).toList();
    }
}
