package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamActionsAdminMenuSnapshotTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int FIRST_TEAM_SLOT = 0;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheMembersOfARegisteredTeam() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals("""
                Team Alpha | 27
                4 | BOOK | Alpha | Members: Alpha-first, Alpha-second
                13 | RED_WOOL | Remove the team | Removes Alpha from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """, fixture.describeLastMenu());
    }

    @Test
    void showsThatTheTeamIsNotRegisteredAnyMoreWhenTheMenuIsFilledAfterItsRemoval() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(FIRST_TEAM_SLOT);
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        fixture.refillLastMenu();

        assertEquals("""
                Team Alpha | 27
                4 | BOOK | Alpha | This team is not registered any more
                13 | RED_WOOL | Remove the team | Removes Alpha from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """, fixture.describeLastMenu());
    }
}
