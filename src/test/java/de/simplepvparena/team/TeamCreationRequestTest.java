package de.simplepvparena.team;

import org.junit.jupiter.api.Test;

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
        assertEquals("Steve", request.getFirstPlayerName());
        assertEquals("Alex", request.getSecondPlayerName());
    }

    @Test
    void ignoresTheSpacesAroundTheValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("  Alpha   Steve  Alex ");

        assertTrue(request.isAccepted());
        assertEquals("Alpha", request.getTeamName());
        assertEquals("Alex", request.getSecondPlayerName());
    }

    @Test
    void rejectsALineWithTooFewValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve");

        assertFalse(request.isAccepted());
        assertEquals("Expected 3 values separated by spaces (<teamName> <firstPlayer> <secondPlayer>), "
                + "but got 2: Alpha Steve", request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsALineWithTooManyValues() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve Alex Notch");

        assertFalse(request.isAccepted());
        assertEquals("Expected 3 values separated by spaces (<teamName> <firstPlayer> <secondPlayer>), "
                + "but got 4: Alpha Steve Alex Notch", request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnEmptyLine() {
        TeamCreationRequest request = TeamCreationRequest.parse("   ");

        assertFalse(request.isAccepted());
        assertEquals("Nothing was typed, expected <teamName> <firstPlayer> <secondPlayer>",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsTheSamePlayerNamedTwice() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha Steve steve");

        assertFalse(request.isAccepted());
        assertEquals("A team needs two different players, Steve was named twice",
                request.getRejectionMessage().orElseThrow());
    }

    @Test
    void handsOutNoValuesOfARejectedLine() {
        TeamCreationRequest request = TeamCreationRequest.parse("Alpha");

        assertThrows(IllegalStateException.class, request::getTeamName);
        assertThrows(IllegalStateException.class, request::getFirstPlayerName);
        assertThrows(IllegalStateException.class, request::getSecondPlayerName);
    }
}
