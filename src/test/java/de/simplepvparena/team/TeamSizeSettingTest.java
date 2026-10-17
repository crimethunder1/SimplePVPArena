package de.simplepvparena.team;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamSizeSettingTest {

    private final TeamRegistry teamRegistry = new TeamRegistry();

    @Test
    void startsWithTeamsOfTwoPlayers() {
        assertEquals(2, TeamSizeSetting.DEFAULT_TEAM_SIZE);
        assertEquals(2, new TeamSizeSetting().getTeamSize());
    }

    @Test
    void takesEveryTeamSizeFromOneToEight() {
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();

        for (int teamSize = 1; teamSize <= 8; teamSize++) {
            teamSizeSetting.setTeamSize(teamSize);

            assertEquals(teamSize, teamSizeSetting.getTeamSize());
            assertTrue(TeamSizeSetting.isAllowed(teamSize), "a team size of " + teamSize);
        }
    }

    @Test
    void refusesATeamSizeBelowOneOrAboveEightAndKeepsTheOldOne() {
        TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
        teamSizeSetting.setTeamSize(3);

        for (int teamSize : new int[] {-1, 0, 9, 100}) {
            IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                    () -> teamSizeSetting.setTeamSize(teamSize));

            assertEquals("The team size must lie between 1 and 8 players, not " + teamSize, refusal.getMessage());
            assertFalse(TeamSizeSetting.isAllowed(teamSize), "a team size of " + teamSize);
            assertEquals(3, teamSizeSetting.getTeamSize());
        }
    }

    @Test
    void describesTheMatchUpAndThePlayerRangeOfATeamSize() {
        assertEquals("1 (1 versus 1)", TeamSizeSetting.describeMatchUp(1));
        assertEquals("2 (2 versus 2)", TeamSizeSetting.describeMatchUp(2));
        assertEquals("8 (8 versus 8)", TeamSizeSetting.describeMatchUp(8));
        assertEquals("1 player", TeamSizeSetting.describePlayerRange(1));
        assertEquals("1 to 2 players", TeamSizeSetting.describePlayerRange(2));
        assertEquals("1 to 8 players", TeamSizeSetting.describePlayerRange(8));
    }

    @Test
    void allowsEveryTeamSizeFromOneToEightWithoutTeamsAndWithoutATournament() {
        for (int teamSize = 1; teamSize <= 8; teamSize++) {
            assertEquals(Optional.empty(), TeamSizeSetting.findChangeRejection(TeamSizeSetting.DEFAULT_TEAM_SIZE, teamSize,
                    false, teamRegistry), "a team size of " + teamSize);
        }
    }

    @Test
    void refusesATeamSizeOutsideOfOneToEight() {
        for (int teamSize : new int[] {0, 9}) {
            assertEquals(Optional.of("The team size must lie between 1 and 8 players"),
                    TeamSizeSetting.findChangeRejection(TeamSizeSetting.DEFAULT_TEAM_SIZE, teamSize, false,
                            teamRegistry), "a team size of " + teamSize);
        }
    }

    @Test
    void refusesEveryChangeWhileATournamentIsInProgress() {
        for (int teamSize = 1; teamSize <= 8; teamSize++) {
            assertEquals(Optional.of("The team size cannot be changed while a tournament is in progress"),
                    TeamSizeSetting.findChangeRejection(TeamSizeSetting.DEFAULT_TEAM_SIZE, teamSize, true,
                            teamRegistry), "a team size of " + teamSize);
        }
    }

    @Test
    void refusesATeamSizeBelowTheSizeOfOneTeamAndNamesItWithItsPlayers() {
        registerTeam("Alpha", 2);
        registerTeam("Bravo", 3);

        assertEquals(Optional.of("1 team has more than 2 players: Bravo (Bravo1, Bravo2, Bravo3). Remove it first"),
                TeamSizeSetting.findChangeRejection(3, 2, false, teamRegistry));
    }

    @Test
    void refusesATeamSizeBelowTheSizeOfSeveralTeamsAndNamesThemAll() {
        registerTeam("Alpha", 3);
        registerTeam("Bravo", 1);
        registerTeam("Charlie", 2);

        assertEquals(Optional.of("2 teams have more than 1 player: Alpha (Alpha1, Alpha2, Alpha3), "
                        + "Charlie (Charlie1, Charlie2). Remove them first"),
                TeamSizeSetting.findChangeRejection(3, 1, false, teamRegistry));
    }

    @Test
    void namesFiveTeamsAndCountsTheOthersWhenMoreThanFiveTeamsAreTooLarge() {
        for (String teamName : List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf")) {
            registerTeam(teamName, 3);
        }

        assertEquals(Optional.of("7 teams have more than 2 players: Alpha (Alpha1, Alpha2, Alpha3), "
                        + "Bravo (Bravo1, Bravo2, Bravo3), Charlie (Charlie1, Charlie2, Charlie3), "
                        + "Delta (Delta1, Delta2, Delta3), Echo (Echo1, Echo2, Echo3) and 2 more. Remove them first"),
                TeamSizeSetting.findChangeRejection(3, 2, false, teamRegistry));
    }

    @Test
    void namesExactlyFiveTeamsWithoutCountingOthers() {
        for (String teamName : List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo")) {
            registerTeam(teamName, 2);
        }

        assertEquals(Optional.of("5 teams have more than 1 player: Alpha (Alpha1, Alpha2), Bravo (Bravo1, Bravo2), "
                        + "Charlie (Charlie1, Charlie2), Delta (Delta1, Delta2), Echo (Echo1, Echo2). "
                        + "Remove them first"),
                TeamSizeSetting.findChangeRejection(2, 1, false, teamRegistry));
    }

    @Test
    void allowsATeamSizeThatEqualsTheLargestTeamAndLeavesSmallerTeamsAsTheyAre() {
        registerTeam("Solo", 1);
        registerTeam("Duo", 2);
        registerTeam("Quartet", 4);

        assertEquals(Optional.empty(), TeamSizeSetting.findChangeRejection(8, 4, false, teamRegistry));
        assertEquals(Optional.empty(), TeamSizeSetting.findChangeRejection(8, 8, false, teamRegistry));

        assertEquals(List.of("Solo", "Duo", "Quartet"), teamRegistry.getTeamNames());
        assertEquals("Solo1", teamRegistry.findTeam("Solo").orElseThrow().getMemberNamesJoined());
        assertEquals("Duo1, Duo2", teamRegistry.findTeam("Duo").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void namesTheRangeBeforeTheTournamentAndTheTournamentBeforeTheTeams() {
        registerTeam("Bravo", 3);

        assertEquals(Optional.of("The team size must lie between 1 and 8 players"),
                TeamSizeSetting.findChangeRejection(3, 0, true, teamRegistry));
        assertEquals(Optional.of("The team size cannot be changed while a tournament is in progress"),
                TeamSizeSetting.findChangeRejection(3, 2, true, teamRegistry));
    }

    @Test
    void allowsALargerTeamSizeEvenWhenATeamHasMorePlayersThanTheNewSize() {
        registerTeam("Alpha", 2);
        registerTeam("Quartet", 4);

        assertEquals(Optional.empty(), TeamSizeSetting.findChangeRejection(2, 3, false, teamRegistry));
    }

    @Test
    void allowsTheSameTeamSizeEvenWhenATeamHasMorePlayers() {
        registerTeam("Quartet", 4);

        assertEquals(Optional.empty(), TeamSizeSetting.findChangeRejection(3, 3, false, teamRegistry));
    }

    @Test
    void refusesASmallerTeamSizeWhenATeamHasMorePlayersThanTheNewSize() {
        registerTeam("Quartet", 4);

        assertEquals(Optional.of("1 team has more than 3 players: Quartet (Quartet1, Quartet2, Quartet3, Quartet4). "
                        + "Remove it first"),
                TeamSizeSetting.findChangeRejection(4, 3, false, teamRegistry));
    }

    @Test
    void describesTheTeamsLargerAndSmallerThanATeamSizeWithTheirPlayerCounts() {
        registerTeam("Alpha", 3);
        registerTeam("Bravo", 1);
        registerTeam("Charlie", 2);
        registerTeam("Delta", 4);

        assertEquals(Optional.of("2 teams have more than 2 players: Alpha (3), Delta (4)"),
                TeamSizeSetting.describeTeamsLargerThan(2, teamRegistry));
        assertEquals(Optional.of("1 team has fewer than 2 players: Bravo (1)"),
                TeamSizeSetting.describeTeamsSmallerThan(2, teamRegistry));
        assertEquals(Optional.empty(), TeamSizeSetting.describeTeamsLargerThan(4, teamRegistry));
        assertEquals(Optional.empty(), TeamSizeSetting.describeTeamsSmallerThan(1, teamRegistry));
    }

    @Test
    void findsTheTeamsLargerAndSmallerThanATeamSizeInTheOrderOfTheRegistry() {
        registerTeam("Alpha", 3);
        registerTeam("Bravo", 1);
        registerTeam("Charlie", 2);
        registerTeam("Delta", 4);

        assertEquals(List.of("Alpha", "Delta"), teamNames(TeamSizeSetting.findTeamsLargerThan(2, teamRegistry)));
        assertEquals(List.of("Bravo"), teamNames(TeamSizeSetting.findTeamsSmallerThan(2, teamRegistry)));
        assertEquals(List.of(), teamNames(TeamSizeSetting.findTeamsLargerThan(4, teamRegistry)));
        assertEquals(List.of("Alpha", "Bravo", "Charlie"),
                teamNames(TeamSizeSetting.findTeamsSmallerThan(4, teamRegistry)));
    }

    private void registerTeam(String teamName, int playerCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), teamName + playerNumber));
        }
        teamRegistry.registerTeam(teamName, members);
    }

    private static List<String> teamNames(List<Team> teams) {
        return teams.stream().map(Team::getTeamName).toList();
    }
}
