package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundParticipantsTest {

    private final UUID firstTeamPlayerOne = UUID.randomUUID();
    private final UUID firstTeamPlayerTwo = UUID.randomUUID();
    private final UUID secondTeamPlayerOne = UUID.randomUUID();
    private final UUID secondTeamPlayerTwo = UUID.randomUUID();

    @Test
    void hasNoOutcomeWhileBothTeamsHaveActivePlayers() {
        RoundParticipants participants = createParticipants();

        assertTrue(participants.eliminate(firstTeamPlayerOne));
        assertTrue(participants.determineOutcome().isEmpty());
    }

    @Test
    void declaresTheSurvivingTeamAsRoundWinner() {
        RoundParticipants participants = createParticipants();

        participants.eliminate(firstTeamPlayerOne);
        participants.eliminate(firstTeamPlayerTwo);

        Optional<RoundOutcome> outcome = participants.determineOutcome();
        assertTrue(outcome.isPresent());
        assertFalse(outcome.get().isDraw());
        assertEquals("bravo", outcome.get().winnerTeamName().orElseThrow());
        assertTrue(participants.isTeamDefeated("alpha"));
    }

    @Test
    void declaresADrawWhenBothTeamsAreDefeated() {
        RoundParticipants participants = createParticipants();

        participants.eliminate(firstTeamPlayerOne);
        participants.eliminate(secondTeamPlayerOne);
        participants.eliminate(firstTeamPlayerTwo);
        participants.eliminate(secondTeamPlayerTwo);

        Optional<RoundOutcome> outcome = participants.determineOutcome();
        assertTrue(outcome.isPresent());
        assertTrue(outcome.get().isDraw());
    }

    @Test
    void ignoresPlayersThatAreNotPartOfTheRound() {
        RoundParticipants participants = createParticipants();

        assertFalse(participants.eliminate(UUID.randomUUID()));
        assertTrue(participants.determineOutcome().isEmpty());
    }

    @Test
    void ignoresRepeatedEliminationOfTheSamePlayer() {
        RoundParticipants participants = createParticipants();

        assertTrue(participants.eliminate(firstTeamPlayerOne));
        assertFalse(participants.eliminate(firstTeamPlayerOne));
        assertEquals(1, participants.getActivePlayers("alpha").size());
    }

    @Test
    void reportsTheTeamOfAnActivePlayer() {
        RoundParticipants participants = createParticipants();

        assertEquals("bravo", participants.findTeamNameOfPlayer(secondTeamPlayerOne).orElseThrow());
        participants.eliminate(secondTeamPlayerOne);
        assertTrue(participants.findTeamNameOfPlayer(secondTeamPlayerOne).isEmpty());
    }

    @Test
    void startsWithAllFourPlayersActive() {
        RoundParticipants participants = createParticipants();

        assertEquals(4, participants.getAllActivePlayers().size());
    }

    private RoundParticipants createParticipants() {
        return new RoundParticipants("alpha", List.of(firstTeamPlayerOne, firstTeamPlayerTwo),
                "bravo", List.of(secondTeamPlayerOne, secondTeamPlayerTwo));
    }
}
