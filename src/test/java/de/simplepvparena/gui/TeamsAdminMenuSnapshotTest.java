package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamsAdminMenuSnapshotTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int NEXT_PAGE_SLOT = 53;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheTeamsMenuWithoutTeams() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals("""
                Teams (page 1) | 54
                47 | BOOK | Teams | Registered teams: 0 / Page 1 of 1 / A click on a team opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Add a team | Asks for <teamName> <player> [<player> ...] in the chat / \
                Player names cannot be typed into an inventory / The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheTeamsMenuWithOneTeam() {
        fixture.registerTeams("Alpha");

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals("""
                Teams (page 1) | 54
                0 | PLAYER_HEAD | Alpha | Members: Alpha-first, Alpha-second / Click for the options of this team
                47 | BOOK | Teams | Registered teams: 1 / Page 1 of 1 / A click on a team opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Add a team | Asks for <teamName> <player> [<player> ...] in the chat / \
                Player names cannot be typed into an inventory / The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTeamsOfOneThreeAndEightPlayers() {
        fixture.registerTeamOf("Solo", "Steve");
        fixture.registerTeamOf("Trio", "Alex", "Notch", "Jeb_");
        fixture.registerTeamOf("Squad", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8");

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals("""
                Teams (page 1) | 54
                0 | PLAYER_HEAD | Solo | Members: Steve / Click for the options of this team
                1 | PLAYER_HEAD | Trio | Members: Alex, Notch, Jeb_ / Click for the options of this team
                2 | PLAYER_HEAD | Squad | Members: P1, P2, P3, P4, P5, P6, P7, P8 / Click for the options of this team
                47 | BOOK | Teams | Registered teams: 3 / Page 1 of 1 / A click on a team opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Add a team | Asks for <teamName> <player> [<player> ...] in the chat / \
                Player names cannot be typed into an inventory / The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void showsAFullPageOfFortyFiveTeamsWithoutAnotherPage() {
        fixture.registerNumberedTeams(45);

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals("Teams (page 1) | 54\n" + describeTeamSlots(1, 45) + """
                47 | BOOK | Teams | Registered teams: 45 / Page 1 of 1 / A click on a team opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Add a team | Asks for <teamName> <player> [<player> ...] in the chat / \
                Player names cannot be typed into an inventory / The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheFirstPageOfFortySixTeamsWithANextPage() {
        fixture.registerNumberedTeams(46);

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals("Teams (page 1) | 54\n" + describeTeamSlots(1, 45) + """
                47 | BOOK | Teams | Registered teams: 46 / Page 1 of 2 / A click on a team opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Add a team | Asks for <teamName> <player> [<player> ...] in the chat / \
                Player names cannot be typed into an inventory / The menu closes so you can type
                53 | ARROW | Next page | Page 2 of 2
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheSecondPageOfFortySixTeamsWithAPreviousPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(NEXT_PAGE_SLOT);

        assertEquals("""
                Teams (page 2) | 54
                0 | PLAYER_HEAD | Team46 | Members: Team46-first, Team46-second / Click for the options of this team
                45 | ARROW | Previous page | Page 1 of 2
                47 | BOOK | Teams | Registered teams: 46 / Page 2 of 2 / A click on a team opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Add a team | Asks for <teamName> <player> [<player> ...] in the chat / \
                Player names cannot be typed into an inventory / The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void namesTheTargetPageOnTheNextPageTileOfTheFirstOfTwoPages() {
        fixture.registerNumberedTeams(46);

        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertEquals(List.of("Page 2 of 2"),
                fixture.lastMenu().findTile(NEXT_PAGE_SLOT).orElseThrow().loreLines());
        assertEquals("Page 1 of 2", fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines().get(1));
    }

    @Test
    void namesTheTargetPageOnThePreviousPageTileOfTheSecondOfTwoPages() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(NEXT_PAGE_SLOT);

        assertEquals(List.of("Page 1 of 2"),
                fixture.lastMenu().findTile(PREVIOUS_PAGE_SLOT).orElseThrow().loreLines());
        assertEquals("Page 2 of 2", fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines().get(1));
    }

    private static String describeTeamSlots(int firstTeamNumber, int lastTeamNumber) {
        StringBuilder description = new StringBuilder();
        for (int teamNumber = firstTeamNumber; teamNumber <= lastTeamNumber; teamNumber++) {
            String teamName = AdminMenuFixture.numberedTeamName(teamNumber);
            description.append(teamNumber - firstTeamNumber).append(" | PLAYER_HEAD | ").append(teamName)
                    .append(" | Members: ").append(teamName).append("-first, ").append(teamName)
                    .append("-second / Click for the options of this team\n");
        }
        return description.toString();
    }
}
