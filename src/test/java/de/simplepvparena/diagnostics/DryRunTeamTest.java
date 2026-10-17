package de.simplepvparena.diagnostics;

import de.simplepvparena.team.TeamMember;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DryRunTeamTest {

    @Test
    void createsTwoTestTeamsOfTwoPlaceholderPlayersEach() {
        List<DryRunTeam> testTeams = DryRunTeam.createTestTeams(2);

        assertEquals(List.of(DryRunTeam.FIRST_TEAM_NAME, DryRunTeam.SECOND_TEAM_NAME),
                testTeams.stream().map(DryRunTeam::teamName).toList());
        assertEquals(List.of("Dry-Run-Alpha-1", "Dry-Run-Alpha-2"), playerNames(testTeams.get(0)));
        assertEquals(List.of("Dry-Run-Bravo-1", "Dry-Run-Bravo-2"), playerNames(testTeams.get(1)));
        Set<UUID> uniqueIds = testTeams.stream()
                .flatMap(testTeam -> testTeam.members().stream())
                .map(TeamMember::uniqueId)
                .collect(Collectors.toSet());
        assertEquals(4, uniqueIds.size());
    }

    @Test
    void createsTwoTestTeamsOfTheGivenNumberOfPlaceholderPlayers() {
        for (int memberCount = 1; memberCount <= 8; memberCount++) {
            List<DryRunTeam> testTeams = DryRunTeam.createTestTeams(memberCount);

            assertEquals(memberCount, testTeams.get(0).members().size());
            assertEquals(memberCount, testTeams.get(1).members().size());
            assertEquals("Dry-Run-Alpha-" + memberCount, playerNames(testTeams.get(0)).get(memberCount - 1));
            assertEquals("Dry-Run-Bravo-1", playerNames(testTeams.get(1)).get(0));
            assertEquals(2 * memberCount, testTeams.stream()
                    .flatMap(testTeam -> testTeam.members().stream())
                    .map(TeamMember::uniqueId)
                    .collect(Collectors.toSet()).size());
        }
    }

    @Test
    void holdsAnyNumberOfMembersAndKeepsThemWhenTheGivenListChanges() {
        List<TeamMember> members = new ArrayList<>(List.of(new TeamMember(UUID.randomUUID(), "Steve"),
                new TeamMember(UUID.randomUUID(), "Alex"), new TeamMember(UUID.randomUUID(), "Notch")));
        DryRunTeam testTeam = new DryRunTeam("Trio", members);

        members.clear();

        assertEquals(List.of("Steve", "Alex", "Notch"), playerNames(testTeam));
    }

    @Test
    void refusesAMissingTeamNameOrMemberList() {
        assertThrows(NullPointerException.class, () -> new DryRunTeam(null, List.of()));
        assertThrows(NullPointerException.class, () -> new DryRunTeam("Trio", null));
    }

    private static List<String> playerNames(DryRunTeam testTeam) {
        return testTeam.members().stream().map(TeamMember::playerName).toList();
    }
}
