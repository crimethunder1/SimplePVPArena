package de.simplepvparena.team;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamRegistryPersistenceTest {

    private final TeamMember firstMember = new TeamMember(UUID.randomUUID(), "PlayerOne");
    private final TeamMember secondMember = new TeamMember(UUID.randomUUID(), "PlayerTwo");
    private final TeamMember thirdMember = new TeamMember(UUID.randomUUID(), "PlayerThree");
    private final TeamMember fourthMember = new TeamMember(UUID.randomUUID(), "PlayerFour");

    @Test
    void writesAndReadsTeamsWithUniqueIdsAndNames() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", firstMember, secondMember);
        registry.registerTeam("Bravo", thirdMember, fourthMember);

        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);

        TeamRegistry reloadedRegistry = new TeamRegistry();
        assertTrue(reloadedRegistry.loadFromConfiguration(configuration).isEmpty());

        assertEquals(2, reloadedRegistry.getTeamCount());
        assertEquals(List.of("Alpha", "Bravo"), reloadedRegistry.getTeamNames());
        Team reloadedTeam = reloadedRegistry.findTeam("alpha").orElseThrow();
        assertEquals(List.of(firstMember, secondMember), reloadedTeam.getMembers());
        assertEquals("Bravo", reloadedRegistry.findTeamByPlayer(fourthMember.uniqueId()).orElseThrow().getTeamName());
    }

    @Test
    void keepsTeamNamesThatContainADot() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Team.One", firstMember, secondMember);

        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);

        TeamRegistry reloadedRegistry = new TeamRegistry();
        reloadedRegistry.loadFromConfiguration(configuration);

        assertEquals(List.of("Team.One"), reloadedRegistry.getTeamNames());
    }

    @Test
    void startsEmptyWhenNoTeamsAreStored() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", firstMember, secondMember);

        assertTrue(registry.loadFromConfiguration(new MemoryConfiguration()).isEmpty());
        assertEquals(0, registry.getTeamCount());
    }

    @Test
    void dropsTheStoredTeamsThatAreNoLongerReadable() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        List<Map<String, Object>> storedTeams = new ArrayList<>();
        storedTeams.add(storedTeam("Alpha", List.of(storedMember(firstMember), storedMember(secondMember))));
        storedTeams.add(storedTeam("Broken", List.of(storedMember(thirdMember))));
        storedTeams.add(storedTeam("Invalid", List.of(storedMember("not-a-uuid", "PlayerFive"),
                storedMember(fourthMember))));
        storedTeams.add(storedTeam(null, List.of(storedMember(thirdMember), storedMember(fourthMember))));
        configuration.set("teams", storedTeams);

        TeamRegistry registry = new TeamRegistry();
        List<String> skippedTeamNames = registry.loadFromConfiguration(configuration);

        assertEquals(1, registry.getTeamCount());
        assertEquals(List.of("Alpha"), registry.getTeamNames());
        assertEquals(3, skippedTeamNames.size());
        assertTrue(skippedTeamNames.contains("Broken"));
        assertTrue(skippedTeamNames.contains("Invalid"));
    }

    @Test
    void dropsAStoredTeamWhoseMembersBelongToAnEarlierTeam() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("teams", List.of(
                storedTeam("Alpha", List.of(storedMember(firstMember), storedMember(secondMember))),
                storedTeam("Bravo", List.of(storedMember(firstMember), storedMember(thirdMember)))));

        TeamRegistry registry = new TeamRegistry();

        assertEquals(List.of("Bravo"), registry.loadFromConfiguration(configuration));
        assertEquals(List.of("Alpha"), registry.getTeamNames());
    }

    private Map<String, Object> storedTeam(String teamName, List<Map<String, Object>> storedMembers) {
        Map<String, Object> storedTeam = new LinkedHashMap<>();
        if (teamName != null) {
            storedTeam.put("name", teamName);
        }
        storedTeam.put("members", storedMembers);
        return storedTeam;
    }

    private Map<String, Object> storedMember(TeamMember member) {
        return storedMember(member.uniqueId().toString(), member.playerName());
    }

    private Map<String, Object> storedMember(String uniqueIdText, String playerName) {
        Map<String, Object> storedMember = new LinkedHashMap<>();
        storedMember.put("unique-id", uniqueIdText);
        storedMember.put("name", playerName);
        return storedMember;
    }
}
