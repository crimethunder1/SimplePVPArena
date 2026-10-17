package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
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
    private static final int SMALLER_TEAMS_SLOT = 46;
    private static final int INFO_SLOT = 47;
    private static final int LARGER_TEAMS_SLOT = 48;
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

        for (int slot : List.of(3, LAST_TEAM_SLOT_OF_A_PAGE, UNUSED_SLOT_BESIDE_THE_BACK_TILE,
                UNUSED_SLOT_BESIDE_THE_ADD_TILE)) {
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

    @Test
    void makesTheTeamsLargerWritesConfigYmlAndRefreshesTheMenu() {
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        AdminMenuHolder teamsHolder = fixture.lastMenu().holder();

        fixture.click(LARGER_TEAMS_SLOT);

        assertEquals(3, fixture.teamSize());
        assertEquals(List.of("Team size is now 3 (3 versus 3)"), fixture.messages());
        assertEquals(List.of(3), fixture.writtenTeamSizes());
        assertTeamSizeTilesRefreshed(teamsHolder, "Team size: 3 (3 versus 3)",
                "Click to make teams of 2 players", "Click to make teams of 4 players");
    }

    @Test
    void makesTheTeamsSmallerWritesConfigYmlAndRefreshesTheMenu() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        AdminMenuHolder teamsHolder = fixture.lastMenu().holder();

        fixture.click(SMALLER_TEAMS_SLOT);

        assertEquals(2, fixture.teamSize());
        assertEquals(List.of("Team size is now 2 (2 versus 2)"), fixture.messages());
        assertEquals(List.of(2), fixture.writtenTeamSizes());
        assertTeamSizeTilesRefreshed(teamsHolder, "Team size: 2 (2 versus 2)",
                "Click to make teams of 1 player", "Click to make teams of 3 players");
    }

    @Test
    void walksTheTeamSizeFromOneToEightAndBackAndGreysTheTileAtEachEnd() {
        fixture.setTeamSize(1);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        for (int teamSize = 2; teamSize <= 8; teamSize++) {
            fixture.click(LARGER_TEAMS_SLOT);
            assertEquals(teamSize, fixture.teamSize());
        }
        assertEquals("GRAY_DYE", fixture.lastMenu().findTile(LARGER_TEAMS_SLOT).orElseThrow().material().name());
        for (int teamSize = 7; teamSize >= 1; teamSize--) {
            fixture.click(SMALLER_TEAMS_SLOT);
            assertEquals(teamSize, fixture.teamSize());
        }

        assertEquals("GRAY_DYE", fixture.lastMenu().findTile(SMALLER_TEAMS_SLOT).orElseThrow().material().name());
        assertEquals(List.of(2, 3, 4, 5, 6, 7, 8, 7, 6, 5, 4, 3, 2, 1), fixture.writtenTeamSizes());
        assertEquals("Team size is now 1 (1 versus 1)", fixture.messages().get(fixture.messages().size() - 1));
        assertEquals(14, fixture.lastMenu().refreshCount());
    }

    @Test
    void reportsTheReasonOfTheGreySmallerTeamsTileAndChangesNothing() {
        fixture.setTeamSize(1);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertTeamSizeRefused(SMALLER_TEAMS_SLOT, "The smallest team size is 1", 1);
    }

    @Test
    void reportsTheReasonOfTheGreyLargerTeamsTileAndChangesNothing() {
        fixture.setTeamSize(8);
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertTeamSizeRefused(LARGER_TEAMS_SLOT, "The largest team size is 8", 8);
    }

    @Test
    void refusesToChangeTheTeamSizeWhileATournamentIsRunning() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertTeamSizeRefused(LARGER_TEAMS_SLOT,
                "The team size cannot be changed while a tournament is in progress", 2);
        assertTeamSizeRefused(SMALLER_TEAMS_SLOT,
                "The team size cannot be changed while a tournament is in progress", 2);
    }

    @Test
    void refusesToChangeTheTeamSizeWhileATournamentIsPaused() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.pauseTournament();
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertTeamSizeRefused(LARGER_TEAMS_SLOT,
                "The team size cannot be changed while a tournament is in progress", 2);
    }

    @Test
    void refusesSmallerTeamsWhileATeamHasMorePlayersAndNamesIt() {
        fixture.setTeamSize(3);
        fixture.registerTeams("Alpha");
        fixture.registerTeamOf("Trio", "Steve", "Alex", "Notch");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);

        assertTeamSizeRefused(SMALLER_TEAMS_SLOT,
                "1 team has more than 2 players: Trio (Steve, Alex, Notch). Remove it first", 3);
        assertEquals(List.of("Alpha", "Trio"), fixture.teamRegistry().getTeamNames());
    }

    @Test
    void keepsTheNewTeamSizeWhenConfigYmlCannotBeWrittenAndSaysSo() {
        fixture.registerTeams("Alpha");
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        AdminMenuHolder teamsHolder = fixture.lastMenu().holder();
        fixture.failTeamSizeWrites(new IOException("config.yml is read only"));

        fixture.click(LARGER_TEAMS_SLOT);

        assertEquals(3, fixture.teamSize());
        assertEquals(List.of("Team size is now 3 (3 versus 3), but config.yml could not be written: "
                + "config.yml is read only"), fixture.messages());
        assertEquals(List.of(), fixture.writtenTeamSizes());
        assertTeamSizeTilesRefreshed(teamsHolder, "Team size: 3 (3 versus 3)",
                "Click to make teams of 2 players", "Click to make teams of 4 players");
    }

    @Test
    void namesTheKindOfWriteFailureThatCarriesNoMessage() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.failTeamSizeWrites(new IOException());

        fixture.click(SMALLER_TEAMS_SLOT);

        assertEquals(1, fixture.teamSize());
        assertEquals(List.of("Team size is now 1 (1 versus 1), but config.yml could not be written: IOException"),
                fixture.messages());
    }

    @Test
    void refusesTheTeamSizeClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_TEAMS_SLOT);
        fixture.revokeAdminPermission();

        fixture.click(LARGER_TEAMS_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertEquals(2, fixture.teamSize());
        assertEquals(List.of(), fixture.writtenTeamSizes());
    }

    private void assertTeamSizeTilesRefreshed(AdminMenuHolder teamsHolder, String teamSizeLine,
                                              String smallerTeamsLore, String largerTeamsLore) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertSame(teamsHolder, menu.holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(1, menu.refreshCount());
        assertEquals(teamSizeLine, menu.findTile(INFO_SLOT).orElseThrow().loreLines().get(2));
        assertEquals(List.of(smallerTeamsLore), menu.findTile(SMALLER_TEAMS_SLOT).orElseThrow().loreLines());
        assertEquals(List.of(largerTeamsLore), menu.findTile(LARGER_TEAMS_SLOT).orElseThrow().loreLines());
    }

    private void assertTeamSizeRefused(int slot, String reason, int unchangedTeamSize) {
        AdminMenuHolder teamsHolder = fixture.lastMenu().holder();
        fixture.messages().clear();

        fixture.click(slot);

        assertEquals(List.of(reason), fixture.messages());
        assertEquals(unchangedTeamSize, fixture.teamSize());
        assertEquals(List.of(), fixture.writtenTeamSizes());
        assertSame(teamsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
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
