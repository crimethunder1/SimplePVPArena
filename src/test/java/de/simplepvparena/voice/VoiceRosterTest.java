package de.simplepvparena.voice;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceRosterTest {

    private static final UUID RED_ONE = UUID.randomUUID();
    private static final UUID RED_TWO = UUID.randomUUID();
    private static final UUID BLUE_ONE = UUID.randomUUID();
    private static final UUID BLUE_TWO = UUID.randomUUID();
    private static final UUID RED_RESERVE = UUID.randomUUID();
    private static final UUID SPECTATOR = UUID.randomUUID();
    private static final UUID OTHER_SPECTATOR = UUID.randomUUID();
    private static final UUID OPERATOR = UUID.randomUUID();

    private static final Map<UUID, String> FIGHTERS = Map.of(RED_ONE, "Red", RED_TWO, "Red", BLUE_ONE, "Blue",
            BLUE_TWO, "Blue");

    @Test
    void withoutAMatchEverybodyHearsEverybody() {
        VoiceRoster roster = VoiceRoster.EMPTY;

        assertTrue(roster.isEmpty());
        assertTrue(roster.canHear(SPECTATOR, RED_ONE));
        assertTrue(roster.canHear(RED_ONE, SPECTATOR));
        assertFalse(roster.isFighter(RED_ONE));
    }

    @Test
    void fightersHearTheirTeamAndTheirOpponents() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(), Set.of());

        assertTrue(roster.canHear(RED_TWO, RED_ONE));
        assertTrue(roster.canHear(BLUE_ONE, RED_ONE));
        assertTrue(roster.canHear(RED_ONE, BLUE_TWO));
    }

    @Test
    void fightersDoNotHearTheSpectatorsButTheSpectatorsHearThem() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(), Set.of());

        assertFalse(roster.canHear(SPECTATOR, RED_ONE));
        assertFalse(roster.canHear(SPECTATOR, BLUE_TWO));
        assertTrue(roster.canHear(RED_ONE, SPECTATOR));
        assertTrue(roster.canHear(OTHER_SPECTATOR, SPECTATOR));
    }

    @Test
    void fightersHearPlayersWithTheBypassPermission() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(), Set.of(OPERATOR));

        assertTrue(roster.canHear(OPERATOR, RED_ONE));
        assertTrue(roster.canHear(OPERATOR, BLUE_ONE));
        assertFalse(roster.canHear(SPECTATOR, RED_ONE));
    }

    @Test
    void aFighterWhoIsOutTalksOnlyWithHisTeamAndTheSpectators() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(RED_ONE), Set.of());

        assertTrue(roster.canHear(RED_ONE, RED_TWO), "the teammate still hears him");
        assertTrue(roster.canHear(RED_TWO, RED_ONE), "he still hears the teammate");
        assertFalse(roster.canHear(RED_ONE, BLUE_ONE), "the opponents no longer hear him");
        assertFalse(roster.canHear(BLUE_ONE, RED_ONE), "he no longer hears the opponents");
        assertTrue(roster.canHear(SPECTATOR, RED_ONE), "he hears the spectators again");
        assertTrue(roster.canHear(RED_ONE, SPECTATOR));
    }

    @Test
    void twoFightersWhoAreOutOfDifferentTeamsDoNotHearEachOther() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(RED_ONE, BLUE_ONE), Set.of());

        assertFalse(roster.canHear(RED_ONE, BLUE_ONE));
        assertFalse(roster.canHear(BLUE_ONE, RED_ONE));
    }

    @Test
    void aFighterWithTheBypassPermissionFollowsTheRulesOfTheFighters() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(RED_ONE), Set.of(RED_ONE));

        assertFalse(roster.canHear(RED_ONE, BLUE_ONE));
    }

    @Test
    void pluginSoundsWithoutASpeakerAreNeverMuted() {
        VoiceRoster roster = new VoiceRoster(FIGHTERS, Set.of(), Set.of());

        assertTrue(roster.canHear(null, RED_ONE));
        assertTrue(roster.canHear(RED_ONE, RED_ONE));
    }

    @Test
    void readsTheFightersOfTheRunningMatchAndLeavesTheReservesOut() {
        Match match = twoAgainstTwoWithAReserve();
        match.setState(MatchState.RUNNING);
        match.beginRound(List.of(RED_ONE, RED_TWO), List.of(BLUE_ONE, BLUE_TWO));

        VoiceRoster roster = VoiceRoster.of(Optional.of(match),
                (ignored, player) -> player.equals(RED_RESERVE), Set.of(OPERATOR));

        assertEquals(FIGHTERS, roster.fighterTeams());
        assertEquals(Set.of(), roster.eliminatedFighters());
        assertEquals(Set.of(OPERATOR), roster.bypassPlayers());
        assertFalse(roster.isFighter(RED_RESERVE));
    }

    @Test
    void readsWhoIsOutOfTheRunningRound() {
        Match match = twoAgainstTwoWithAReserve();
        match.setState(MatchState.RUNNING);
        match.beginRound(List.of(RED_ONE, RED_TWO), List.of(BLUE_ONE, BLUE_TWO));
        match.getRoundParticipants().orElseThrow().eliminate(BLUE_TWO);

        VoiceRoster roster = VoiceRoster.of(Optional.of(match), (ignored, player) -> player.equals(RED_RESERVE),
                Set.of());

        assertEquals(Set.of(BLUE_TWO), roster.eliminatedFighters());
    }

    @Test
    void betweenTheRoundsNobodyIsOutButTheFightersStayIsolated() {
        Match match = twoAgainstTwoWithAReserve();
        match.setState(MatchState.RUNNING);
        match.beginRound(List.of(RED_ONE, RED_TWO), List.of(BLUE_ONE, BLUE_TWO));
        match.getRoundParticipants().orElseThrow().eliminate(BLUE_TWO);
        match.setState(MatchState.BETWEEN_ROUNDS);

        VoiceRoster roster = VoiceRoster.of(Optional.of(match), (ignored, player) -> false, Set.of());

        assertEquals(Set.of(), roster.eliminatedFighters());
        assertTrue(roster.isFighter(BLUE_TWO));
        assertFalse(roster.canHear(SPECTATOR, BLUE_TWO));
    }

    @Test
    void aFinishedOrCancelledMatchIsolatesNobody() {
        for (MatchState state : List.of(MatchState.FINISHED, MatchState.CANCELLED)) {
            Match match = twoAgainstTwoWithAReserve();
            match.setState(state);

            assertSame(VoiceRoster.EMPTY, VoiceRoster.of(Optional.of(match), (ignored, player) -> false, Set.of()),
                    state.name());
        }
        assertSame(VoiceRoster.EMPTY, VoiceRoster.of(Optional.empty(), (ignored, player) -> false, Set.of()));
    }

    private static Match twoAgainstTwoWithAReserve() {
        Team red = new Team("Red", List.of(new TeamMember(RED_ONE, "RedOne"), new TeamMember(RED_TWO, "RedTwo"),
                new TeamMember(RED_RESERVE, "RedReserve")));
        Team blue = new Team("Blue", List.of(new TeamMember(BLUE_ONE, "BlueOne"),
                new TeamMember(BLUE_TWO, "BlueTwo")));
        return Match.regularMatch("M1", red, blue, new Arena());
    }
}
