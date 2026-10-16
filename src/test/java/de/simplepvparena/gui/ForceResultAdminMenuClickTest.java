package de.simplepvparena.gui;

import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceResultAdminMenuClickTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_FORCE_RESULT_SLOT = 16;

    private static final int MENU_SIZE = 27;
    private static final int EMPTY_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int FIRST_TEAM_SLOT = 11;
    private static final int SECOND_TEAM_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private static final String MATCH_ID = "round1-match1";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void decidesTheRunningMatchForTheFirstTeamAndReturnsToTheMatchMenu() {
        openTheForceResultMenuOfARunningTournament();
        String firstTeamName = fixture.runningFirstTeamName();

        fixture.click(FIRST_TEAM_SLOT);

        assertDecidedFor(firstTeamName);
    }

    @Test
    void decidesTheRunningMatchForTheSecondTeamAndReturnsToTheMatchMenu() {
        openTheForceResultMenuOfARunningTournament();
        String secondTeamName = fixture.runningSecondTeamName();

        fixture.click(SECOND_TEAM_SLOT);

        assertDecidedFor(secondTeamName);
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClickedWhileTheMatchRuns() {
        openTheForceResultMenuOfARunningTournament();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClickedWhileTheMatchRuns() {
        openTheForceResultMenuOfARunningTournament();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void returnsToTheMatchMenuWithoutDecidingTheMatch() {
        openTheForceResultMenuOfARunningTournament();

        fixture.click(BACK_SLOT);

        assertMatchMenuOpened();
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(MATCH_ID, fixture.runningMatch().getMatchId());
        assertTrue(bracketMatch().getWinnerTeamName().isEmpty());
    }

    @Test
    void warnsOnEverySlotButBackThatTheMatchIsNotRunningAnyMoreAndReturnsToTheMatchMenu() {
        openTheForceResultMenuOfARunningTournament();
        AdminMenuHolder forceResultHolder = fixture.lastMenu().holder();
        String firstTeamName = fixture.runningFirstTeamName();
        fixture.decideRunningMatchFor(firstTeamName);

        for (int slot = 0; slot < MENU_SIZE; slot++) {
            if (slot == BACK_SLOT) {
                continue;
            }
            fixture.messages().clear();

            fixture.clickIn(forceResultHolder, slot);

            assertEquals(List.of("Match " + MATCH_ID + " is not running any more"), fixture.messages(),
                    "slot " + slot);
            assertMatchMenuOpened();
            assertEquals(Optional.of(firstTeamName), bracketMatch().getWinnerTeamName(), "slot " + slot);
        }
    }

    @Test
    void returnsToTheMatchMenuWithoutAWarningWhenTheMatchIsNotRunningAnyMore() {
        openTheForceResultMenuOfARunningTournament();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        fixture.messages().clear();

        fixture.click(BACK_SLOT);

        assertMatchMenuOpened();
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheForceResultMenuOfARunningTournament();
        AdminMenuHolder forceResultHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(forceResultHolder, fixture.lastMenu().holder());
        assertEquals(MATCH_ID, fixture.runningMatch().getMatchId());
        assertTrue(bracketMatch().getWinnerTeamName().isEmpty());
    }

    private void openTheForceResultMenuOfARunningTournament() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_FORCE_RESULT_SLOT);
        assertEquals(AdminMenuType.FORCE_RESULT, fixture.lastMenu().holder().getMenuType());
    }

    private BracketMatch bracketMatch() {
        return fixture.tournamentManager().getBracket().orElseThrow().findMatch(MATCH_ID).orElseThrow();
    }

    private void assertDecidedFor(String winningTeamName) {
        assertEquals(List.of("Match " + MATCH_ID + " was decided for " + winningTeamName), fixture.messages());
        assertTrue(bracketMatch().isDecided());
        assertEquals(Optional.of(winningTeamName), bracketMatch().getWinnerTeamName());
        assertTrue(fixture.matchService().findMatchById(MATCH_ID).isEmpty());
        assertMatchMenuOpened();
        assertEquals("round1-match2", fixture.runningMatch().getMatchId());
        assertEquals("Running match: " + fixture.runningMatch().describe(),
                fixture.lastMenu().findTile(INFO_SLOT).orElseThrow().loreLines().get(1));
    }

    private void assertMatchMenuOpened() {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.MATCH, menu.holder().getMenuType());
        assertEquals("Match and tournament", menu.title());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder forceResultHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(forceResultHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(MATCH_ID, fixture.runningMatch().getMatchId());
    }
}
