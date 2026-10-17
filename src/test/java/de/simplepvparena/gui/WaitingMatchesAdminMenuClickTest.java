package de.simplepvparena.gui;

import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingMatchesAdminMenuClickTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;

    private static final int FIRST_MATCH_SLOT = 0;
    private static final int EMPTY_SLOT = 1;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final int TEAM_COUNT_FOR_TWO_PAGES = 128;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void opensTheWaitingMatchesFromTheMatchMenuWhileTheTournamentRuns() {
        startATournamentOfFourTeams();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);

        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals("round1-match1", fixture.runningMatch().getMatchId(), "nothing is decided by opening the menu");
    }

    @Test
    void opensTheWaitingMatchesFromTheMatchMenuWhileTheTournamentIsPaused() {
        startATournamentOfFourTeams();
        fixture.pauseTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);

        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void stillTriggersTheUnavailableTileWithoutATournamentAndReportsTheReason() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL,
                fixture.lastMenu().findTile(MATCH_DECIDE_WAITING_MATCH_SLOT).orElseThrow().material());

        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
    }

    @Test
    void stillTriggersTheUnavailableTileAfterTheTournamentIsFinishedAndReportsTheReason() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();
        fixture.decideRunningMatchFor("Alpha");
        assertEquals(TournamentState.FINISHED, fixture.tournamentManager().getState());
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();

        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
    }

    @Test
    void opensTheResultChoiceOfTheClickedMatch() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();

        fixture.click(FIRST_MATCH_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.WAITING_MATCH_RESULT, menu.holder().getMenuType());
        assertEquals(Optional.of("round1-match2"), menu.holder().getSubject());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals("Result of round1-match2", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertFalse(fixture.tournamentManager().getBracket().orElseThrow().findMatch("round1-match2")
                .orElseThrow().isDecided(), "nothing is decided by opening the result choice");
    }

    @Test
    void opensTheResultChoiceOfAMatchOnTheSecondPageWithThatPage() {
        startATournamentForTwoPages();
        openTheWaitingMatches();
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(FIRST_MATCH_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.WAITING_MATCH_RESULT, menu.holder().getMenuType());
        assertEquals(Optional.of("round1-match47"), menu.holder().getSubject());
        assertEquals(1, menu.holder().getPageIndex());
        assertEquals("Result of round1-match47", menu.title());
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenTheFirstSlotIsClickedWithoutAWaitingMatch() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.startFinal();
        openTheWaitingMatches();

        assertNothingHappensOnAClickOn(FIRST_MATCH_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenThePageSlotsAreClickedOnTheOnlyPage() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();

        assertNothingHappensOnAClickOn(PREVIOUS_PAGE_SLOT);
        assertNothingHappensOnAClickOn(NEXT_PAGE_SLOT);
    }

    @Test
    void turnsToTheNextPageAndBack() {
        startATournamentForTwoPages();
        openTheWaitingMatches();

        fixture.click(NEXT_PAGE_SLOT);

        assertWaitingMatchesOpened(1, "Waiting matches (page 2)");

        fixture.click(NEXT_PAGE_SLOT);

        assertWaitingMatchesOpened(1, "Waiting matches (page 2)");

        fixture.click(PREVIOUS_PAGE_SLOT);

        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void returnsToTheMatchMenu() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.MATCH, menu.holder().getMenuType());
        assertEquals("Match and tournament", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        startATournamentOfFourTeams();
        openTheWaitingMatches();
        AdminMenuHolder waitingMatchesHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(FIRST_MATCH_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(waitingMatchesHolder, fixture.lastMenu().holder());
    }

    private void startATournamentOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
    }

    private void startATournamentForTwoPages() {
        fixture.configureArena();
        fixture.registerNumberedTeams(TEAM_COUNT_FOR_TWO_PAGES);
        fixture.startTournament();
    }

    private void openTheWaitingMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
        assertEquals(AdminMenuType.WAITING_MATCHES, fixture.lastMenu().holder().getMenuType());
    }

    private void assertWaitingMatchesOpened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.WAITING_MATCHES, menu.holder().getMenuType());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertEquals(54, menu.size());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder waitingMatchesHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(waitingMatchesHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }
}
