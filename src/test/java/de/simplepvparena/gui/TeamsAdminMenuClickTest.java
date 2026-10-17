package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamsAdminMenuClickTest {

    private static final int MAIN_TEAMS_SLOT = 11;

    private static final int FIRST_TEAM_SLOT = 0;
    private static final int LAST_TEAM_SLOT_OF_A_PAGE = 44;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int UNUSED_SLOT_BESIDE_THE_PREVIOUS_PAGE = 46;
    private static final int INFO_SLOT = 47;
    private static final int UNUSED_SLOT_BESIDE_THE_INFO = 48;
    private static final int BACK_SLOT = 49;
    private static final int UNUSED_SLOT_BESIDE_THE_BACK_TILE = 50;
    private static final int ADD_SLOT = 51;
    private static final int UNUSED_SLOT_BESIDE_THE_ADD_TILE = 52;
    private static final int NEXT_PAGE_SLOT = 53;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void opensTheOptionsOfEveryTeamOfTheFirstPage() {
        fixture.registerTeams("Alpha", "Bravo", "Charlie");
        List<String> teamNames = List.of("Alpha", "Bravo", "Charlie");

        for (int slot = 0; slot < teamNames.size(); slot++) {
            fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

            fixture.click(slot);

            assertTeamActionsOpened(teamNames.get(slot), 0);
        }
    }

    @Test
    void opensTheOptionsOfTheLastTeamOfAFullPage() {
        fixture.registerNumberedTeams(45);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(LAST_TEAM_SLOT_OF_A_PAGE);

        assertTeamActionsOpened("Team45", 0);
    }

    @Test
    void opensTheOptionsOfATeamOfTheSecondPageWithThatPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(FIRST_TEAM_SLOT);

        assertTeamActionsOpened("Team46", 1);
    }

    @Test
    void leavesTheMenuAsItIsWhenASlotWithoutATeamIsClicked() {
        fixture.registerTeams("Alpha", "Bravo", "Charlie");

        for (int slot : List.of(3, LAST_TEAM_SLOT_OF_A_PAGE, UNUSED_SLOT_BESIDE_THE_PREVIOUS_PAGE,
                UNUSED_SLOT_BESIDE_THE_INFO, UNUSED_SLOT_BESIDE_THE_BACK_TILE, UNUSED_SLOT_BESIDE_THE_ADD_TILE)) {
            fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

            assertNothingHappensOnAClickOn(slot);
        }
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void turnsToTheNextPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(NEXT_PAGE_SLOT);

        assertTeamsOpened(1, "Teams (page 2)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void turnsBackToThePreviousPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(PREVIOUS_PAGE_SLOT);

        assertTeamsOpened(0, "Teams (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void ignoresTheEmptyPreviousPageSlotOnTheFirstPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertNothingHappensOnAClickOn(PREVIOUS_PAGE_SLOT);
    }

    @Test
    void ignoresTheEmptyNextPageSlotOnTheLastPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(NEXT_PAGE_SLOT);

        assertNothingHappensOnAClickOn(NEXT_PAGE_SLOT);
    }

    @Test
    void returnsToTheMainMenu() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.MAIN, menu.holder().getMenuType());
        assertEquals("Tournament administration", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void closesTheMenuAndRegistersAChatRequestForTheTeamToAdd() {
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        fixture.click(ADD_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertTrue(fixture.chatInputService().isWaiting(fixture.admin()));
        assertEquals(1, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void knownBehaviourReadsTheTeamListAgainOnTheClickAndOpensTheTeamThatMovedUpToTheClickedSlot() {
        fixture.registerTeams("Alpha", "Bravo", "Charlie");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        assertEquals("Alpha", fixture.lastMenu().findTile(FIRST_TEAM_SLOT).orElseThrow().displayName());
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        fixture.click(FIRST_TEAM_SLOT);

        assertTeamActionsOpened("Bravo", 0);
    }

    @Test
    void knownBehaviourReadsTheTeamListAgainOnTheClickAndIgnoresTheShownLastTeamThatHasNoSlotAnyMore() {
        fixture.registerTeams("Alpha", "Bravo", "Charlie");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        assertEquals("Charlie", fixture.lastMenu().findTile(2).orElseThrow().displayName());
        assertTrue(fixture.teamRegistry().removeTeam("Alpha"));

        assertNothingHappensOnAClickOn(2);
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        AdminMenuHolder teamsHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(teamsHolder, fixture.lastMenu().holder());
        assertEquals(0, fixture.pendingChatInputCount());
    }

    private void assertTeamActionsOpened(String teamName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAM_ACTIONS, menu.holder().getMenuType());
        assertEquals(Optional.of(teamName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals("Team " + teamName, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertTeamsOpened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAMS, menu.holder().getMenuType());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder teamsHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(teamsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(0, fixture.pendingChatInputCount());
    }
}
