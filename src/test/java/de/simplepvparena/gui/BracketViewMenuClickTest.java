package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketViewMenuClickTest {

    private static final int PREVIOUS_ROUND_SLOT = 45;
    private static final int BACK_SLOT = 48;
    private static final int NEXT_ROUND_SLOT = 53;
    private static final int MENU_SIZE = 54;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void opensTheBracketViewThatIsRegisteredForItsType() {
        fixture.openBracketView();

        assertEquals(AdminMenuType.BRACKET_VIEW, fixture.lastMenu().holder().getMenuType());
        assertEquals(MENU_SIZE, fixture.lastMenu().size());
        assertFalse(fixture.lastMenu().holder().isOpenedFromMainMenu());
    }

    @Test
    void turnsToTheNextAndBackToThePreviousRound() {
        startATournamentOfEightTeams();
        fixture.openBracketView();

        fixture.click(NEXT_ROUND_SLOT);
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
        assertEquals("Bracket (round 2)", fixture.lastMenu().title());
        fixture.click(NEXT_ROUND_SLOT);
        assertEquals(2, fixture.lastMenu().holder().getPageIndex());
        fixture.click(PREVIOUS_ROUND_SLOT);

        assertEquals(1, fixture.lastMenu().holder().getPageIndex());
        assertEquals("Bracket (round 2)", fixture.lastMenu().title());
        assertFalse(fixture.isMenuClosed());
        assertEquals(List.of(), fixture.messages());
    }

    @Test
    void turnsNoFurtherThanTheFirstAndTheLastRound() {
        startATournamentOfEightTeams();
        fixture.openBracketView();
        AdminMenuHolder firstRound = fixture.lastMenu().holder();

        fixture.click(PREVIOUS_ROUND_SLOT);
        assertSame(firstRound, fixture.lastMenu().holder());
        fixture.click(NEXT_ROUND_SLOT);
        fixture.click(NEXT_ROUND_SLOT);
        AdminMenuHolder lastRound = fixture.lastMenu().holder();
        fixture.click(NEXT_ROUND_SLOT);

        assertSame(lastRound, fixture.lastMenu().holder());
        assertEquals(2, lastRound.getPageIndex());
    }

    @Test
    void ignoresAClickOnAMatchTileTheInfoTileAndEveryEmptySlot() {
        startATournamentOfEightTeams();
        fixture.openBracketView();
        fixture.click(NEXT_ROUND_SLOT);
        AdminMenuHolder secondRound = fixture.lastMenu().holder();

        for (int slot = 0; slot < MENU_SIZE; slot++) {
            if (Set.of(PREVIOUS_ROUND_SLOT, NEXT_ROUND_SLOT).contains(slot)) {
                continue;
            }
            fixture.click(slot);
            assertSame(secondRound, fixture.lastMenu().holder(), "slot " + slot);
        }

        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertEquals(List.of(), fixture.messages());
    }

    @Test
    void readsTheBracketAgainWhenTheRoundChangesButNotWhileItStaysOpen() {
        startATournamentOfEightTeams();
        fixture.openBracketView();
        String winner = fixture.runningFirstTeamName();
        String loser = fixture.runningSecondTeamName();

        fixture.decideRunningMatchFor(winner);

        assertEquals(List.of(winner + " against " + loser, "Running now"),
                fixture.lastMenu().findTile(0).orElseThrow().loreLines());
        assertEquals(0, fixture.lastMenu().refreshCount());
        fixture.click(NEXT_ROUND_SLOT);
        fixture.click(PREVIOUS_ROUND_SLOT);
        assertEquals(List.of(winner + " against " + loser, "Won by " + winner),
                fixture.lastMenu().findTile(0).orElseThrow().loreLines());
    }

    @Test
    void offersNoBackTileWhenTheMenuWasNotOpenedFromTheMainMenu() {
        fixture.openBracketView();

        assertFalse(fixture.lastMenu().findTile(BACK_SLOT).isPresent());
        fixture.click(BACK_SLOT);
        assertEquals(AdminMenuType.BRACKET_VIEW, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void returnsToTheMainMenuWhenItWasOpenedFromThere() {
        startATournamentOfEightTeams();
        fixture.clickIn(AdminMenuHolder.bracketView(0, true), NEXT_ROUND_SLOT);

        assertTrue(fixture.lastMenu().holder().isOpenedFromMainMenu());
        assertTrue(fixture.lastMenu().findTile(BACK_SLOT).isPresent());
        fixture.click(BACK_SLOT);

        assertEquals(AdminMenuType.MAIN, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void limitsARoundBeyondTheLastOneToTheLastRound() {
        startATournamentOfEightTeams();
        RecordingAdminMenuWindows windows = new RecordingAdminMenuWindows();
        AdminMenuNavigator navigator = new AdminMenuNavigator(windows);
        navigator.register(AdminMenuType.BRACKET_VIEW, new BracketViewMenu(navigator, fixture.tournamentManager(),
                fixture.matchService()));

        navigator.open(fixture.admin(), AdminMenuHolder.bracketView(9, false));

        RecordingAdminMenuWindows.RecordedMenu menu = windows.lastOpenedMenu(fixture.admin());
        assertEquals("Bracket (round 3)", menu.title());
        assertEquals("round3-match1", menu.findTile(0).orElseThrow().displayName());
        assertFalse(menu.findTile(NEXT_ROUND_SLOT).isPresent());
    }

    private void startATournamentOfEightTeams() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel");
        fixture.startTournament();
    }
}
