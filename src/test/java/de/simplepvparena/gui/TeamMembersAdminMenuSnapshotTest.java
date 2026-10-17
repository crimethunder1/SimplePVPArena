package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamMembersAdminMenuSnapshotTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAM_REMOVE_PLAYER_SLOT = 12;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsEveryPlayerOfTheTeamAsAHead() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha");
        fixture.registerTeamOf("Trio", "Steve", "Alex", "Notch");

        openThePlayersOf("Trio");

        assertEquals("""
                Players of Trio | 27
                4 | BOOK | Trio | Players: 3 of 3 / A click on a player removes the player
                9 | PLAYER_HEAD | Steve | Click to remove the player from the team
                10 | PLAYER_HEAD | Alex | Click to remove the player from the team
                11 | PLAYER_HEAD | Notch | Click to remove the player from the team
                22 | OAK_DOOR | Back | Returns to the options of team Trio
                """, fixture.describeLastMenu());
    }

    @Test
    void showsEightPlayersInTheMiddleRow() {
        fixture.setTeamSize(8);
        fixture.registerTeamOf("Octet", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8");

        openThePlayersOf("Octet");

        assertEquals("""
                Players of Octet | 27
                4 | BOOK | Octet | Players: 8 of 8 / A click on a player removes the player
                9 | PLAYER_HEAD | P1 | Click to remove the player from the team
                10 | PLAYER_HEAD | P2 | Click to remove the player from the team
                11 | PLAYER_HEAD | P3 | Click to remove the player from the team
                12 | PLAYER_HEAD | P4 | Click to remove the player from the team
                13 | PLAYER_HEAD | P5 | Click to remove the player from the team
                14 | PLAYER_HEAD | P6 | Click to remove the player from the team
                15 | PLAYER_HEAD | P7 | Click to remove the player from the team
                16 | PLAYER_HEAD | P8 | Click to remove the player from the team
                22 | OAK_DOOR | Back | Returns to the options of team Octet
                """, fixture.describeLastMenu());
    }

    @Test
    void showsThatTheTeamIsNotRegisteredAnyMoreWhenTheMenuIsFilledAfterItsRemoval() {
        fixture.registerTeams("Alpha");
        openThePlayersOf("Alpha");
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        fixture.refillLastMenu();

        assertEquals("""
                Players of Alpha | 27
                4 | BOOK | Alpha | This team is not registered any more
                22 | OAK_DOOR | Back | Returns to the options of team Alpha
                """, fixture.describeLastMenu());
    }

    private void openThePlayersOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);
    }
}
