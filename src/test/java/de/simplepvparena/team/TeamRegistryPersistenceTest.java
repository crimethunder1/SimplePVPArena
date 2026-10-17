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
        registry.registerTeam("Alpha", List.of(firstMember, secondMember));
        registry.registerTeam("Bravo", List.of(thirdMember, fourthMember));

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
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("teams", List.of(storedTeam("Team.One",
                List.of(storedMember(firstMember), storedMember(secondMember)))));
        TeamRegistry registry = new TeamRegistry();
        registry.loadFromConfiguration(configuration);

        MemoryConfiguration savedConfiguration = new MemoryConfiguration();
        registry.saveToConfiguration(savedConfiguration);
        TeamRegistry reloadedRegistry = new TeamRegistry();
        reloadedRegistry.loadFromConfiguration(savedConfiguration);

        assertEquals(List.of("Team.One"), reloadedRegistry.getTeamNames());
    }

    @Test
    void startsEmptyWhenNoTeamsAreStored() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", List.of(firstMember, secondMember));

        assertTrue(registry.loadFromConfiguration(new MemoryConfiguration()).isEmpty());
        assertEquals(0, registry.getTeamCount());
    }

    @Test
    void dropsTheStoredTeamsThatAreNoLongerReadable() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        List<Map<String, Object>> storedTeams = new ArrayList<>();
        storedTeams.add(storedTeam("Alpha", List.of(storedMember(firstMember), storedMember(secondMember))));
        storedTeams.add(storedTeam("Broken", List.of()));
        storedTeams.add(storedTeam("Invalid", List.of(storedMember("not-a-uuid", "PlayerFive"),
                storedMember(fourthMember))));
        storedTeams.add(storedTeam(null, List.of(storedMember(thirdMember), storedMember(fourthMember))));
        configuration.set("teams", storedTeams);

        TeamRegistry registry = new TeamRegistry();
        List<String> skippedEntries = registry.loadFromConfiguration(configuration);

        assertEquals(1, registry.getTeamCount());
        assertEquals(List.of("Alpha"), registry.getTeamNames());
        assertEquals(List.of("Team Broken: a team has 1 to 8 players",
                "Team Invalid: member 1 has the invalid unique-id not-a-uuid",
                "Entry 4 of the teams list: the team has no readable name"), skippedEntries);
    }

    @Test
    void dropsAStoredTeamWhoseMembersBelongToAnEarlierTeam() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("teams", List.of(
                storedTeam("Alpha", List.of(storedMember(firstMember), storedMember(secondMember))),
                storedTeam("Bravo", List.of(storedMember(firstMember), storedMember(thirdMember)))));

        TeamRegistry registry = new TeamRegistry();

        assertEquals(List.of("Team Bravo: PlayerOne is already registered in team Alpha"),
                registry.loadFromConfiguration(configuration));
        assertEquals(List.of("Alpha"), registry.getTeamNames());
    }

    @Test
    void namesEveryReasonForASkippedEntryInItsOwnLine() {
        Map<String, Object> memberWithoutUniqueId = new LinkedHashMap<>();
        memberWithoutUniqueId.put("name", "PlayerSix");
        Map<String, Object> memberWithoutName = new LinkedHashMap<>();
        memberWithoutName.put("unique-id", UUID.randomUUID().toString());
        Map<String, Object> teamWithoutMembers = new LinkedHashMap<>();
        teamWithoutMembers.put("name", "NoMembers");
        TeamMember repeatedMember = freshMember("Repeated");
        List<Object> storedTeams = new ArrayList<>();
        storedTeams.add(storedTeam("Alpha", List.of(storedMember(firstMember))));
        storedTeams.add("no team");
        storedTeams.add(storedTeam(null, List.of(storedMember(secondMember))));
        storedTeams.add(teamWithoutMembers);
        storedTeams.add(storedTeamOf("MemberNoMap", List.of("no player")));
        storedTeams.add(storedTeamOf("NoUniqueId", List.of(storedMember(freshMember("Five")), memberWithoutUniqueId)));
        storedTeams.add(storedTeamOf("NoPlayerName", List.of(memberWithoutName)));
        storedTeams.add(storedTeam("BadUniqueId", List.of(storedMember("not-a-uuid", "PlayerSeven"))));
        storedTeams.add(storedTeam("Empty", List.of()));
        storedTeams.add(storedTeam("Nine", storedFreshMembers(9)));
        storedTeams.add(storedTeam("Twice", List.of(storedMember(repeatedMember), storedMember(freshMember("Other")),
                storedMember(repeatedMember))));
        storedTeams.add(storedTeam("ALPHA", List.of(storedMember(thirdMember))));
        storedTeams.add(storedTeam("Bravo", List.of(storedMember(fourthMember), storedMember(firstMember))));
        storedTeams.add(storedTeam("Charlie", List.of(storedMember(thirdMember), storedMember(fourthMember))));
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("teams", storedTeams);
        TeamRegistry registry = new TeamRegistry();

        List<String> skippedEntries = registry.loadFromConfiguration(configuration);

        assertEquals(List.of(
                "Entry 2 of the teams list: it is no team",
                "Entry 3 of the teams list: the team has no readable name",
                "Team NoMembers: the team has no readable members list",
                "Team MemberNoMap: member 1 is no player",
                "Team NoUniqueId: member 2 has no readable unique-id",
                "Team NoPlayerName: member 1 has no readable name",
                "Team BadUniqueId: member 1 has the invalid unique-id not-a-uuid",
                "Team Empty: a team has 1 to 8 players",
                "Team Nine: a team has 1 to 8 players",
                "Team Twice: a team cannot hold the same player twice",
                "Team ALPHA: a team named ALPHA already exists",
                "Team Bravo: PlayerOne is already registered in team Alpha"), skippedEntries);
        assertEquals(List.of("Alpha", "Charlie"), registry.getTeamNames());
    }

    @Test
    void readsNoTeamFromATeamsEntryThatIsNoList() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("teams", "Alpha");
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Bravo", List.of(firstMember));

        assertEquals(List.of("The teams entry is no list, not a single team could be read"),
                registry.loadFromConfiguration(configuration));
        assertEquals(0, registry.getTeamCount());
    }

    @Test
    void loadsTeamsOfOneToEightPlayersWhoseNamesTheMenuWouldRefuse() {
        List<Object> storedTeams = new ArrayList<>();
        for (int memberCount = 1; memberCount <= 8; memberCount++) {
            storedTeams.add(storedTeam("Team " + memberCount + "!", storedFreshMembers(memberCount)));
        }
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set("teams", storedTeams);
        TeamRegistry registry = new TeamRegistry();

        assertTrue(registry.loadFromConfiguration(configuration).isEmpty());

        for (int memberCount = 1; memberCount <= 8; memberCount++) {
            assertEquals(memberCount,
                    registry.findTeam("Team " + memberCount + "!").orElseThrow().getMembers().size());
        }
    }

    private Map<String, Object> storedTeamOf(String teamName, List<Object> storedMembers) {
        Map<String, Object> storedTeam = new LinkedHashMap<>();
        storedTeam.put("name", teamName);
        storedTeam.put("members", storedMembers);
        return storedTeam;
    }

    private List<Map<String, Object>> storedFreshMembers(int memberCount) {
        List<Map<String, Object>> storedMembers = new ArrayList<>();
        for (int memberNumber = 1; memberNumber <= memberCount; memberNumber++) {
            storedMembers.add(storedMember(freshMember("Member" + memberNumber)));
        }
        return storedMembers;
    }

    private static TeamMember freshMember(String playerName) {
        return new TeamMember(UUID.randomUUID(), playerName);
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
