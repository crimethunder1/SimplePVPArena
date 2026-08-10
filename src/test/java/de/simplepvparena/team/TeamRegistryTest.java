package de.simplepvparena.team;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRegistryTest {

    private final TeamMember firstMember = new TeamMember(UUID.randomUUID(), "PlayerOne");
    private final TeamMember secondMember = new TeamMember(UUID.randomUUID(), "PlayerTwo");
    private final TeamMember thirdMember = new TeamMember(UUID.randomUUID(), "PlayerThree");
    private final TeamMember fourthMember = new TeamMember(UUID.randomUUID(), "PlayerFour");

    @Test
    void registersATeamOfTwoPlayers() {
        TeamRegistry registry = new TeamRegistry();

        Team team = registry.registerTeam("alpha", firstMember, secondMember);

        assertEquals("alpha", team.getTeamName());
        assertEquals(2, team.getMembers().size());
        assertEquals(1, registry.getTeamCount());
    }

    @Test
    void findsTeamsCaseInsensitively() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", firstMember, secondMember);

        assertTrue(registry.findTeam("alpha").isPresent());
        assertEquals("Alpha", registry.findTeam("ALPHA").orElseThrow().getTeamName());
    }

    @Test
    void rejectsDuplicateTeamNames() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", firstMember, secondMember);

        assertThrows(IllegalStateException.class, () -> registry.registerTeam("ALPHA", thirdMember, fourthMember));
    }

    @Test
    void rejectsPlayersThatAreAlreadyRegisteredInAnotherTeam() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", firstMember, secondMember);

        assertThrows(IllegalStateException.class, () -> registry.registerTeam("bravo", firstMember, thirdMember));
    }

    @Test
    void rejectsATeamOfTheSamePlayerTwice() {
        assertThrows(IllegalArgumentException.class, () -> new Team("alpha", List.of(firstMember, firstMember)));
    }

    @Test
    void rejectsATeamWithAWrongMemberCount() {
        assertThrows(IllegalArgumentException.class, () -> new Team("alpha", List.of(firstMember)));
        assertThrows(IllegalArgumentException.class,
                () -> new Team("alpha", List.of(firstMember, secondMember, thirdMember)));
    }

    @Test
    void findsTheTeamOfAPlayer() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", firstMember, secondMember);

        assertEquals("alpha", registry.findTeamByPlayer(secondMember.uniqueId()).orElseThrow().getTeamName());
        assertTrue(registry.findTeamByPlayer(thirdMember.uniqueId()).isEmpty());
    }

    @Test
    void removesTeamsAndFreesTheirPlayers() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", firstMember, secondMember);

        assertTrue(registry.removeTeam("ALPHA"));
        assertFalse(registry.removeTeam("alpha"));
        assertEquals(0, registry.getTeamCount());
        registry.registerTeam("bravo", firstMember, thirdMember);
        assertEquals(1, registry.getTeamCount());
    }
}
