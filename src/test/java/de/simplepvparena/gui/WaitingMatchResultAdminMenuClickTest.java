package de.simplepvparena.gui;

import de.simplepvparena.tournament.BracketMatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitingMatchResultAdminMenuClickTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_DECIDE_WAITING_MATCH_SLOT = 14;
    private static final int WAITING_MATCHES_NEXT_PAGE_SLOT = 53;
    private static final int FIRST_WAITING_MATCH_SLOT = 0;
    private static final int LAST_WAITING_MATCH_SLOT_ON_THE_SECOND_PAGE = 17;
    private static final int WAITING_MATCHES_INFO_SLOT = 47;

    private static final int MENU_SIZE = 27;
    private static final int EMPTY_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int FIRST_TEAM_SLOT = 11;
    private static final int SECOND_TEAM_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private static final int TEAM_COUNT_FOR_TWO_PAGES = 128;

    private static final String MATCH_ID = "round1-match2";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void decidesTheWaitingMatchForTheFirstTeamAndReturnsToTheWaitingMatches() {
        openTheResultOfTheWaitingMatchOfFourTeams();
        String firstTeamName = bracketMatch(MATCH_ID).getFirstTeamName().orElseThrow();

        fixture.click(FIRST_TEAM_SLOT);

        assertDecidedFor(firstTeamName);
    }

    @Test
    void decidesTheWaitingMatchForTheSecondTeamAndReturnsToTheWaitingMatches() {
        openTheResultOfTheWaitingMatchOfFourTeams();
        String secondTeamName = bracketMatch(MATCH_ID).getSecondTeamName().orElseThrow();

        fixture.click(SECOND_TEAM_SLOT);

        assertDecidedFor(secondTeamName);
    }

    @Test
    void decidesAMatchWhoseTeamsAreBothGoneAndTheTournamentCarriesOn() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
        String firstTeamName = bracketMatch(MATCH_ID).getFirstTeamName().orElseThrow();
        assertTrue(fixture.teamRegistry().removeTeam(firstTeamName));
        assertTrue(fixture.teamRegistry().removeTeam(bracketMatch(MATCH_ID).getSecondTeamName().orElseThrow()));
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        assertFalse(fixture.matchService().hasActiveMatch(), "the tournament waits for an admin decision");
        openTheResultOfTheFirstWaitingMatch();
        assertEquals(Optional.of(MATCH_ID), fixture.lastMenu().holder().getSubject());

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(List.of("Match " + MATCH_ID + " was decided for " + firstTeamName), fixture.messages());
        assertEquals(Optional.of(firstTeamName), bracketMatch(MATCH_ID).getWinnerTeamName());
        assertEquals("round1-match3", fixture.runningMatch().getMatchId(), "the next match starts by itself");
        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertEquals("round1-match4", fixture.lastMenu().findTile(FIRST_WAITING_MATCH_SLOT).orElseThrow()
                .displayName());
    }

    @Test
    void returnsToTheSamePageOfTheWaitingMatchesAfterADecision() {
        startATournamentForTwoPages();
        openTheWaitingMatchesOnTheSecondPage();
        fixture.click(FIRST_WAITING_MATCH_SLOT);
        assertEquals(Optional.of("round1-match47"), fixture.lastMenu().holder().getSubject());
        String firstTeamName = bracketMatch("round1-match47").getFirstTeamName().orElseThrow();

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(List.of("Match round1-match47 was decided for " + firstTeamName), fixture.messages());
        assertWaitingMatchesOpened(1, "Waiting matches (page 2)");
        assertEquals(62, fixture.tournamentManager().findWaitingMatches().size());
    }

    @Test
    void returnsToTheLastPageThatIsLeftAfterADecision() {
        startATournamentForTwoPages();
        openTheWaitingMatchesOnTheSecondPage();
        fixture.click(LAST_WAITING_MATCH_SLOT_ON_THE_SECOND_PAGE);
        assertEquals(Optional.of("round1-match64"), fixture.lastMenu().holder().getSubject());
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
        for (int matchNumber = 2; matchNumber <= 34; matchNumber += 2) {
            String matchId = "round1-match" + matchNumber;
            assertTrue(fixture.tournamentManager().forceMatchResult(matchId,
                    bracketMatch(matchId).getFirstTeamName().orElseThrow()).successful());
        }
        assertEquals(46, fixture.tournamentManager().findWaitingMatches().size());

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(45, fixture.tournamentManager().findWaitingMatches().size());
        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
    }

    @Test
    void returnsToTheWaitingMatchesWithoutDecidingTheMatch() {
        openTheResultOfTheWaitingMatchOfFourTeams();

        fixture.click(BACK_SLOT);

        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertFalse(bracketMatch(MATCH_ID).isDecided());
    }

    @Test
    void returnsToThePageOfTheWaitingMatchesItWasOpenedFrom() {
        startATournamentForTwoPages();
        openTheWaitingMatchesOnTheSecondPage();
        fixture.click(FIRST_WAITING_MATCH_SLOT);

        fixture.click(BACK_SLOT);

        assertWaitingMatchesOpened(1, "Waiting matches (page 2)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClickedWhileTheMatchWaits() {
        openTheResultOfTheWaitingMatchOfFourTeams();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClickedWhileTheMatchWaits() {
        openTheResultOfTheWaitingMatchOfFourTeams();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void warnsOnEverySlotButBackThatADecidedMatchIsNotWaitingAnyMoreAndReturnsToTheWaitingMatches() {
        openTheResultOfTheWaitingMatchOfFourTeams();
        AdminMenuHolder resultHolder = fixture.lastMenu().holder();
        String firstTeamName = bracketMatch(MATCH_ID).getFirstTeamName().orElseThrow();
        assertTrue(fixture.tournamentManager().forceMatchResult(MATCH_ID, firstTeamName).successful());

        for (int slot = 0; slot < MENU_SIZE; slot++) {
            if (slot == BACK_SLOT) {
                continue;
            }
            fixture.messages().clear();

            fixture.clickIn(resultHolder, slot);

            assertEquals(List.of("Match " + MATCH_ID + " is not waiting any more"), fixture.messages(),
                    "slot " + slot);
            assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
            assertEquals(Optional.of(firstTeamName), bracketMatch(MATCH_ID).getWinnerTeamName(), "slot " + slot);
        }
    }

    @Test
    void warnsThatAMatchThatRunsNowIsNotWaitingAnyMoreAndLeavesItRunning() {
        openTheResultOfTheWaitingMatchOfFourTeams();
        fixture.decideRunningMatchFor(fixture.runningFirstTeamName());
        assertEquals(MATCH_ID, fixture.runningMatch().getMatchId());
        fixture.messages().clear();

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(List.of("Match " + MATCH_ID + " is not waiting any more"), fixture.messages());
        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertEquals(MATCH_ID, fixture.runningMatch().getMatchId());
        assertFalse(bracketMatch(MATCH_ID).isDecided());
    }

    @Test
    void returnsToTheWaitingMatchesWithoutAWarningWhenTheMatchIsNotWaitingAnyMore() {
        openTheResultOfTheWaitingMatchOfFourTeams();
        assertTrue(fixture.tournamentManager().forceMatchResult(MATCH_ID,
                bracketMatch(MATCH_ID).getFirstTeamName().orElseThrow()).successful());
        fixture.messages().clear();

        fixture.click(BACK_SLOT);

        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheResultOfTheWaitingMatchOfFourTeams();
        AdminMenuHolder resultHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(FIRST_TEAM_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(resultHolder, fixture.lastMenu().holder());
        assertFalse(bracketMatch(MATCH_ID).isDecided());
    }

    private void openTheResultOfTheWaitingMatchOfFourTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        fixture.startTournament();
        openTheResultOfTheFirstWaitingMatch();
        assertEquals(Optional.of(MATCH_ID), fixture.lastMenu().holder().getSubject());
    }

    private void startATournamentForTwoPages() {
        fixture.configureArena();
        fixture.registerNumberedTeams(TEAM_COUNT_FOR_TWO_PAGES);
        fixture.startTournament();
    }

    private void openTheResultOfTheFirstWaitingMatch() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
        fixture.click(FIRST_WAITING_MATCH_SLOT);
        assertEquals(AdminMenuType.WAITING_MATCH_RESULT, fixture.lastMenu().holder().getMenuType());
    }

    private void openTheWaitingMatchesOnTheSecondPage() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_DECIDE_WAITING_MATCH_SLOT);
        fixture.click(WAITING_MATCHES_NEXT_PAGE_SLOT);
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
    }

    private BracketMatch bracketMatch(String matchId) {
        return fixture.tournamentManager().getBracket().orElseThrow().findMatch(matchId).orElseThrow();
    }

    private void assertDecidedFor(String winningTeamName) {
        assertEquals(List.of("Match " + MATCH_ID + " was decided for " + winningTeamName), fixture.messages());
        assertEquals(Optional.of(winningTeamName), bracketMatch(MATCH_ID).getWinnerTeamName());
        assertTrue(fixture.tournamentManager().describeBracket().contains("  " + MATCH_ID + " "
                + bracketMatch(MATCH_ID).getFirstTeamName().orElseThrow() + " vs "
                + bracketMatch(MATCH_ID).getSecondTeamName().orElseThrow() + " -> " + winningTeamName),
                fixture.tournamentManager().describeBracket().toString());
        assertEquals("round1-match1", fixture.runningMatch().getMatchId(), "the running match is left alone");
        assertWaitingMatchesOpened(0, "Waiting matches (page 1)");
        assertEquals(List.of("No match is waiting"),
                fixture.lastMenu().findTile(WAITING_MATCHES_INFO_SLOT).orElseThrow().loreLines());
    }

    private void assertWaitingMatchesOpened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.WAITING_MATCHES, menu.holder().getMenuType());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder resultHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(resultHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertFalse(bracketMatch(MATCH_ID).isDecided());
    }
}
