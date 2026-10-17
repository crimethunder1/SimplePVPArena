package de.simplepvparena.team;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamCreationRequestTest {

    private static final int LARGEST_TEAM_SIZE = Team.MAXIMUM_MEMBER_COUNT;

    @Test
    void acceptsATeamNameAndTwoPlayerNames() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve Alex", LARGEST_TEAM_SIZE);

        assertTrue(request.isAccepted());
        assertEquals("Alpha", request.getTeamName());
        assertEquals(List.of("Steve", "Alex"), request.getPlayerNames());
    }

    @Test
    void acceptsATeamNameWithOneToEightPlayerNames() {
        for (int playerCount = 1; playerCount <= 8; playerCount++) {
            List<String> playerNames = createPlayerNames(playerCount);

            TeamCreationRequest request = TeamCreationRequest.parse("Alpha " + String.join(" ", playerNames),
                    LARGEST_TEAM_SIZE);

            assertTrue(request.isAccepted(), playerCount + " players");
            assertEquals("Alpha", request.getTeamName());
            assertEquals(playerNames, request.getPlayerNames());
        }
    }

    @Test
    void ignoresTheSpacesAroundTheValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("  Alpha   Steve  Alex ", LARGEST_TEAM_SIZE);

        assertTrue(request.isAccepted());
        assertEquals("Alpha", request.getTeamName());
        assertEquals(List.of("Steve", "Alex"), request.getPlayerNames());
    }

    @Test
    void rejectsALineWithTooFewValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha", LARGEST_TEAM_SIZE);

        assertFalse(request.isAccepted());
        assertEquals("Expected 2 to 9 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 1: Alpha", request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsALineWithTooManyValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha " + String.join(" ", createPlayerNames(9)),
                LARGEST_TEAM_SIZE);

        assertFalse(request.isAccepted());
        assertEquals("Expected 2 to 9 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 10: Alpha Player1 Player2 Player3 Player4 Player5 Player6 Player7 Player8 Player9",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnEmptyLine() {
        TeamCreationRequest request = TeamCreationRequest.parse("   ", LARGEST_TEAM_SIZE);

        assertFalse(request.isAccepted());
        assertEquals("Nothing was typed, expected <teamName> <player> [<player> ...]",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsTheSamePlayerNamedTwice() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve steve", LARGEST_TEAM_SIZE);

        assertFalse(request.isAccepted());
        assertEquals("A team needs 1 to 8 different players, Steve was named twice",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsTheSamePlayerNamedTwiceAtEveryPairOfPositions() {
        for (int firstPosition = 0; firstPosition < 8; firstPosition++) {
            for (int secondPosition = firstPosition + 1; secondPosition < 8; secondPosition++) {
                List<String> playerNames = new ArrayList<>(createPlayerNames(8));
                playerNames.set(secondPosition, playerNames.get(firstPosition).toUpperCase(Locale.ROOT));

                TeamCreationRequest request = TeamCreationRequest.parse("Alpha " + String.join(" ", playerNames),
                        LARGEST_TEAM_SIZE);

                assertFalse(request.isAccepted(), "positions " + firstPosition + " and " + secondPosition);
                assertEquals("A team needs 1 to 8 different players, Player" + (firstPosition + 1)
                        + " was named twice", request.getRejectionMessage().orElseThrow());
            }
        }
    }

    @Test
    void handsOutNoValuesOfARejectedLine() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha", LARGEST_TEAM_SIZE);

        assertThrows(IllegalStateException.class, request::getTeamName);
        assertThrows(IllegalStateException.class, request::getPlayerNames);
    }

    @Test
    void acceptsUpToTheTeamSizeAndRejectsOnePlayerMoreNamingTheTeamSize() {
        for (int teamSize = 1; teamSize <= 8; teamSize++) {
            for (int playerCount = 1; playerCount <= teamSize; playerCount++) {
                TeamCreationRequest request = TeamCreationRequest.parse(
                        "Alpha " + String.join(" ", createPlayerNames(playerCount)), teamSize);

                assertTrue(request.isAccepted(), playerCount + " players at a team size of " + teamSize);
            }

            TeamCreationRequest tooLarge = TeamCreationRequest.parse(
                    "Alpha " + String.join(" ", createPlayerNames(teamSize + 1)), teamSize);

            assertFalse(tooLarge.isAccepted(), (teamSize + 1) + " players at a team size of " + teamSize);
            assertTrue(tooLarge.getRejectionMessage().orElseThrow().startsWith("Expected 2"
                    + (teamSize == 1 ? "" : " to " + (teamSize + 1)) + " values separated by spaces"),
                    tooLarge.getRejectionMessage().orElseThrow());
        }
    }

    @Test
    void namesTheValueCountOfTeamsOfTwoAndOfThreePlayers() {
        assertEquals("Expected 2 to 3 values separated by spaces (<teamName> <player> [<player> ...]), "
                        + "but got 4: Alpha Steve Alex Notch",
                TeamCreationRequest.parse("Alpha Steve Alex Notch", 2).getRejectionMessage().orElseThrow());
        assertEquals("Expected 2 to 4 values separated by spaces (<teamName> <player> [<player> ...]), "
                        + "but got 5: Alpha Steve Alex Notch Jeb_",
                TeamCreationRequest.parse("Alpha Steve Alex Notch Jeb_", 3).getRejectionMessage().orElseThrow());
        assertEquals("Expected 2 to 4 values separated by spaces (<teamName> <player> [<player> ...]), "
                        + "but got 1: Alpha",
                TeamCreationRequest.parse("Alpha", 3).getRejectionMessage().orElseThrow());
    }

    @Test
    void namesTheSingleValueCountOfTeamsOfOnePlayer() {
        assertEquals("Expected 2 values separated by spaces (<teamName> <player> [<player> ...]), "
                        + "but got 3: Alpha Steve Alex",
                TeamCreationRequest.parse("Alpha Steve Alex", 1).getRejectionMessage().orElseThrow());

        TeamCreationRequest solo = TeamCreationRequest.parse("Alpha Steve", 1);

        assertTrue(solo.isAccepted());
        assertEquals(List.of("Steve"), solo.getPlayerNames());
    }

    @Test
    void namesTheTeamSizeWhenAPlayerIsNamedTwice() {
        assertEquals("A team needs 1 to 2 different players, Steve was named twice",
                TeamCreationRequest.parse("Alpha Steve steve", 2).getRejectionMessage().orElseThrow());
        assertEquals("A team needs 1 to 3 different players, Alex was named twice",
                TeamCreationRequest.parse("Alpha Alex Steve ALEX", 3).getRejectionMessage().orElseThrow());
    }

    @Test
    void refusesATeamSizeOutsideOfOneToEight() {
        for (int teamSize : new int[] {0, 9}) {
            IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                    () -> TeamCreationRequest.parse("Alpha Steve", teamSize));

            assertEquals("The team size must lie between 1 and 8 players, not " + teamSize, refusal.getMessage());
        }
    }

    private static List<String> createPlayerNames(int playerCount) {
        List<String> playerNames = new ArrayList<>();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            playerNames.add("Player" + playerNumber);
        }
        return playerNames;
    }
}
