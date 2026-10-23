package de.simplepvparena.team;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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

        Team team = registry.registerTeam("alpha", List.of(firstMember, secondMember));

        assertEquals("alpha", team.getTeamName());
        assertEquals(2, team.getMembers().size());
        assertEquals(1, registry.getTeamCount());
    }

    @Test
    void findsTeamsCaseInsensitively() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", List.of(firstMember, secondMember));

        assertTrue(registry.findTeam("alpha").isPresent());
        assertEquals("Alpha", registry.findTeam("ALPHA").orElseThrow().getTeamName());
    }

    @Test
    void rejectsDuplicateTeamNames() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", List.of(firstMember, secondMember));

        assertThrows(IllegalStateException.class, () -> registry.registerTeam("ALPHA", List.of(thirdMember, fourthMember)));
    }

    @Test
    void rejectsPlayersThatAreAlreadyRegisteredInAnotherTeam() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", List.of(firstMember, secondMember));

        assertThrows(IllegalStateException.class, () -> registry.registerTeam("bravo", List.of(firstMember, thirdMember)));
    }

    @Test
    void rejectsATeamOfTheSamePlayerTwice() {
        assertThrows(IllegalArgumentException.class, () -> new Team("alpha", List.of(firstMember, firstMember)));
    }

    @Test
    void rejectsATeamWithAWrongMemberCount() {
        assertThrows(IllegalArgumentException.class, () -> new Team("alpha", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Team("alpha", createMembers(9)));
    }

    @Test
    void findsTheTeamOfAPlayer() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", List.of(firstMember, secondMember));

        assertEquals("alpha", registry.findTeamByPlayer(secondMember.uniqueId()).orElseThrow().getTeamName());
        assertTrue(registry.findTeamByPlayer(thirdMember.uniqueId()).isEmpty());
    }

    @Test
    void removesTeamsAndFreesTheirPlayers() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("alpha", List.of(firstMember, secondMember));

        assertTrue(registry.removeTeam("ALPHA"));
        assertFalse(registry.removeTeam("alpha"));
        assertEquals(0, registry.getTeamCount());
        registry.registerTeam("bravo", List.of(firstMember, thirdMember));
        assertEquals(1, registry.getTeamCount());
    }

    @Test
    void registersTeamsOfOneTwoThreeAndEightPlayers() {
        TeamRegistry registry = new TeamRegistry();

        for (int memberCount : List.of(1, 2, 3, 8)) {
            List<TeamMember> members = createMembers(memberCount);

            Team team = registry.registerTeam("Team" + memberCount, members);

            assertEquals(members, team.getMembers());
            assertEquals(team, registry.findTeamByPlayer(members.getLast().uniqueId()).orElseThrow());
        }
        assertEquals(List.of("Team1", "Team2", "Team3", "Team8"), registry.getTeamNames());
    }

    @Test
    void rejectsATeamWithoutPlayersAndATeamOfNinePlayersWithoutRegisteringThem() {
        TeamRegistry registry = new TeamRegistry();

        IllegalArgumentException emptyTeamRefusal = assertThrows(IllegalArgumentException.class,
                () -> registry.registerTeam("alpha", List.of()));
        IllegalArgumentException largeTeamRefusal = assertThrows(IllegalArgumentException.class,
                () -> registry.registerTeam("alpha", createMembers(9)));

        assertEquals("a team has 1 to 8 players", emptyTeamRefusal.getMessage());
        assertEquals("a team has 1 to 8 players", largeTeamRefusal.getMessage());
        assertEquals(0, registry.getTeamCount());
    }

    @Test
    void rejectsTheSamePlayerTwiceAtAnyPositionWithoutRegisteringTheTeam() {
        TeamRegistry registry = new TeamRegistry();

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> registry.registerTeam("alpha", List.of(firstMember, secondMember, thirdMember, firstMember)));

        assertEquals("a team cannot hold the same player twice", refusal.getMessage());
        assertEquals(0, registry.getTeamCount());
        assertTrue(registry.findTeamByPlayer(firstMember.uniqueId()).isEmpty());
    }

    @Test
    void rejectsEveryPlayerOfALargeTeamInAnotherTeam() {
        TeamRegistry registry = new TeamRegistry();
        List<TeamMember> members = createMembers(8);
        registry.registerTeam("alpha", members);

        for (TeamMember member : members) {
            IllegalStateException refusal = assertThrows(IllegalStateException.class,
                    () -> registry.registerTeam("bravo", List.of(thirdMember, fourthMember, member)));

            assertEquals(member.playerName() + " is already registered in team alpha", refusal.getMessage());
        }
        assertEquals(List.of("alpha"), registry.getTeamNames());
    }

    @Test
    void namesTheTakenTeamNameAndTheTakenPlayer() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", List.of(firstMember));

        IllegalStateException takenName = assertThrows(IllegalStateException.class,
                () -> registry.registerTeam("ALPHA", List.of(secondMember)));
        IllegalStateException takenPlayer = assertThrows(IllegalStateException.class,
                () -> registry.registerTeam("Bravo", List.of(secondMember, firstMember)));

        assertEquals("a team named ALPHA already exists", takenName.getMessage());
        assertEquals("PlayerOne is already registered in team Alpha", takenPlayer.getMessage());
        assertEquals(List.of("Alpha"), registry.getTeamNames());
    }

    @Test
    void acceptsTeamNamesOfLettersAndDigitsOfEveryScriptWithHyphensAndUnderscores() {
        TeamRegistry registry = new TeamRegistry();
        List<String> teamNames = List.of("A", "Büffel", "Ωmega", "東京", "Team_1-b", "ABCDEFGHIJKLMNOP",
                "\uD835\uDC9C".repeat(16), "٣٤٥");

        for (String teamName : teamNames) {
            registry.registerTeam(teamName, createMembers(1));
        }

        assertEquals(teamNames, registry.getTeamNames());
    }

    @Test
    void rejectsTeamNamesOutsideTheNameRuleWithoutRegisteringThem() {
        TeamRegistry registry = new TeamRegistry();

        for (String teamName : List.of("Team.One", "Team One", "Alpha!", "ABCDEFGHIJKLMNOPQ", "Team/1", "Ä+")) {
            IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                    () -> registry.registerTeam(teamName, createMembers(2)), teamName);

            assertEquals("The team name " + teamName + " is not allowed, a team name has 1 to 16 letters, digits, "
                    + "hyphens or underscores", refusal.getMessage());
        }
        assertEquals(0, registry.getTeamCount());
    }

    @Test
    void rejectsAnEmptyTeamName() {
        TeamRegistry registry = new TeamRegistry();

        for (String teamName : List.of("", "   ")) {
            IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                    () -> registry.registerTeam(teamName, createMembers(2)));

            assertEquals("A team needs a name, a team name has 1 to 16 letters, digits, hyphens or underscores",
                    refusal.getMessage());
        }
        assertEquals(0, registry.getTeamCount());
    }

    @Test
    void comparesTeamNamesWithUmlautsWithoutCase() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Büffel", List.of(firstMember));

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> registry.registerTeam("BÜFFEL", List.of(secondMember)));

        assertEquals("a team named BÜFFEL already exists", refusal.getMessage());
        assertEquals("Büffel", registry.findTeam("büffel").orElseThrow().getTeamName());
    }

    @Test
    void registersTeamsLargerThanTheTeamSizeBecauseOnlyTheTeamMenuChecksIt() {
        TeamRegistry registry = new TeamRegistry();
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        teamSizeSetting.setTeamSize(1);

        registry.registerTeam("Trio", createMembers(3));
        registry.registerTeam("Squad", createMembers(8));

        assertEquals(1, teamSizeSetting.getTeamSize());
        assertEquals(3, registry.findTeam("Trio").orElseThrow().getMembers().size());
        assertEquals(8, registry.findTeam("Squad").orElseThrow().getMembers().size());
    }

    @Test
    void loadsEveryStoredTeamUpToEightPlayersWhateverTheTeamSizeIs() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Solo", createMembers(1));
        registry.registerTeam("Quartet", createMembers(4));
        registry.registerTeam("Squad", createMembers(8));
        MemoryConfiguration configuration = new MemoryConfiguration();
        registry.saveToConfiguration(configuration);
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();

        TeamRegistry reloadedRegistry = new TeamRegistry();
        List<String> skippedEntries = reloadedRegistry.loadFromConfiguration(configuration);

        assertEquals(List.of(), skippedEntries);
        assertEquals(2, teamSizeSetting.getTeamSize());
        assertEquals(List.of("Solo", "Quartet", "Squad"), reloadedRegistry.getTeamNames());
        assertEquals(8, reloadedRegistry.findTeam("Squad").orElseThrow().getMembers().size());
    }

    @Test
    void renamesATeamAndKeepsItsPlaceAndItsPlayers() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();
        Team formerTeam = registry.findTeam("Bravo").orElseThrow();

        Team renamedTeam = registry.renameTeam("bravo", "Delta");

        assertEquals("Delta", renamedTeam.getTeamName());
        assertEquals(List.of("Alpha", "Delta", "Charlie"), registry.getTeamNames());
        assertTrue(registry.findTeam("Bravo").isEmpty());
        assertSame(renamedTeam, registry.findTeam("DELTA").orElseThrow());
        assertEquals(formerTeam.getMembers(), renamedTeam.getMembers());
        assertEquals("Delta", registry.findTeamByPlayer(thirdMember.uniqueId()).orElseThrow().getTeamName());
        assertEquals("Bravo", formerTeam.getTeamName(), "the former team object stays as it was");
    }

    @Test
    void renamesTheFirstAndTheLastTeamWithoutChangingTheOrder() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        registry.renameTeam("Alpha", "Zulu");
        registry.renameTeam("Charlie", "Able");

        assertEquals(List.of("Zulu", "Bravo", "Able"), registry.getTeamNames());
        assertEquals(3, registry.getTeamCount());
    }

    @Test
    void renamesATeamToAnotherCaseOfItsOwnName() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        Team renamedTeam = registry.renameTeam("Bravo", "BRAVO");

        assertEquals("BRAVO", renamedTeam.getTeamName());
        assertEquals(List.of("Alpha", "BRAVO", "Charlie"), registry.getTeamNames());
        assertSame(renamedTeam, registry.findTeam("bravo").orElseThrow());
    }

    @Test
    void rejectsTheNameOfAnotherTeamInAnyCaseAndKeepsTheTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();
        Team bravo = registry.findTeam("Bravo").orElseThrow();

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> registry.renameTeam("Bravo", "ALPHA"));

        assertEquals("a team named ALPHA already exists", refusal.getMessage());
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), registry.getTeamNames());
        assertSame(bravo, registry.findTeam("Bravo").orElseThrow());
    }

    @Test
    void rejectsANewNameOutsideTheNameRuleAndKeepsTheTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalArgumentException invalidName = assertThrows(IllegalArgumentException.class,
                () -> registry.renameTeam("Bravo", "Bravo Team"));
        IllegalArgumentException blankName = assertThrows(IllegalArgumentException.class,
                () -> registry.renameTeam("Bravo", " "));
        IllegalArgumentException longName = assertThrows(IllegalArgumentException.class,
                () -> registry.renameTeam("Bravo", "A".repeat(17)));

        assertEquals("The team name Bravo Team is not allowed, a team name has 1 to 16 letters, digits, hyphens "
                + "or underscores", invalidName.getMessage());
        assertEquals("A team needs a name, a team name has 1 to 16 letters, digits, hyphens or underscores",
                blankName.getMessage());
        assertEquals("The team name " + "A".repeat(17) + " is not allowed, a team name has 1 to 16 letters, "
                + "digits, hyphens or underscores", longName.getMessage());
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), registry.getTeamNames());
    }

    @Test
    void tellsTheRenameObserversTheStoredFormerNameAndTheNewName() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();
        List<String> renames = new ArrayList<>();
        registry.addRenameObserver((formerName, newName) -> renames.add(formerName + " -> " + newName));

        registry.renameTeam("bravo", "Delta");
        assertThrows(IllegalStateException.class, () -> registry.renameTeam("Alpha", "charlie"));

        assertEquals(List.of("Bravo -> Delta"), renames);
    }

    @Test
    void rejectsTheRenameOfAnUnknownTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> registry.renameTeam("Delta", "Echo"));

        assertEquals("no team named Delta is registered", refusal.getMessage());
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), registry.getTeamNames());
    }

    @Test
    void addsAPlayerAtTheEndOfTheTeamAndKeepsThePlaceOfTheTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();
        TeamMember newMember = new TeamMember(UUID.randomUUID(), "Newcomer");
        Team formerTeam = registry.findTeam("Bravo").orElseThrow();

        Team changedTeam = registry.addMember("bravo", newMember);

        assertEquals("PlayerThree, Newcomer", changedTeam.getMemberNamesJoined());
        assertSame(changedTeam, registry.findTeam("Bravo").orElseThrow());
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), registry.getTeamNames());
        assertEquals("Bravo", registry.findTeamByPlayer(newMember.uniqueId()).orElseThrow().getTeamName());
        assertEquals("PlayerThree", formerTeam.getMemberNamesJoined(), "the former team object stays as it was");
    }

    @Test
    void rejectsAPlayerOfAnotherTeamAndNamesThatTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> registry.addMember("Bravo", firstMember));

        assertEquals("PlayerOne is already registered in team Alpha", refusal.getMessage());
        assertEquals("PlayerThree", registry.findTeam("Bravo").orElseThrow().getMemberNamesJoined());
        assertEquals("PlayerOne, PlayerTwo", registry.findTeam("Alpha").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void rejectsAPlayerThatIsAlreadyInTheSameTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> registry.addMember("Bravo", thirdMember));

        assertEquals("PlayerThree is already registered in team Bravo", refusal.getMessage());
        assertEquals("PlayerThree", registry.findTeam("Bravo").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void rejectsANinthPlayer() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Octet", createMembers(8));

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> registry.addMember("Octet", new TeamMember(UUID.randomUUID(), "Ninth")));

        assertEquals("team Octet already has 8 players, a team has at most 8 players", refusal.getMessage());
        assertEquals(8, registry.findTeam("Octet").orElseThrow().getMembers().size());
    }

    @Test
    void addsAnEighthPlayer() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Septet", createMembers(7));

        Team changedTeam = registry.addMember("Septet", new TeamMember(UUID.randomUUID(), "Eighth"));

        assertEquals(8, changedTeam.getMembers().size());
    }

    @Test
    void rejectsAPlayerForAnUnknownTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> registry.addMember("Delta", new TeamMember(UUID.randomUUID(), "Newcomer")));

        assertEquals("no team named Delta is registered", refusal.getMessage());
        assertEquals(3, registry.getTeamCount());
    }

    @Test
    void removesAPlayerKeepsThePlaceOfTheTeamAndFreesThePlayerForAnotherTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();
        Team formerTeam = registry.findTeam("Alpha").orElseThrow();

        Team changedTeam = registry.removeMember("ALPHA", firstMember.uniqueId());

        assertEquals("PlayerTwo", changedTeam.getMemberNamesJoined());
        assertSame(changedTeam, registry.findTeam("Alpha").orElseThrow());
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), registry.getTeamNames());
        assertTrue(registry.findTeamByPlayer(firstMember.uniqueId()).isEmpty());
        assertEquals("PlayerOne, PlayerTwo", formerTeam.getMemberNamesJoined(),
                "the former team object stays as it was");

        registry.addMember("Bravo", firstMember);

        assertEquals("Bravo", registry.findTeamByPlayer(firstMember.uniqueId()).orElseThrow().getTeamName());
    }

    @Test
    void rejectsTheRemovalOfTheLastPlayer() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> registry.removeMember("Bravo", thirdMember.uniqueId()));

        assertEquals("a team keeps at least 1 player, remove the team instead", refusal.getMessage());
        assertEquals("PlayerThree", registry.findTeam("Bravo").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void rejectsTheRemovalOfAPlayerOutsideOfTheTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> registry.removeMember("Alpha", thirdMember.uniqueId()));

        assertEquals("the player " + thirdMember.uniqueId() + " is not in team Alpha", refusal.getMessage());
        assertEquals("PlayerOne, PlayerTwo", registry.findTeam("Alpha").orElseThrow().getMemberNamesJoined());
        assertEquals("PlayerThree", registry.findTeam("Bravo").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void rejectsTheRemovalOfAPlayerFromAnUnknownTeam() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> registry.removeMember("Delta", firstMember.uniqueId()));

        assertEquals("no team named Delta is registered", refusal.getMessage());
    }

    @Test
    void savesTheChangedTeamsInTheOrderOfTheRegistry() {
        TeamRegistry registry = registryOfAlphaBravoAndCharlie();
        TeamMember newMember = new TeamMember(UUID.randomUUID(), "Newcomer");
        registry.renameTeam("Bravo", "Delta");
        registry.addMember("Delta", newMember);
        registry.removeMember("Alpha", secondMember.uniqueId());
        MemoryConfiguration configuration = new MemoryConfiguration();

        registry.saveToConfiguration(configuration);
        TeamRegistry loadedRegistry = new TeamRegistry();
        List<String> skippedEntries = loadedRegistry.loadFromConfiguration(configuration);

        assertEquals(List.of(), skippedEntries);
        assertEquals(List.of("Alpha", "Delta", "Charlie"), loadedRegistry.getTeamNames());
        assertEquals("PlayerOne", loadedRegistry.findTeam("Alpha").orElseThrow().getMemberNamesJoined());
        assertEquals("PlayerThree, Newcomer", loadedRegistry.findTeam("Delta").orElseThrow().getMemberNamesJoined());
    }

    private TeamRegistry registryOfAlphaBravoAndCharlie() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", List.of(firstMember, secondMember));
        registry.registerTeam("Bravo", List.of(thirdMember));
        registry.registerTeam("Charlie", List.of(fourthMember));
        return registry;
    }

    private static List<TeamMember> createMembers(int memberCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int memberNumber = 1; memberNumber <= memberCount; memberNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), "Member" + memberNumber));
        }
        return members;
    }
}
