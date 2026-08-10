package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchServicePvpLockTest {

    private static final UUID FIRST_FIGHTER = UUID.randomUUID();
    private static final UUID SECOND_FIGHTER = UUID.randomUUID();
    private static final UUID FIRST_OPPONENT = UUID.randomUUID();
    private static final UUID SECOND_OPPONENT = UUID.randomUUID();
    private static final UUID SPECTATOR = UUID.randomUUID();
    private static final UUID OTHER_SPECTATOR = UUID.randomUUID();

    private final RoundParticipants roundParticipants = new RoundParticipants(
            "Alpha", List.of(FIRST_FIGHTER, SECOND_FIGHTER),
            "Beta", List.of(FIRST_OPPONENT, SECOND_OPPONENT));

    @Test
    void blocksDamageBetweenTwoPlayersThatDoNotFight() {
        assertFalse(MatchService.isPvpAllowedInRound(roundParticipants, SPECTATOR, OTHER_SPECTATOR));
    }

    @Test
    void allowsDamageBetweenTwoOpposingFightersOfTheRunningRound() {
        assertTrue(MatchService.isPvpAllowedInRound(roundParticipants, FIRST_FIGHTER, FIRST_OPPONENT));
        assertTrue(MatchService.isPvpAllowedInRound(roundParticipants, SECOND_OPPONENT, SECOND_FIGHTER));
    }

    @Test
    void blocksDamageBetweenAFighterAndAPlayerThatDoesNotFight() {
        assertFalse(MatchService.isPvpAllowedInRound(roundParticipants, FIRST_FIGHTER, SPECTATOR));
        assertFalse(MatchService.isPvpAllowedInRound(roundParticipants, SPECTATOR, FIRST_FIGHTER));
    }

    @Test
    void leavesDamageBetweenTwoTeamMatesToTheFriendlyFireCheck() {
        assertTrue(MatchService.isPvpAllowedInRound(roundParticipants, FIRST_FIGHTER, SECOND_FIGHTER));
        assertTrue(roundParticipants.isSameActiveTeam(FIRST_FIGHTER, SECOND_FIGHTER));
    }

    @Test
    void blocksDamageAgainstAFighterThatIsAlreadyEliminated() {
        roundParticipants.eliminate(SECOND_OPPONENT);

        assertFalse(MatchService.isPvpAllowedInRound(roundParticipants, FIRST_FIGHTER, SECOND_OPPONENT));
        assertFalse(MatchService.isPvpAllowedInRound(roundParticipants, SECOND_OPPONENT, FIRST_FIGHTER));
    }

    @Test
    void blocksDamageBetweenPlayersWhileNoRoundIsRunning() {
        assertFalse(MatchService.isPvpAllowedInRound(null, FIRST_FIGHTER, FIRST_OPPONENT));
        assertFalse(MatchService.isPvpAllowedInRound(null, SPECTATOR, OTHER_SPECTATOR));
    }

    @Test
    void neverBlocksSelfInflictedDamage() {
        assertTrue(MatchService.isPvpAllowedInRound(roundParticipants, FIRST_FIGHTER, FIRST_FIGHTER));
        assertTrue(MatchService.isPvpAllowedInRound(roundParticipants, SPECTATOR, SPECTATOR));
        assertTrue(MatchService.isPvpAllowedInRound(null, SPECTATOR, SPECTATOR));
    }
}
