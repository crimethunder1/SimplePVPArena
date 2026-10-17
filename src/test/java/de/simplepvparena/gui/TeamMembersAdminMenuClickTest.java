package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamMembersAdminMenuClickTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_FIRST_TEAM_SLOT = 0;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;
    private static final int TEAM_REMOVE_PLAYER_SLOT = 12;

    private static final int INFO_SLOT = 4;
    private static final int FIRST_PLAYER_SLOT = 9;
    private static final int SECOND_PLAYER_SLOT = 10;
    private static final int THIRD_PLAYER_SLOT = 11;
    private static final int EMPTY_SLOT = 0;
    private static final int BACK_SLOT = 22;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void removesTheClickedPlayerSavesTheTeamsAndOpensThePlayersAgain() {
        fixture.setTeamSize(3);
        fixture.registerTeamOf("Trio", "Steve", "Alex", "Notch");
        openThePlayersOf("Trio");

        fixture.click(SECOND_PLAYER_SLOT);

        assertEquals(List.of("Alex left team Trio"), fixture.messages());
        assertEquals("Steve, Notch", fixture.teamRegistry().findTeam("Trio").orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertTeamMembersOpened("Trio", 0);
        assertEquals("Notch", fixture.lastMenu().findTile(SECOND_PLAYER_SLOT).orElseThrow().displayName());
        assertTrue(fixture.lastMenu().findTile(THIRD_PLAYER_SLOT).isEmpty());
        assertEquals(List.of("Players: 2 of 3", "A click on a player removes the player"),
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines());
    }

    @Test
    void refusesToRemoveTheLastPlayerAndGreysTheTileInTheOptionsAfterwards() {
        fixture.registerTeamOf("Duo", "Steve", "Alex");
        openThePlayersOf("Duo");
        fixture.click(FIRST_PLAYER_SLOT);
        fixture.messages().clear();
        AdminMenuHolder teamMembersHolder = fixture.lastMenu().holder();

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(List.of("A team keeps at least 1 player, remove the team instead"), fixture.messages());
        assertEquals("Alex", fixture.teamRegistry().findTeam("Duo").orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamConfigurationSaveCount());
        assertSame(teamMembersHolder, fixture.lastMenu().holder());

        fixture.click(BACK_SLOT);

        assertEquals(MenuItems.UNAVAILABLE_MATERIAL,
                fixture.lastMenu().findTile(TEAM_REMOVE_PLAYER_SLOT).orElseThrow().material());
    }

    @Test
    void refusesToRemoveAPlayerOfATeamThatStartedAMatchMeanwhile() {
        fixture.registerTeams("Alpha", "Bravo");
        openThePlayersOf("Alpha");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        AdminMenuHolder teamMembersHolder = fixture.lastMenu().holder();

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(List.of("Team Alpha is in match M1, it cannot be changed before the match is over"),
                fixture.messages());
        assertEquals("Alpha-first, Alpha-second",
                fixture.teamRegistry().findTeam("Alpha").orElseThrow().getMemberNamesJoined());
        assertEquals(0, fixture.teamConfigurationSaveCount());
        assertSame(teamMembersHolder, fixture.lastMenu().holder());
    }

    @Test
    void removesAPlayerOfATeamWithoutAMatchDuringATournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        String teamName = fixture.teamRegistry().getTeamNames().stream()
                .filter(name -> fixture.runningMatch().findTeamByName(name).isEmpty())
                .findFirst()
                .orElseThrow();
        openThePlayersOf(teamName);

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(List.of(teamName + "-first left team " + teamName), fixture.messages());
        assertEquals(teamName + "-second",
                fixture.teamRegistry().findTeam(teamName).orElseThrow().getMemberNamesJoined());
        assertEquals(1, fixture.teamConfigurationSaveCount());
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileOrAnEmptySlotIsClicked() {
        fixture.registerTeams("Alpha");
        openThePlayersOf("Alpha");

        assertNothingHappensOnAClickOn(INFO_SLOT);
        assertNothingHappensOnAClickOn(EMPTY_SLOT);
        assertNothingHappensOnAClickOn(THIRD_PLAYER_SLOT);
    }

    @Test
    void returnsToTheOptionsOfTheTeamAndThenToTheFirstPageOfTheTeamList() {
        fixture.registerTeams("Alpha", "Bravo");
        openThePlayersOf("Alpha");

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAM_ACTIONS, menu.holder().getMenuType());
        assertEquals(Optional.of("Alpha"), menu.holder().getSubject());
        assertEquals(0, menu.holder().getPageIndex());

        fixture.click(BACK_SLOT);

        assertEquals(AdminMenuType.TEAMS, fixture.lastMenu().holder().getMenuType());
        assertEquals("Teams (page 1)", fixture.lastMenu().title());
    }

    @Test
    void returnsToTheOptionsOfTheTeamAndThenToTheSecondPageOfTheTeamList() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);
        assertTeamMembersOpened("Team46", 1);

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAM_ACTIONS, menu.holder().getMenuType());
        assertEquals(Optional.of("Team46"), menu.holder().getSubject());
        assertEquals(1, menu.holder().getPageIndex());

        fixture.click(BACK_SLOT);

        assertEquals(AdminMenuType.TEAMS, fixture.lastMenu().holder().getMenuType());
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
        assertEquals("Teams (page 2)", fixture.lastMenu().title());
    }

    @Test
    void keepsTheSecondPageAfterTheRemovalOfAPlayer() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(List.of("Team46-first left team Team46"), fixture.messages());
        assertTeamMembersOpened("Team46", 1);
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        fixture.registerTeams("Alpha");
        openThePlayersOf("Alpha");
        fixture.revokeAdminPermission();

        fixture.click(FIRST_PLAYER_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertEquals("Alpha-first, Alpha-second",
                fixture.teamRegistry().findTeam("Alpha").orElseThrow().getMemberNamesJoined());
    }

    private void openThePlayersOf(String teamName) {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(fixture.teamRegistry().getTeamNames().indexOf(teamName));
        fixture.click(TEAM_REMOVE_PLAYER_SLOT);
        fixture.messages().clear();
    }

    private void assertTeamMembersOpened(String teamName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAM_MEMBERS, menu.holder().getMenuType());
        assertEquals(Optional.of(teamName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals("Players of " + teamName, menu.title());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder teamMembersHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(teamMembersHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(0, fixture.teamConfigurationSaveCount());
    }
}
