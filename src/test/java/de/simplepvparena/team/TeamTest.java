package de.simplepvparena.team;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeamTest {

    @Test
    void holdsOneTwoThreeAndEightPlayersInTheirOrder() {
        for (int memberCount : List.of(1, 2, 3, 8)) {
            List<TeamMember> members = createMembers(memberCount);

            Team team = new Team("Alpha", members);

            assertEquals(members, team.getMembers(), memberCount + " players");
        }
    }

    @Test
    void namesTheSmallestAndTheLargestTeam() {
        assertEquals(1, Team.MINIMUM_MEMBER_COUNT);
        assertEquals(8, Team.MAXIMUM_MEMBER_COUNT);
    }

    @Test
    void rejectsATeamWithoutPlayers() {
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> new Team("Alpha", List.of()));

        assertEquals("a team has 1 to 8 players", refusal.getMessage());
    }

    @Test
    void rejectsATeamOfNinePlayers() {
        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> new Team("Alpha", createMembers(9)));

        assertEquals("a team has 1 to 8 players", refusal.getMessage());
    }

    @Test
    void rejectsTheSamePlayerTwiceInEveryPairOfPositions() {
        for (int firstPosition = 0; firstPosition < Team.MAXIMUM_MEMBER_COUNT; firstPosition++) {
            for (int secondPosition = firstPosition + 1; secondPosition < Team.MAXIMUM_MEMBER_COUNT;
                 secondPosition++) {
                List<TeamMember> members = new ArrayList<>(createMembers(Team.MAXIMUM_MEMBER_COUNT));
                members.set(secondPosition, members.get(firstPosition));

                IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                        () -> new Team("Alpha", members), "positions " + firstPosition + " and " + secondPosition);

                assertEquals("a team cannot hold the same player twice", refusal.getMessage());
            }
        }
    }

    @Test
    void rejectsTheSameUniqueIdUnderAnotherName() {
        UUID uniqueId = UUID.randomUUID();
        List<TeamMember> members = List.of(new TeamMember(uniqueId, "Steve"),
                new TeamMember(UUID.randomUUID(), "Alex"), new TeamMember(uniqueId, "SteveRenamed"));

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> new Team("Alpha", members));

        assertEquals("a team cannot hold the same player twice", refusal.getMessage());
    }

    @Test
    void keepsItsMembersWhenTheGivenListChangesAfterwards() {
        List<TeamMember> members = new ArrayList<>(createMembers(3));
        Team team = new Team("Alpha", members);

        members.clear();

        assertEquals(3, team.getMembers().size());
    }

    private static List<TeamMember> createMembers(int memberCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int memberNumber = 1; memberNumber <= memberCount; memberNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), "Player" + memberNumber));
        }
        return members;
    }
}
