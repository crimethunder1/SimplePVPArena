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

    @Test
    void acceptsATeamNameAndTwoPlayerNames() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve Alex");

        assertTrue(request.isAccepted());
        assertEquals("Alpha", request.getTeamName());
        assertEquals(List.of("Steve", "Alex"), request.getPlayerNames());
    }

    @Test
    void acceptsATeamNameWithOneToEightPlayerNames() {
        for (int playerCount = 1; playerCount <= 8; playerCount++) {
            List<String> playerNames = createPlayerNames(playerCount);

            TeamCreationRequest request = TeamCreationRequest.parse("Alpha " + String.join(" ", playerNames));

            assertTrue(request.isAccepted(), playerCount + " players");
            assertEquals("Alpha", request.getTeamName());
            assertEquals(playerNames, request.getPlayerNames());
        }
    }

    @Test
    void ignoresTheSpacesAroundTheValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("  Alpha   Steve  Alex ");

        assertTrue(request.isAccepted());
        assertEquals("Alpha", request.getTeamName());
        assertEquals(List.of("Steve", "Alex"), request.getPlayerNames());
    }

    @Test
    void rejectsALineWithTooFewValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha");

        assertFalse(request.isAccepted());
        assertEquals("Expected 2 to 9 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 1: Alpha", request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsALineWithTooManyValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha " + String.join(" ", createPlayerNames(9)));

        assertFalse(request.isAccepted());
        assertEquals("Expected 2 to 9 values separated by spaces (<teamName> <player> [<player> ...]), "
                + "but got 10: Alpha Player1 Player2 Player3 Player4 Player5 Player6 Player7 Player8 Player9",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnEmptyLine() {
        TeamCreationRequest request = TeamCreationRequest.parse("   ");

        assertFalse(request.isAccepted());
        assertEquals("Nothing was typed, expected <teamName> <player> [<player> ...]",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsTheSamePlayerNamedTwice() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve steve");

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

                TeamCreationRequest request = TeamCreationRequest.parse("Alpha " + String.join(" ", playerNames));

                assertFalse(request.isAccepted(), "positions " + firstPosition + " and " + secondPosition);
                assertEquals("A team needs 1 to 8 different players, Player" + (firstPosition + 1)
                        + " was named twice", request.getRejectionMessage().orElseThrow());
            }
        }
    }

    @Test
    void handsOutNoValuesOfARejectedLine() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha");

        assertThrows(IllegalStateException.class, request::getTeamName);
        assertThrows(IllegalStateException.class, request::getPlayerNames);
    }

    private static List<String> createPlayerNames(int playerCount) {
        List<String> playerNames = new ArrayList<>();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            playerNames.add("Player" + playerNumber);
        }
        return playerNames;
    }
}
