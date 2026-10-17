package de.simplepvparena.gui;

import de.simplepvparena.match.Match;
import de.simplepvparena.tournament.BracketMatch;
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
                4 | BOOK | Alpha | Members: Alpha-first, Alpha-second / Players: 2 of 2
                10 | NAME_TAG | Rename the team | Asks for the new name in the chat / \
                The team keeps its place in the team list / The menu closes so you can type
                11 | GRAY_DYE | Add a player | The team already has 2 players, the team size is 2
                12 | RED_DYE | Remove a player | Shows the players of Alpha / A click on a player removes the player
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
                10 | GRAY_DYE | Rename the team | This team is not registered any more
                11 | GRAY_DYE | Add a player | This team is not registered any more
                12 | GRAY_DYE | Remove a player | This team is not registered any more
                13 | RED_WOOL | Remove the team | Removes Alpha from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """, fixture.describeLastMenu());
    }

    @Test
    void showsEveryTileActiveForATeamBelowTheTeamSizeWithoutATournament() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha", "Bravo");

        openTheOptionsOf("Alpha");

        assertEquals("""
                Team Alpha | 27
                4 | BOOK | Alpha | Members: Alpha-first, Alpha-second / Players: 2 of 3
                10 | NAME_TAG | Rename the team | Asks for the new name in the chat / \
                The team keeps its place in the team list / The menu closes so you can type
                11 | LIME_DYE | Add a player | Asks for the name of the player in the chat / \
                The menu closes so you can type
                12 | RED_DYE | Remove a player | Shows the players of Alpha / A click on a player removes the player
                13 | RED_WOOL | Remove the team | Removes Alpha from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheAddTileGreyForATeamThatReachedTheTeamSize() {
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Trio", "Steve", "Alex", "Notch");

        openTheOptionsOf("Trio");

        assertEquals("""
                Team Trio | 27
                4 | BOOK | Trio | Members: Steve, Alex, Notch / Players: 3 of 3
                10 | NAME_TAG | Rename the team | Asks for the new name in the chat / \
                The team keeps its place in the team list / The menu closes so you can type
                11 | GRAY_DYE | Add a player | The team already has 3 players, the team size is 3
                12 | RED_DYE | Remove a player | Shows the players of Trio / A click on a player removes the player
                13 | RED_WOOL | Remove the team | Removes Trio from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheAddTileGreyForATeamLargerThanTheTeamSize() {
        fixture.registerTeamOf("Quartet", "Steve", "Alex", "Notch", "Jeb_");

        openTheOptionsOf("Quartet");

        assertEquals("4 | BOOK | Quartet | Members: Steve, Alex, Notch, Jeb_ / Players: 4 of 2",
                describeSlot(4));
        assertEquals("11 | GRAY_DYE | Add a player | The team already has 4 players, the team size is 2",
                describeSlot(11));
    }

    @Test
    void showsTheRemovePlayerTileGreyForATeamOfOnePlayer() {
        fixture.registerTeamOf("Solo", "Steve");

        openTheOptionsOf("Solo");

        assertEquals("""
                Team Solo | 27
                4 | BOOK | Solo | Members: Steve / Players: 1 of 2
                10 | NAME_TAG | Rename the team | Asks for the new name in the chat / \
                The team keeps its place in the team list / The menu closes so you can type
                11 | LIME_DYE | Add a player | Asks for the name of the player in the chat / \
                The menu closes so you can type
                12 | GRAY_DYE | Remove a player | A team keeps at least 1 player, remove the team instead
                13 | RED_WOOL | Remove the team | Removes Solo from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """, fixture.describeLastMenu());
    }

    @Test
    void showsOnlyTheRenameTileGreyForATeamWithoutAMatchDuringATournament() {
        fixture.configureArena();
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        String teamName = findTeamWithoutTheRunningMatch();

        openTheOptionsOf(teamName);

        assertEquals("""
                Team %1$s | 27
                4 | BOOK | %1$s | Members: %1$s-first, %1$s-second / Players: 2 of 3
                10 | GRAY_DYE | Rename the team | A team cannot be renamed while a tournament is in progress, \
                the bracket keeps its name
                11 | LIME_DYE | Add a player | Asks for the name of the player in the chat / \
                The menu closes so you can type
                12 | RED_DYE | Remove a player | Shows the players of %1$s / A click on a player removes the player
                13 | RED_WOOL | Remove the team | Removes %1$s from the registry / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the team list
                """.formatted(teamName), fixture.describeLastMenu());
    }

    @Test
    void showsEveryChangeTileGreyForATeamOfTheRunningMatch() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha", "Bravo", "Charlie");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");

        openTheOptionsOf("Alpha");

        String reason = "Team Alpha is in match M1, it cannot be changed before the match is over";
        assertEquals("""
                Team Alpha | 27
                4 | BOOK | Alpha | Members: Alpha-first, Alpha-second / Players: 2 of 3
                10 | GRAY_DYE | Rename the team | %1$s
                11 | GRAY_DYE | Add a player | %1$s
                12 | GRAY_DYE | Remove a player | %1$s
                13 | GRAY_DYE | Remove the team | %1$s
                22 | OAK_DOOR | Back | Returns to the team list
                """.formatted(reason), fixture.describeLastMenu());
    }

    @Test
    void showsEveryChangeTileGreyForATeamWhoseMatchWaitsForTheKitChoice() {
        AdminMenuFixture kitChoiceFixture = AdminMenuFixture.withKitChoice();
        kitChoiceFixture.configureArena();
        kitChoiceFixture.setTeamSize(3);
        kitChoiceFixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        kitChoiceFixture.createKit("Archer");
        kitChoiceFixture.createKit("Knight");
        kitChoiceFixture.startTournament();
        assertTrue(kitChoiceFixture.kitChoiceService().isPending());
        BracketMatch choosingMatch = kitChoiceFixture.tournamentManager().getBracket().orElseThrow()
                .findMatch("round1-match1").orElseThrow();
        String teamName = choosingMatch.getFirstTeamName().orElseThrow();
        kitChoiceFixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        kitChoiceFixture.click(kitChoiceFixture.teamRegistry().getTeamNames().indexOf(teamName));

        String reason = "Team " + teamName + " is waiting for the kit choice of match round1-match1, it cannot be "
                + "changed before the match is over";
        assertEquals("""
                Team %1$s | 27
                4 | BOOK | %1$s | Members: %1$s-first, %1$s-second / Players: 2 of 3
                10 | GRAY_DYE | Rename the team | A team cannot be renamed while a tournament is in progress, \
                the bracket keeps its name
                11 | GRAY_DYE | Add a player | %2$s
                12 | GRAY_DYE | Remove a player | %2$s
                13 | GRAY_DYE | Remove the team | %2$s
                22 | OAK_DOOR | Back | Returns to the team list
                """.formatted(teamName, reason), kitChoiceFixture.describeLastMenu());
    }

    private void openTheOptionsOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
    }

    private String findTeamWithoutTheRunningMatch() {
        Match runningMatch = fixture.runningMatch();
        return fixture.teamRegistry().getTeamNames().stream()
                .filter(teamName -> runningMatch.findTeamByName(teamName).isEmpty())
                .findFirst()
                .orElseThrow();
    }

    private String describeSlot(int slot) {
        return fixture.describeLastMenu().lines()
                .filter(line -> line.startsWith(slot + " | "))
                .findFirst()
                .orElseThrow();
    }
}
