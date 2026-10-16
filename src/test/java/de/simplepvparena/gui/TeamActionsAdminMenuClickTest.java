package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamActionsAdminMenuClickTest {

    private static final int MAIN_TEAMS_SLOT = 11;
    private static final int TEAMS_FIRST_TEAM_SLOT = 0;
    private static final int TEAMS_NEXT_PAGE_SLOT = 53;

    private static final int EMPTY_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int REMOVE_SLOT = 13;
    private static final int BACK_SLOT = 22;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        openTheOptionsOfAlpha();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        openTheOptionsOfAlpha();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void asksForTheConfirmationOfTheRemovalWithTheTeamNameAndTheFirstPage() {
        openTheOptionsOfAlpha();

        fixture.click(REMOVE_SLOT);

        assertRemovalConfirmationOpened("Alpha", 0);
        assertTrue(fixture.teamRegistry().findTeam("Alpha").isPresent(), "nothing is removed before the confirmation");
    }

    @Test
    void asksForTheConfirmationOfTheRemovalWithTheTeamNameAndTheSecondPage() {
        openTheOptionsOfTheOnlyTeamOfTheSecondPage();

        fixture.click(REMOVE_SLOT);

        assertRemovalConfirmationOpened("Team46", 1);
        assertEquals(46, fixture.teamRegistry().getTeamCount());
    }

    @Test
    void returnsToTheFirstPageOfTheTeamList() {
        openTheOptionsOfAlpha();

        fixture.click(BACK_SLOT);

        assertTeamsOpened(0, "Teams (page 1)");
    }

    @Test
    void returnsToTheSecondPageOfTheTeamList() {
        openTheOptionsOfTheOnlyTeamOfTheSecondPage();

        fixture.click(BACK_SLOT);

        assertTeamsOpened(1, "Teams (page 2)");
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheOptionsOfAlpha();
        AdminMenuHolder teamActionsHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(REMOVE_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(teamActionsHolder, fixture.lastMenu().holder());
    }

    private void openTheOptionsOfAlpha() {
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
    }

    private void openTheOptionsOfTheOnlyTeamOfTheSecondPage() {
        fixture.registerNumberedTeams(46);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.click(TEAMS_NEXT_PAGE_SLOT);
        fixture.click(TEAMS_FIRST_TEAM_SLOT);
    }

    private void assertRemovalConfirmationOpened(String teamName, int pageIndex) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.REMOVE_TEAM), menu.holder().getConfirmationRequest());
        assertEquals(Optional.of(teamName), menu.holder().getSubject());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals("Remove the team?", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertTeamsOpened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.TEAMS, menu.holder().getMenuType());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder teamActionsHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(teamActionsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }
}
