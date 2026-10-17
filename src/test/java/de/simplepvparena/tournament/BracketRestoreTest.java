package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketRestoreTest {

    private static final int SEEDS_PER_TEAM_COUNT = 5;

    @Test
    void restoresEveryMatchOfBracketsFromTwoToTwentyTeams() {
        for (int teamCount = 2; teamCount <= 20; teamCount++) {
            for (int seed = 0; seed < SEEDS_PER_TEAM_COUNT; seed++) {
                Random random = new Random(teamCount * 1000L + seed);
                Bracket original = Bracket.createSingleElimination(numberedTeamNames(teamCount));
                decideSomeMatches(original, random);

                Bracket restored = Bracket.restore(original.describeFirstRound(), resultsOf(original));

                assertSameBracket(original, restored, teamCount + " teams, seed " + seed);
            }
        }
    }

    @Test
    void keepsTheOrderOfTheDecisions() {
        Bracket original = Bracket.createSingleElimination(numberedTeamNames(8));
        decideMatch(original, "round1-match3");
        decideMatch(original, "round1-match1");
        decideMatch(original, "round1-match4");

        Bracket restored = Bracket.restore(original.describeFirstRound(), resultsOf(original));

        assertEquals(List.of("round1-match3", "round1-match1", "round1-match4"),
                restored.getMatchesInDecisionOrder().stream().map(BracketMatch::getMatchId).toList());
    }

    @Test
    void leavesTheByesOutOfTheDecisionOrderAndDecidesThemAgainOnTheRestore() {
        Bracket original = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie"));

        Bracket restored = Bracket.restore(original.describeFirstRound(), resultsOf(original));

        assertTrue(original.getMatchesInDecisionOrder().isEmpty());
        assertEquals(original.getTeamNamesWithFirstRoundBye(), restored.getTeamNamesWithFirstRoundBye());
        assertEquals(Optional.of("Alpha"), restored.findMatch("round1-match1").orElseThrow().getWinnerTeamName());
        assertEquals(Optional.of("Alpha"), restored.findMatch("round2-match1").orElseThrow().getFirstTeamName());
    }

    @Test
    void rejectsAResultForAnUnknownMatch() {
        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(fourTeamFirstRound(), List.of(result("round3-match1", "Alpha"))));

        assertEquals("a result names unknown match round3-match1", rejection.getMessage());
    }

    @Test
    void rejectsAWinnerThatDoesNotTakePartInTheMatch() {
        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(fourTeamFirstRound(), List.of(result("round1-match1", "Charlie"))));

        assertEquals("Charlie does not take part in match round1-match1", rejection.getMessage());
    }

    @Test
    void rejectsAMatchThatIsDecidedTwice() {
        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(fourTeamFirstRound(), List.of(result("round1-match1", "Alpha"),
                        result("round1-match1", "Bravo"))));

        assertEquals("match round1-match1 is decided twice", rejection.getMessage());
    }

    @Test
    void rejectsATeamThatStandsInTheFirstRoundTwice() {
        List<SavedFirstRoundMatch> firstRound = List.of(slot("round1-match1", "Alpha", "Bravo"),
                slot("round1-match2", "Charlie", "alpha"));

        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(firstRound, List.of()));

        assertEquals("team alpha stands in the first round twice", rejection.getMessage());
    }

    @Test
    void rejectsAResultThatComesBeforeTheResultsItDependsOn() {
        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(fourTeamFirstRound(), List.of(result("round2-match1", "Alpha"),
                        result("round1-match1", "Alpha"), result("round1-match2", "Charlie"))));

        assertEquals("match round2-match1 is decided before both of its teams are known", rejection.getMessage());
    }

    @Test
    void rejectsAResultForAMatchThatIsDecidedByABye() {
        List<SavedFirstRoundMatch> firstRound = List.of(slot("round1-match1", "Alpha", null),
                slot("round1-match2", "Bravo", "Charlie"));

        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(firstRound, List.of(result("round1-match1", "Alpha"))));

        assertEquals("match round1-match1 is decided by a bye and takes no result", rejection.getMessage());
    }

    @Test
    void rejectsAFirstRoundWhoseSizeIsNoPowerOfTwo() {
        List<SavedFirstRoundMatch> firstRound = List.of(slot("round1-match1", "Alpha", "Bravo"),
                slot("round1-match2", "Charlie", "Delta"), slot("round1-match3", "Echo", "Foxtrot"));

        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(firstRound, List.of()));

        assertEquals("the first round holds 3 matches, a bracket needs a power of two", rejection.getMessage());
    }

    @Test
    void rejectsAnUnknownOrRepeatedMatchOfTheFirstRound() {
        IllegalArgumentException unknownMatch = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(List.of(slot("round1-match1", "Alpha", "Bravo"),
                        slot("round1-match7", "Charlie", "Delta")), List.of()));
        IllegalArgumentException repeatedMatch = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(List.of(slot("round1-match1", "Alpha", "Bravo"),
                        slot("round1-match1", "Charlie", "Delta")), List.of()));

        assertEquals("the first round names unknown match round1-match7", unknownMatch.getMessage());
        assertEquals("the first round lists match round1-match1 twice", repeatedMatch.getMessage());
    }

    @Test
    void rejectsAFirstRoundMatchWithoutATeamAndABracketWithASingleTeam() {
        IllegalArgumentException emptyMatch = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(List.of(slot("round1-match1", "Alpha", "Bravo"),
                        slot("round1-match2", null, null)), List.of()));
        IllegalArgumentException singleTeam = assertThrows(IllegalArgumentException.class,
                () -> Bracket.restore(List.of(slot("round1-match1", "Alpha", null)), List.of()));

        assertEquals("match round1-match2 of the first round has no team", emptyMatch.getMessage());
        assertEquals("at least 2 teams are required", singleTeam.getMessage());
    }

    @Test
    void refusesToRestoreATournamentWhoseFinalIsAlreadyDecided() {
        Bracket finished = Bracket.createSingleElimination(List.of("Alpha", "Bravo"));
        finished.recordWinner("round1-match1", "Alpha");
        SavedTournament savedTournament = SavedTournament.capture(finished, TournamentState.RUNNING, Instant.EPOCH,
                Instant.EPOCH, Map.of());

        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                savedTournament::restoreBracket);

        assertEquals("the results already decide the final, nothing is left to continue", rejection.getMessage());
    }

    @Test
    void capturesTheDecisionKindsNextToTheResults() {
        Bracket bracket = Bracket.createSingleElimination(List.of("Alpha", "Bravo", "Charlie", "Delta"));
        decideMatch(bracket, "round1-match1");
        decideMatch(bracket, "round1-match2");

        SavedTournament savedTournament = SavedTournament.capture(bracket, TournamentState.PAUSED, Instant.EPOCH,
                Instant.EPOCH, Map.of("round1-match2", MatchDecisionKind.WALKOVER));

        assertEquals(List.of(Optional.empty(), Optional.of(MatchDecisionKind.WALKOVER)),
                savedTournament.results().stream().map(SavedMatchResult::decidedBy).toList());
        assertFalse(savedTournament.restoreBracket().isComplete());
    }

    private static void decideSomeMatches(Bracket bracket, Random random) {
        while (random.nextInt(4) != 0) {
            List<BracketMatch> playableMatches = bracket.getPlayableMatches();
            if (playableMatches.isEmpty()) {
                return;
            }
            BracketMatch match = playableMatches.get(random.nextInt(playableMatches.size()));
            String winnerTeamName = random.nextBoolean()
                    ? match.getFirstTeamName().orElseThrow()
                    : match.getSecondTeamName().orElseThrow();
            bracket.recordWinner(match.getMatchId(), winnerTeamName);
        }
    }

    private static void assertSameBracket(Bracket original, Bracket restored, String description) {
        assertEquals(original.getRoundCount(), restored.getRoundCount(), description);
        assertEquals(original.getAllMatches().size(), restored.getAllMatches().size(), description);
        for (BracketMatch originalMatch : original.getAllMatches()) {
            BracketMatch restoredMatch = restored.findMatch(originalMatch.getMatchId()).orElseThrow();
            String matchDescription = description + ", " + originalMatch.getMatchId();
            assertEquals(originalMatch.getRoundIndex(), restoredMatch.getRoundIndex(), matchDescription);
            assertEquals(originalMatch.getPositionIndex(), restoredMatch.getPositionIndex(), matchDescription);
            assertEquals(originalMatch.getFirstTeamName(), restoredMatch.getFirstTeamName(), matchDescription);
            assertEquals(originalMatch.getSecondTeamName(), restoredMatch.getSecondTeamName(), matchDescription);
            assertEquals(originalMatch.getWinnerTeamName(), restoredMatch.getWinnerTeamName(), matchDescription);
        }
        assertEquals(matchIds(original.getPlayableMatches()), matchIds(restored.getPlayableMatches()), description);
        assertEquals(matchIds(original.getMatchesInDecisionOrder()), matchIds(restored.getMatchesInDecisionOrder()),
                description);
        assertEquals(original.getTeamNamesWithFirstRoundBye(), restored.getTeamNamesWithFirstRoundBye(), description);
    }

    private static List<String> matchIds(List<BracketMatch> matches) {
        return matches.stream().map(BracketMatch::getMatchId).toList();
    }

    private static List<SavedMatchResult> resultsOf(Bracket bracket) {
        return SavedTournament.capture(bracket, TournamentState.RUNNING, Instant.EPOCH, Instant.EPOCH, Map.of())
                .results();
    }

    private static void decideMatch(Bracket bracket, String matchId) {
        bracket.recordWinner(matchId, bracket.findMatch(matchId).orElseThrow().getFirstTeamName().orElseThrow());
    }

    private static List<String> numberedTeamNames(int teamCount) {
        List<String> teamNames = new ArrayList<>();
        for (int teamNumber = 1; teamNumber <= teamCount; teamNumber++) {
            teamNames.add(String.format(Locale.ROOT, "Team%02d", teamNumber));
        }
        return teamNames;
    }

    private static List<SavedFirstRoundMatch> fourTeamFirstRound() {
        return List.of(slot("round1-match1", "Alpha", "Bravo"), slot("round1-match2", "Charlie", "Delta"));
    }

    private static SavedFirstRoundMatch slot(String matchId, String firstTeamName, String secondTeamName) {
        return new SavedFirstRoundMatch(matchId, Optional.ofNullable(firstTeamName),
                Optional.ofNullable(secondTeamName));
    }

    private static SavedMatchResult result(String matchId, String winnerTeamName) {
        return new SavedMatchResult(matchId, winnerTeamName, Optional.empty());
    }
}
