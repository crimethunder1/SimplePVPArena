package de.simplepvparena.gui;

import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.TournamentState;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainAdminMenuClickTest {

    private static final int INFO_SLOT = 4;
    private static final int EMPTY_SLOT = 0;
    private static final int ARENA_SLOT = 10;
    private static final int TEAMS_SLOT = 11;
    private static final int MATCH_SLOT = 12;
    private static final int BORDER_SELECTOR_SLOT = 13;
    private static final int FINAL_SLOT = 14;
    private static final int STATUS_SLOT = 15;
    private static final int BRACKET_SLOT = 16;
    private static final int DRY_RUN_SLOT = 18;
    private static final int RELOAD_SLOT = 20;
    private static final int CLOSE_SLOT = 22;
    private static final int KIT_SLOT = 24;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        openTheMainMenuOfAReadyEvent();

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        openTheMainMenuOfAReadyEvent();

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void opensTheArenaMenu() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(ARENA_SLOT);

        assertOpened(AdminMenuType.ARENA, "Arena");
    }

    @Test
    void opensTheFirstPageOfTheTeamsMenu() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(TEAMS_SLOT);

        assertOpened(AdminMenuType.TEAMS, "Teams (page 1)");
        assertEquals(54, fixture.lastMenu().size());
    }

    @Test
    void opensTheMatchMenu() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(MATCH_SLOT);

        assertOpened(AdminMenuType.MATCH, "Match and tournament");
    }

    @Test
    void opensTheKitMenu() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(KIT_SLOT);

        assertOpened(AdminMenuType.KIT, "Kit");
    }

    @Test
    void closesTheMenuAndSwitchesTheBorderSelectorModeOn() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(BORDER_SELECTOR_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertTrue(fixture.isBorderSelectorModeOn());
        assertEquals(List.of(lines(
                "Border selector mode is on",
                "Right click any block in the world to mark it as a border block, "
                        + "right click a marked one to take its mark away again",
                "No border item is needed: stairs, fences and everything else that already "
                        + "stands there can become part of the wall",
                "Every other right click is blocked while the mode is on, chests and doors included",
                "Open the tournament menu and click the tile again to switch it off")), fixture.messages());
    }

    @Test
    void closesTheMenuAndSwitchesTheBorderSelectorModeOffAgain() {
        fixture.switchBorderSelectorModeOn();
        openTheMainMenuOfAReadyEvent();

        fixture.click(BORDER_SELECTOR_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertFalse(fixture.isBorderSelectorModeOn());
        assertEquals(List.of(lines(
                "Border selector mode is off",
                "Right clicking works the way it always did again, every mark you set stays")), fixture.messages());
    }

    @Test
    void refusesTheUnavailableFinalTileWithoutRefreshingOrClosingTheMenu() {
        openTheMainMenuOfAReadyEvent();
        AdminMenuHolder mainHolder = fixture.lastMenu().holder();

        fixture.click(FINAL_SLOT);

        assertEquals(List.of("No tournament is in progress"), fixture.messages());
        assertSame(mainHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
    }

    @Test
    void startsTheReadyFinalWithoutRefreshingTheMenu() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.openMainMenu();
        AdminMenuHolder mainHolder = fixture.lastMenu().holder();

        fixture.click(FINAL_SLOT);

        assertEquals(List.of("The final has started"), fixture.messages());
        assertEquals(FinalMatchStage.RUNNING, fixture.tournamentManager().getFinalMatchStage());
        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
        assertSame(mainHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertEquals(Material.NETHER_STAR, fixture.lastMenu().findTile(FINAL_SLOT).orElseThrow().material(),
                "the tile still shows the final as ready");
    }

    @Test
    void writesTheStatusIntoTheChatAndKeepsTheMenuOpen() {
        openTheMainMenuOfAReadyEvent();
        AdminMenuHolder mainHolder = fixture.lastMenu().holder();

        fixture.click(STATUS_SLOT);

        assertEquals(List.of("Tournament state: IDLE"), fixture.messages());
        assertSame(mainHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
    }

    @Test
    void closesTheMenuAndWritesTheBracketIntoTheChat() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(BRACKET_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of("Bracket", "No tournament is in progress"), fixture.messages());
    }

    @Test
    void closesTheMenuAndReportsTheDryRunBeforeAnEvent() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(DRY_RUN_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of(
                "Dry run of the tournament pipeline: 3 of 7 steps passed, 3 failed, 1 skipped",
                "  [ok] Two test teams registered",
                "  [FAILED] Arena, fight area and spawns",
                "  [FAILED] The kit a round start hands out",
                "  [skipped] Start gate countdown",
                "  [ok] Bracket and a simulated match",
                "  [FAILED] The cleaning after the match",
                "  [ok] Test teams removed again"), stepLines(fixture.messages()));
        assertEquals(2, fixture.teamRegistry().getTeamCount(), "the test teams are removed again");
        assertEquals(TournamentState.IDLE, fixture.tournamentManager().getState());
    }

    @Test
    void closesTheMenuAndRefusesTheDryRunWhileATournamentIsInProgress() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startTournament();
        fixture.openMainMenu();

        fixture.click(DRY_RUN_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of(
                "The dry run was not started",
                "A tournament is in progress, the dry run is a check for before an event",
                "It registers two test teams and would put them into the registry of a running bracket",
                "Wait until the event is over or cancel it in the match menu of /tournament"), fixture.messages());
        assertEquals(TournamentState.RUNNING, fixture.tournamentManager().getState());
    }

    @Test
    void closesTheMenuAndReportsTheReloadedConfiguration() {
        openTheMainMenuOfAReadyEvent();

        fixture.click(RELOAD_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.reloadedConfigurations().size());
        assertEquals(fixture.reloadedConfigurations().get(0), fixture.messages());
    }

    @Test
    void warnsAfterTheReloadThatTheNewSettingsArriveWithTheNextRoundWhileAMatchRuns() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.openMainMenu();

        fixture.click(RELOAD_SLOT);

        assertTrue(fixture.isMenuClosed());
        List<String> expectedMessages = new ArrayList<>(fixture.reloadedConfigurations().get(0));
        expectedMessages.add(lines(
                "A match is running, the new settings arrive with the next round",
                "The running round keeps its timer and its fighters keep the kit they were given"));
        assertEquals(expectedMessages, fixture.messages());
    }

    @Test
    void closesTheMenuFromTheCloseTile() {
        openTheMainMenuOfAReadyEvent();
        AdminMenuHolder mainHolder = fixture.lastMenu().holder();

        fixture.click(CLOSE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertSame(mainHolder, fixture.lastMenu().holder());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheMainMenuOfAReadyEvent();
        AdminMenuHolder mainHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(BORDER_SELECTOR_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(mainHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isBorderSelectorModeOn());
    }

    private void openTheMainMenuOfAReadyEvent() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.openMainMenu();
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder mainHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(mainHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private void assertOpened(AdminMenuType menuType, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(menuType, menu.holder().getMenuType());
        assertEquals(title, menu.title());
        assertEquals(0, menu.holder().getPageIndex());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    private static List<String> stepLines(List<String> messages) {
        return messages.stream()
                .filter(message -> message.startsWith("Dry run of the tournament pipeline")
                        || message.startsWith("  ["))
                .toList();
    }

    private static String lines(String... lines) {
        return String.join("\n", lines);
    }
}
