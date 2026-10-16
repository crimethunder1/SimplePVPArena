package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketSeedingTest {

    private static final int SMALLEST_TEAM_COUNT = 2;
    private static final int LARGEST_TEAM_COUNT = 32;

    @Test
    void placesEveryTeamInExactlyOneFirstRoundMatch() {
        for (int teamCount = SMALLEST_TEAM_COUNT; teamCount <= LARGEST_TEAM_COUNT; teamCount++) {
            List<String> teamNames = createTeamNames(teamCount);
            List<BracketMatch> firstRound = createFirstRound(teamNames);

            List<String> placedTeamNames = new ArrayList<>();
            int emptySlotCount = 0;
            for (BracketMatch match : firstRound) {
                assertTrue(match.hasBothParticipants() || match.hasExactlyOneParticipant(),
                        teamCount + " teams: " + match.getMatchId() + " has no participant");
                Optional<String> firstTeamName = match.getFirstTeamName();
                Optional<String> secondTeamName = match.getSecondTeamName();
                firstTeamName.ifPresent(placedTeamNames::add);
                secondTeamName.ifPresent(placedTeamNames::add);
                emptySlotCount += (firstTeamName.isEmpty() ? 1 : 0) + (secondTeamName.isEmpty() ? 1 : 0);
            }

            assertEquals(teamCount, placedTeamNames.size(), teamCount + " teams: " + placedTeamNames);
            assertEquals(Set.copyOf(teamNames), new HashSet<>(placedTeamNames), teamCount + " teams");
            assertEquals(countByes(firstRound), emptySlotCount,
                    teamCount + " teams: only the opponent slot of a bye may stay empty");
        }
    }

    @Test
    void createsAsManyByesAsTheBracketHasSpareSlots() {
        for (int teamCount = SMALLEST_TEAM_COUNT; teamCount <= LARGEST_TEAM_COUNT; teamCount++) {
            List<String> teamNames = createTeamNames(teamCount);
            Bracket bracket = Bracket.createSingleElimination(teamNames);
            List<BracketMatch> firstRound = bracket.getRounds().get(0);
            int bracketSize = firstRound.size() * 2;

            assertEquals(bracketSize - teamCount, countByes(firstRound), teamCount + " teams");
            assertEquals(bracketSize - teamCount, bracket.getTeamNamesWithFirstRoundBye().size(),
                    teamCount + " teams");
        }
    }

    @Test
    void spreadsTheByesEvenlyOverBothHalvesOfTheFirstRound() {
        for (int teamCount = SMALLEST_TEAM_COUNT; teamCount <= LARGEST_TEAM_COUNT; teamCount++) {
            List<BracketMatch> firstRound = createFirstRound(createTeamNames(teamCount));
            int halfSize = firstRound.size() / 2;

            int upperHalfByeCount = countByes(firstRound.subList(0, halfSize));
            int lowerHalfByeCount = countByes(firstRound.subList(halfSize, firstRound.size()));

            assertTrue(Math.abs(upperHalfByeCount - lowerHalfByeCount) <= 1,
                    teamCount + " teams: " + upperHalfByeCount + " byes in the upper half, "
                            + lowerHalfByeCount + " in the lower half");
        }
    }

    @Test
    void pairsTwoByesInTheSecondRoundOnlyWhenMoreThanHalfOfTheFirstRoundAreByes() {
        for (int teamCount = SMALLEST_TEAM_COUNT; teamCount <= LARGEST_TEAM_COUNT; teamCount++) {
            List<BracketMatch> firstRound = createFirstRound(createTeamNames(teamCount));
            if (firstRound.size() == 1) {
                continue;
            }
            int byeCount = countByes(firstRound);

            int secondRoundMatchesBetweenByes = 0;
            for (int matchIndex = 0; matchIndex < firstRound.size(); matchIndex += 2) {
                if (firstRound.get(matchIndex).hasExactlyOneParticipant()
                        && firstRound.get(matchIndex + 1).hasExactlyOneParticipant()) {
                    secondRoundMatchesBetweenByes++;
                }
            }

            assertEquals(Math.max(0, byeCount - firstRound.size() / 2), secondRoundMatchesBetweenByes,
                    teamCount + " teams with " + byeCount + " byes");
        }
    }

    @Test
    void buildsTheStandardSeedingOrder() {
        assertEquals(List.of(1, 2), Bracket.buildSeedingOrder(2));
        assertEquals(List.of(1, 4, 2, 3), Bracket.buildSeedingOrder(4));
        assertEquals(List.of(1, 8, 4, 5, 2, 7, 3, 6), Bracket.buildSeedingOrder(8));
        assertEquals(List.of(1, 16, 8, 9, 4, 13, 5, 12, 2, 15, 7, 10, 3, 14, 6, 11), Bracket.buildSeedingOrder(16));
    }

    @Test
    void placesTheFirstTwoSeedsInDifferentHalves() {
        for (int teamCount = SMALLEST_TEAM_COUNT; teamCount <= LARGEST_TEAM_COUNT; teamCount++) {
            List<String> teamNames = createTeamNames(teamCount);
            List<BracketMatch> firstRound = createFirstRound(teamNames);
            if (firstRound.size() == 1) {
                assertTrue(firstRound.get(0).containsTeam(teamNames.get(0))
                        && firstRound.get(0).containsTeam(teamNames.get(1)), teamCount + " teams");
                continue;
            }
            int halfSize = firstRound.size() / 2;

            int firstSeedHalf = findPositionIndex(firstRound, teamNames.get(0)) / halfSize;
            int secondSeedHalf = findPositionIndex(firstRound, teamNames.get(1)) / halfSize;

            assertNotEquals(firstSeedHalf, secondSeedHalf, teamCount + " teams");
        }
    }

    @Test
    void letsTheFirstTwoSeedsMeetOnlyInTheFinal() {
        for (int teamCount = SMALLEST_TEAM_COUNT; teamCount <= LARGEST_TEAM_COUNT; teamCount++) {
            List<String> teamNames = createTeamNames(teamCount);
            Bracket bracket = Bracket.createSingleElimination(teamNames);
            String firstSeed = teamNames.get(0);
            String secondSeed = teamNames.get(1);

            while (!bracket.getFinalMatch().isPlayable()) {
                for (BracketMatch match : bracket.getPlayableMatches()) {
                    assertFalse(match.containsTeam(firstSeed) && match.containsTeam(secondSeed),
                            teamCount + " teams: the first two seeds meet in " + match.getMatchId());
                    bracket.recordWinner(match.getMatchId(), findBetterSeed(match, teamNames));
                }
            }

            BracketMatch finalMatch = bracket.getFinalMatch();
            assertTrue(finalMatch.containsTeam(firstSeed) && finalMatch.containsTeam(secondSeed),
                    teamCount + " teams: " + finalMatch);
        }
    }

    private static List<String> createTeamNames(int teamCount) {
        List<String> teamNames = new ArrayList<>(teamCount);
        for (int teamIndex = 0; teamIndex < teamCount; teamIndex++) {
            teamNames.add("team" + (teamIndex + 1));
        }
        return teamNames;
    }

    private static List<BracketMatch> createFirstRound(List<String> teamNames) {
        return Bracket.createSingleElimination(teamNames).getRounds().get(0);
    }

    private static int countByes(List<BracketMatch> matches) {
        return (int) matches.stream().filter(BracketMatch::hasExactlyOneParticipant).count();
    }

    private static int findPositionIndex(List<BracketMatch> firstRound, String teamName) {
        return firstRound.stream()
                .filter(match -> match.containsTeam(teamName))
                .findFirst()
                .orElseThrow()
                .getPositionIndex();
    }

    private static String findBetterSeed(BracketMatch match, List<String> teamNames) {
        String firstTeamName = match.getFirstTeamName().orElseThrow();
        String secondTeamName = match.getSecondTeamName().orElseThrow();
        return teamNames.indexOf(firstTeamName) < teamNames.indexOf(secondTeamName) ? firstTeamName : secondTeamName;
    }
}
