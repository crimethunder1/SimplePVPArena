package de.simplepvparena.gui;

import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.TournamentState;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchAdminMenuClickTest {

    private static final int MAIN_MATCH_SLOT = 12;

    private static final int INFO_SLOT = 4;
    private static final int EMPTY_SLOT = 0;
    private static final int START_SLOT = 10;
    private static final int FORCE_START_SLOT = 11;
    private static final int PAUSE_SLOT = 12;
    private static final int CANCEL_SLOT = 13;
    private static final int FINAL_SLOT = 15;
    private static final int FORCE_RESULT_SLOT = 16;
    private static final int BACK_SLOT = 22;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void startsTheTournamentAndRefreshesTheMenu() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(START_SLOT);

        assertEquals(List.of("Tournament started"), fixture.messages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertRefreshedOnce(matchHolder);
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL,
                fixture.lastMenu().findTile(START_SLOT).orElseThrow().material());
    }

    @Test
    void reportsTheMissingTeamsWhenTheTournamentIsStartedWithoutTeamsAndRefreshesTheMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(START_SLOT);

        assertEquals(List.of("At least 2 teams are required"), fixture.messages());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void refusesTheStartOfAnUnevenTeamCountAndRefreshesTheMenu() {
        openTheMatchMenuWithTeams("Alpha", "Bravo", "Charlie");
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(START_SLOT);

        assertEquals(List.of("The team count is uneven, use \"Force start\" in the match menu of /tournament "
                + "to start anyway"), fixture.messages());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void stillTriggersTheUnavailableStartTileWhileATournamentIsInProgress() {
        openTheMatchMenuOfARunningTournament();
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(START_SLOT);

        assertEquals(List.of("A tournament is already in progress"), fixture.messages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void forceStartsTheTournamentWithAnUnevenTeamCountAndRefreshesTheMenu() {
        openTheMatchMenuWithTeams("Alpha", "Bravo", "Charlie");
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(FORCE_START_SLOT);

        assertEquals(List.of("Tournament started"), fixture.messages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void stillTriggersTheUnavailableForceStartTileWhileATournamentIsInProgress() {
        openTheMatchMenuOfARunningTournament();
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(FORCE_START_SLOT);

        assertEquals(List.of("A tournament is already in progress"), fixture.messages());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void pausesTheRunningTournamentAndRefreshesTheMenu() {
        openTheMatchMenuOfARunningTournament();
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(PAUSE_SLOT);

        assertEquals(List.of("Tournament paused"), fixture.messages());
        assertEquals(TournamentState.PAUSED, fixture.tournamentManager().getState());
        assertTrue(fixture.matchService().isRoundTimerPaused());
        assertRefreshedOnce(matchHolder);
        assertEquals("Continue the tournament",
                fixture.lastMenu().findTile(PAUSE_SLOT).orElseThrow().displayName());
    }

    @Test
    void continuesThePausedTournamentAndRefreshesTheMenu() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.pauseTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(PAUSE_SLOT);

        assertEquals(List.of("Tournament resumed"), fixture.messages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
        assertFalse(fixture.matchService().isRoundTimerPaused());
        assertRefreshedOnce(matchHolder);
        assertEquals("Pause the tournament", fixture.lastMenu().findTile(PAUSE_SLOT).orElseThrow().displayName());
    }

    @Test
    void stillTriggersTheUnavailablePauseTileWithoutATournament() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(PAUSE_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void asksForTheConfirmationOfTheCancelWhileATournamentIsInProgress() {
        openTheMatchMenuOfARunningTournament();

        fixture.click(CANCEL_SLOT);

        assertCancelConfirmationOpened();
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState(),
                "nothing is cancelled before the confirmation");
    }

    @Test
    void asksForTheConfirmationOfTheCancelEvenThoughTheTileIsUnavailableWithoutATournament() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL,
                fixture.lastMenu().findTile(CANCEL_SLOT).orElseThrow().material());

        fixture.click(CANCEL_SLOT);

        assertCancelConfirmationOpened();
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
    }

    @Test
    void startsTheReadyFinalAndRefreshesTheMenu() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(FINAL_SLOT);

        assertEquals(List.of("The final has started"), fixture.messages());
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
        assertRefreshedOnce(matchHolder);
        MenuTile finalTile = fixture.lastMenu().findTile(FINAL_SLOT).orElseThrow();
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL, finalTile.material());
        assertEquals(List.of("The final is not ready, its stage is RUNNING"), finalTile.loreLines());
    }

    @Test
    void stillTriggersTheUnavailableFinalTileWithoutATournamentAndRefreshesTheMenu() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(FINAL_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertEquals(FinalMatchStage.PENDING, fixture.tournamentManager().getFinalMatchStage());
        assertRefreshedOnce(matchHolder);
    }

    @Test
    void opensTheForceResultMenuOfTheRunningMatch() {
        openTheMatchMenuOfARunningTournament();

        fixture.click(FORCE_RESULT_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.FORCE_RESULT, menu.holder().getMenuType());
        assertEquals(Optional.of("round1-match1"), menu.holder().getSubject());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals("Result of round1-match1", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertTrue(fixture.matchService().hasActiveMatch(), "nothing is decided by opening the menu");
    }

    @Test
    void refusesTheForceResultWithoutARunningMatchAndKeepsTheMenuAsItIs() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(FORCE_RESULT_SLOT);

        assertEquals(List.of("No match is running"), fixture.messages());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
    }

    @Test
    void returnsToTheMainMenu() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.MAIN, menu.holder().getMenuType());
        assertEquals("Tournament administration", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheMatchMenuWithTeams("Alpha", "Bravo");
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(START_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
    }

    private void openTheMatchMenuWithTeams(String... teamNames) {
        fixture.configureArena();
        fixture.registerTeams(teamNames);
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
    }

    private void openTheMatchMenuOfARunningTournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        assertEquals(Material.CLOCK, fixture.lastMenu().findTile(PAUSE_SLOT).orElseThrow().material());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertRefreshedOnce(AdminMenuHolder matchHolder) {
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(1, fixture.lastMenu().refreshCount());
    }

    private void assertCancelConfirmationOpened() {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.CANCEL_TOURNAMENT), menu.holder().getConfirmationRequest());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals("Cancel the tournament?", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }
}
