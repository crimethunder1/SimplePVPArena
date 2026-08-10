package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchScoreTest {

    @Test
    void startsWithoutPoints() {
        MatchScore score = new MatchScore("alpha", "bravo");

        assertEquals(0, score.getPoints("alpha"));
        assertEquals(0, score.getPoints("bravo"));
        assertFalse(score.isDecided());
        assertTrue(score.getWinnerTeamName().isEmpty());
    }

    @Test
    void isUndecidedAfterOnePointEach() {
        MatchScore score = new MatchScore("alpha", "bravo");

        score.awardPoint("alpha");
        score.awardPoint("bravo");

        assertFalse(score.isDecided());
    }

    @Test
    void isDecidedAfterTwoPointsInBestOfThree() {
        MatchScore score = new MatchScore("alpha", "bravo");

        score.awardPoint("alpha");
        score.awardPoint("alpha");

        assertTrue(score.isDecided());
        assertEquals("alpha", score.getWinnerTeamName().orElseThrow());
        assertEquals(2, score.getPoints("alpha"));
    }

    @Test
    void isDecidedAfterTwoOfThreeRoundsEvenWhenOnePointWasLost() {
        MatchScore score = new MatchScore("alpha", "bravo");

        score.awardPoint("bravo");
        score.awardPoint("alpha");
        score.awardPoint("alpha");

        assertTrue(score.isDecided());
        assertEquals("alpha", score.getWinnerTeamName().orElseThrow());
    }

    @Test
    void rejectsFurtherPointsAfterTheMatchIsDecided() {
        MatchScore score = new MatchScore("alpha", "bravo");
        score.awardPoint("alpha");
        score.awardPoint("alpha");

        assertThrows(IllegalStateException.class, () -> score.awardPoint("bravo"));
    }

    @Test
    void rejectsPointsForUnknownTeams() {
        MatchScore score = new MatchScore("alpha", "bravo");

        assertThrows(IllegalArgumentException.class, () -> score.awardPoint("charlie"));
        assertThrows(IllegalArgumentException.class, () -> score.getPoints("charlie"));
    }

    @Test
    void rejectsAMatchOfATeamAgainstItself() {
        assertThrows(IllegalArgumentException.class, () -> new MatchScore("alpha", "alpha"));
    }
}
