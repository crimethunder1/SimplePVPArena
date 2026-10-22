package de.simplepvparena.gui;

import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.TournamentState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecidedMatchesAdminMenuClickTest {

    private static final int MAIN_MATCH_SLOT = 12;
    private static final int MATCH_REVOKE_RESULT_SLOT = 20;

    private static final int FIRST_MATCH_SLOT = 0;
    private static final int EMPTY_SLOT = 1;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_NO_SLOT = 15;

    private static final int TEAM_COUNT_FOR_TWO_PAGES = 128;
    private static final int LAST_DECIDED_MATCH_NUMBER_FOR_TWO_PAGES = 48;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void opensTheDecidedMatchesFromTheMatchMenuWhileTheTournamentRuns() {
        startATournamentOfFourTeams();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_REVOKE_RESULT_SLOT);

        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void opensTheDecidedMatchesFromTheMatchMenuWhileTheTournamentIsPaused() {
        startATournamentOfFourTeams();
        fixture.pauseTournament();
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);

        fixture.click(MATCH_REVOKE_RESULT_SLOT);

        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void stillTriggersTheUnavailableTileWithoutATournamentAndReportsTheReason() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        AdminMenuHolder matchHolder = fixture.lastMenu().holder();
        assertEquals(MenuItems.UNAVAILABLE_MATERIAL,
                fixture.lastMenu().findTile(MATCH_REVOKE_RESULT_SLOT).orElseThrow().material());

        fixture.click(MATCH_REVOKE_RESULT_SLOT);

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

        fixture.click(MATCH_REVOKE_RESULT_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertSame(matchHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
    }

    @Test
    void asksBeforeTheResultOfTheClickedMatchIsTakenBack() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();

        fixture.click(FIRST_MATCH_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.REVOKE_RESULT), menu.holder().getConfirmationRequest());
        assertEquals(Optional.of("round1-match2"), menu.holder().getSubject());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals("Take back the result of round1-match2?", menu.title());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertTrue(bracketMatch("round1-match2").isDecided(), "nothing is taken back by asking");
    }

    @Test
    void asksWithThePageOfAMatchOnTheSecondPage() {
        startATournamentForTwoPages();
        openTheDecidedMatches();
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(FIRST_MATCH_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of("round1-match47"), menu.holder().getSubject());
        assertEquals(1, menu.holder().getPageIndex());
        assertEquals("Take back the result of round1-match47?", menu.title());
    }

    @Test
    void takesTheResultBackOnYesAndReturnsToTheDecidedMatches() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();
        fixture.click(FIRST_MATCH_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match2 is open again"), fixture.messages());
        assertFalse(bracketMatch("round1-match2").isDecided());
        assertTrue(bracketMatch("round1-match2").isPlayable());
        assertEquals(Optional.empty(), bracketMatch("round2-match1").getSecondTeamName());
        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
        assertEquals("""
                Decided matches (page 1) | 54
                47 | BOOK | Decided matches | No result can be taken back
                49 | OAK_DOOR | Back | Returns to the match menu
                """, fixture.describeLastMenu());
    }

    @Test
    void returnsToTheSecondPageAfterTheResultOfAMatchOnItWasTakenBack() {
        startATournamentForTwoPages();
        openTheDecidedMatches();
        fixture.click(NEXT_PAGE_SLOT);
        fixture.click(FIRST_MATCH_SLOT);

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("Match round1-match47 is open again"), fixture.messages());
        assertFalse(bracketMatch("round1-match47").isDecided());
        assertDecidedMatchesOpened(1, "Decided matches (page 2)");
    }

    @Test
    void changesNothingOnNoAndReturnsToTheDecidedMatches() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        String winnerTeamName = firstTeamOf("round1-match2");
        openTheDecidedMatches();
        fixture.click(FIRST_MATCH_SLOT);

        fixture.click(CONFIRM_NO_SLOT);

        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(Optional.of(winnerTeamName), bracketMatch("round1-match2").getWinnerTeamName());
        assertEquals(Optional.of(winnerTeamName), bracketMatch("round2-match1").getSecondTeamName());
        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
    }

    @Test
    void checksAgainOnYesAndReportsThatTheFollowingMatchStartedMeanwhile() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        String winnerTeamName = fixture.runningFirstTeamName();
        fixture.decideRunningMatchFor(winnerTeamName);
        openTheDecidedMatches();
        fixture.click(FIRST_MATCH_SLOT);
        assertEquals(Optional.of("round1-match1"), fixture.lastMenu().holder().getSubject());
        fixture.startFinal();

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("The following match round2-match1 is running"), fixture.messages());
        assertEquals(Optional.of(winnerTeamName), bracketMatch("round1-match1").getWinnerTeamName());
        assertEquals("round2-match1", fixture.runningMatch().getMatchId());
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
    }

    @Test
    void checksAgainOnYesAndReportsThatTheTournamentWasCancelledMeanwhile() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();
        fixture.click(FIRST_MATCH_SLOT);
        assertTrue(fixture.tournamentManager().cancelTournament().successful());

        fixture.click(CONFIRM_YES_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenTheFirstSlotIsClickedWithoutADecidedMatch() {
        startATournamentOfFourTeams();
        openTheDecidedMatches();

        assertNothingHappensOnAClickOn(FIRST_MATCH_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenThePageSlotsAreClickedOnTheOnlyPage() {
        startATournamentOfFourTeams();
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();

        assertNothingHappensOnAClickOn(PREVIOUS_PAGE_SLOT);
        assertNothingHappensOnAClickOn(NEXT_PAGE_SLOT);
    }

    @Test
    void turnsToTheNextPageAndBack() {
        startATournamentForTwoPages();
        openTheDecidedMatches();

        fixture.click(NEXT_PAGE_SLOT);

        assertDecidedMatchesOpened(1, "Decided matches (page 2)");

        fixture.click(NEXT_PAGE_SLOT);

        assertDecidedMatchesOpened(1, "Decided matches (page 2)");

        fixture.click(PREVIOUS_PAGE_SLOT);

        assertDecidedMatchesOpened(0, "Decided matches (page 1)");
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void returnsToTheMatchMenu() {
        startATournamentOfFourTeams();
        openTheDecidedMatches();

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
        decideForTheFirstTeam("round1-match2");
        openTheDecidedMatches();
        AdminMenuHolder decidedMatchesHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(FIRST_MATCH_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(decidedMatchesHolder, fixture.lastMenu().holder());
        assertTrue(bracketMatch("round1-match2").isDecided());
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
        for (int matchNumber = 2; matchNumber <= LAST_DECIDED_MATCH_NUMBER_FOR_TWO_PAGES; matchNumber++) {
            decideForTheFirstTeam("round1-match" + matchNumber);
        }
    }

    private void openTheDecidedMatches() {
        fixture.openMenuFromTheMainMenu(MAIN_MATCH_SLOT);
        fixture.click(MATCH_REVOKE_RESULT_SLOT);
        assertEquals(AdminMenuType.DECIDED_MATCHES, fixture.lastMenu().holder().getMenuType());
    }

    private void assertDecidedMatchesOpened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.DECIDED_MATCHES, menu.holder().getMenuType());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertEquals(54, menu.size());
        assertFalse(fixture.isMenuClosed());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder decidedMatchesHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(decidedMatchesHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
    }

    private void decideForTheFirstTeam(String matchId) {
        assertTrue(fixture.tournamentManager().forceMatchResult(matchId, firstTeamOf(matchId)).successful());
    }

    private BracketMatch bracketMatch(String matchId) {
        return fixture.tournamentManager().getBracket().orElseThrow().findMatch(matchId).orElseThrow();
    }

    private String firstTeamOf(String matchId) {
        return bracketMatch(matchId).getFirstTeamName().orElseThrow();
    }
}
