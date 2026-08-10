package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuddenDeathTickTest {

    private static final long FIXED_SEED = 20250810L;

    private final UUID firstTeamPlayerOne = UUID.randomUUID();
    private final UUID firstTeamPlayerTwo = UUID.randomUUID();
    private final UUID secondTeamPlayerOne = UUID.randomUUID();
    private final UUID secondTeamPlayerTwo = UUID.randomUUID();

    private final SuddenDeathTick suddenDeathTick = new SuddenDeathTick(new Random(FIXED_SEED));

    @Test
    void damagesEveryActiveFighterOnceWithoutEndingTheRound() {
        RoundParticipants participants = createParticipants();
        List<UUID> damagedPlayers = new ArrayList<>();

        Optional<RoundOutcome> outcome = suddenDeathTick.apply(participants, 2.0D, damagedPlayers::add);

        assertTrue(outcome.isEmpty());
        assertEquals(4, damagedPlayers.size());
        assertEquals(participants.getAllActivePlayers(), Set.copyOf(damagedPlayers));
    }

    @Test
    void treatsTwoDeathsInTheSameTickAsADraw() {
        RoundParticipants participants = new RoundParticipants("alpha", List.of(firstTeamPlayerOne),
                "bravo", List.of(secondTeamPlayerOne));
        List<UUID> damagedPlayers = new ArrayList<>();

        Optional<RoundOutcome> outcome = suddenDeathTick.apply(participants, 20.0D,
                playerUniqueId -> killPlayer(participants, damagedPlayers, playerUniqueId));

        assertEquals(2, damagedPlayers.size());
        assertTrue(outcome.isPresent());
        assertTrue(outcome.get().isDraw());
        assertTrue(outcome.get().isSuddenDeathDraw());
    }

    @Test
    void keepsDamagingTheRemainingFightersAfterTheFirstDeath() {
        RoundParticipants participants = createParticipants();
        List<UUID> damagedPlayers = new ArrayList<>();

        Optional<RoundOutcome> outcome = suddenDeathTick.apply(participants, 20.0D,
                playerUniqueId -> killPlayer(participants, damagedPlayers, playerUniqueId));

        assertEquals(4, damagedPlayers.size());
        assertTrue(outcome.isPresent());
        assertTrue(outcome.get().isDraw());
    }

    @Test
    void declaresTheSurvivingTeamAsWinnerWhenOnlyOneTeamFalls() {
        RoundParticipants participants = createParticipants();
        List<UUID> damagedPlayers = new ArrayList<>();

        Optional<RoundOutcome> outcome = suddenDeathTick.apply(participants, 20.0D, playerUniqueId -> {
            damagedPlayers.add(playerUniqueId);
            if ("alpha".equals(participants.findTeamNameOfPlayer(playerUniqueId).orElse(null))) {
                participants.eliminate(playerUniqueId);
            }
        });

        assertEquals(4, damagedPlayers.size());
        assertTrue(outcome.isPresent());
        assertFalse(outcome.get().isDraw());
        assertEquals("bravo", outcome.get().winnerTeamName().orElseThrow());
    }

    @Test
    void doesNotDamageFightersThatAreAlreadyEliminated() {
        RoundParticipants participants = createParticipants();
        participants.eliminate(firstTeamPlayerOne);
        List<UUID> damagedPlayers = new ArrayList<>();

        suddenDeathTick.apply(participants, 2.0D, damagedPlayers::add);

        assertEquals(3, damagedPlayers.size());
        assertFalse(damagedPlayers.contains(firstTeamPlayerOne));
    }

    @Test
    void appliesNoDamageWithoutAPositiveDamageAmount() {
        RoundParticipants participants = createParticipants();
        List<UUID> damagedPlayers = new ArrayList<>();

        assertTrue(suddenDeathTick.apply(participants, 0.0D, damagedPlayers::add).isEmpty());
        assertTrue(suddenDeathTick.apply(participants, -1.0D, damagedPlayers::add).isEmpty());
        assertTrue(damagedPlayers.isEmpty());
    }

    @Test
    void shufflesTheDamageOrderWithTheGivenRandom() {
        RoundParticipants participants = createEightPlayerParticipants();
        List<UUID> registrationOrder = new ArrayList<>(participants.getAllActivePlayers());
        List<UUID> damagedPlayers = new ArrayList<>();

        new SuddenDeathTick(new Random(FIXED_SEED)).apply(participants, 2.0D, damagedPlayers::add);

        List<UUID> expectedOrder = new ArrayList<>(registrationOrder);
        Collections.shuffle(expectedOrder, new Random(FIXED_SEED));
        assertEquals(expectedOrder, damagedPlayers);
        assertNotEquals(registrationOrder, damagedPlayers);
    }

    @Test
    void usesADifferentOrderForFollowingTicks() {
        RoundParticipants participants = createEightPlayerParticipants();
        List<UUID> firstTickOrder = new ArrayList<>();
        List<UUID> secondTickOrder = new ArrayList<>();

        suddenDeathTick.apply(participants, 2.0D, firstTickOrder::add);
        suddenDeathTick.apply(participants, 3.0D, secondTickOrder::add);

        assertNotEquals(firstTickOrder, secondTickOrder);
        assertEquals(Set.copyOf(firstTickOrder), Set.copyOf(secondTickOrder));
    }

    private void killPlayer(RoundParticipants participants, List<UUID> damagedPlayers, UUID playerUniqueId) {
        damagedPlayers.add(playerUniqueId);
        participants.eliminate(playerUniqueId);
    }

    private RoundParticipants createParticipants() {
        return new RoundParticipants("alpha", List.of(firstTeamPlayerOne, firstTeamPlayerTwo),
                "bravo", List.of(secondTeamPlayerOne, secondTeamPlayerTwo));
    }

    private RoundParticipants createEightPlayerParticipants() {
        List<UUID> firstTeamPlayers = new ArrayList<>();
        List<UUID> secondTeamPlayers = new ArrayList<>();
        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            firstTeamPlayers.add(UUID.randomUUID());
            secondTeamPlayers.add(UUID.randomUUID());
        }
        return new RoundParticipants("alpha", firstTeamPlayers, "bravo", secondTeamPlayers);
    }
}
