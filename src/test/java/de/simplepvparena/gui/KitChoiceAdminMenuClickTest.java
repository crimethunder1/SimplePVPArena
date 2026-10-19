package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitChoiceAdminMenuClickTest {

    private static final int MAIN_KIT_CHOICE_SLOT = 23;

    private static final int FIRST_KIT_SLOT = 0;
    private static final int SECOND_KIT_SLOT = 1;
    private static final int THIRD_KIT_SLOT = 2;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 53;

    private static final String OVER_WARNING = "The kit choice for match round1-match1 is over";

    private final AdminMenuFixture fixture = AdminMenuFixture.withKitChoice();

    private int matchStartCount;

    @Test
    void choosesTheClickedKitForThisMatchAndClosesThePopup() {
        createArcherAndKnight();
        beginTheChoice();

        fixture.click(SECOND_KIT_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, matchStartCount);
        assertFalse(fixture.kitChoiceService().isPending());
        assertSame(fixture.kitLibrary().findKit("Knight").orElseThrow().kitDefinition(),
                fixture.activeKitDefinition());
        assertEquals(Optional.of("Archer"), fixture.kitLibrary().getDefaultKitName());
        assertEquals(List.of("admin chose the kit Knight for match round1-match1"), fixture.messages());
    }

    @Test
    void startsTheMatchOfTheTournamentWithTheKitThatWasClicked() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo", "Charlie", "Delta");
        createArcherAndKnight();
        fixture.startTournament();
        assertEquals(AdminMenuType.KIT_CHOICE, fixture.lastMenu().holder().getMenuType());
        assertFalse(fixture.matchService().hasActiveMatch());

        fixture.click(SECOND_KIT_SLOT);

        assertEquals("round1-match1", fixture.runningMatch().getMatchId());
        assertSame(fixture.kitLibrary().findKit("Knight").orElseThrow().kitDefinition(),
                fixture.activeKitDefinition());
        assertFalse(fixture.tournamentManager().isKitChoicePending());
    }

    @Test
    void warnsAndClosesThePopupWhenTheChoiceIsOverAtTheClick() {
        createArcherAndKnight();
        beginTheChoice();
        fixture.kitChoiceScheduler().advanceSeconds(30);
        fixture.messages().clear();

        fixture.click(SECOND_KIT_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of(OVER_WARNING), fixture.messages());
        assertEquals(1, matchStartCount);
        assertSame(fixture.kitLibrary().findKit("Archer").orElseThrow().kitDefinition(),
                fixture.activeKitDefinition());
    }

    @Test
    void choosesNothingForTheNextMatchFromThePopupOfAFormerMatch() {
        createArcherAndKnight();
        beginTheChoice();
        AdminMenuHolder formerPopup = fixture.lastMenu().holder();
        fixture.kitChoiceScheduler().advanceSeconds(30);
        fixture.kitChoiceService().begin("round1-match2", "Charlie", "Delta", () -> matchStartCount++);
        fixture.messages().clear();

        fixture.clickIn(formerPopup, SECOND_KIT_SLOT);

        assertEquals(List.of(OVER_WARNING), fixture.messages());
        assertEquals("round1-match2", fixture.kitChoiceService().getPending().orElseThrow().matchId());
        assertEquals(1, matchStartCount);
    }

    @Test
    void leavesThePopupAsItIsWhenTheInfoTileOrASlotWithoutAKitIsClicked() {
        createArcherAndKnight();
        beginTheChoice();
        RecordingAdminMenuWindows.RecordedMenu popup = fixture.lastMenu();

        fixture.click(INFO_SLOT);
        fixture.click(THIRD_KIT_SLOT);
        fixture.click(PREVIOUS_PAGE_SLOT);
        fixture.click(NEXT_PAGE_SLOT);

        assertSame(popup, fixture.lastMenu());
        assertFalse(fixture.isMenuClosed());
        assertEquals(List.of(), fixture.messages());
        assertTrue(fixture.kitChoiceService().isPending());
        assertEquals(0, matchStartCount);
    }

    @Test
    void turnsThePagesAndChoosesAKitOnTheSecondPage() {
        fixture.createNumberedKits(46);
        beginTheChoice();

        fixture.click(NEXT_PAGE_SLOT);

        assertEquals(AdminMenuType.KIT_CHOICE, fixture.lastMenu().holder().getMenuType());
        assertEquals(Optional.of("round1-match1"), fixture.lastMenu().holder().getSubject());
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());

        fixture.click(PREVIOUS_PAGE_SLOT);

        assertEquals(0, fixture.lastMenu().holder().getPageIndex());

        fixture.click(NEXT_PAGE_SLOT);
        fixture.click(FIRST_KIT_SLOT);

        assertEquals(1, matchStartCount);
        assertSame(fixture.kitLibrary().findKit("Kit46").orElseThrow().kitDefinition(),
                fixture.activeKitDefinition());
    }

    @Test
    void refusesTheChoiceOfAPlayerWhoLostTheAdminPermission() {
        createArcherAndKnight();
        beginTheChoice();
        fixture.revokeAdminPermission();

        fixture.click(SECOND_KIT_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.kitChoiceService().isPending());
        assertEquals(0, matchStartCount);
    }

    @Test
    void opensThePopupAgainFromTheTileInTheMainMenu() {
        createArcherAndKnight();
        beginTheChoice();
        fixture.openMainMenu();

        fixture.click(MAIN_KIT_CHOICE_SLOT);

        assertEquals(AdminMenuType.KIT_CHOICE, fixture.lastMenu().holder().getMenuType());
        assertEquals(Optional.of("round1-match1"), fixture.lastMenu().holder().getSubject());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());

        fixture.click(FIRST_KIT_SLOT);

        assertEquals(1, matchStartCount);
    }

    @Test
    void doesNothingWhenTheKitChoiceTileVanishedBetweenShowingAndClicking() {
        createArcherAndKnight();
        beginTheChoice();
        fixture.openMainMenu();
        RecordingAdminMenuWindows.RecordedMenu mainMenu = fixture.lastMenu();
        fixture.kitChoiceScheduler().advanceSeconds(30);
        fixture.messages().clear();

        fixture.click(MAIN_KIT_CHOICE_SLOT);

        assertSame(mainMenu, fixture.lastMenu());
        assertFalse(fixture.isMenuClosed());
        assertEquals(List.of(), fixture.messages());
    }

    @Test
    void doesNothingOnTheFreeMainMenuSlotWithoutAKitChoice() {
        createArcherAndKnight();
        fixture.openMainMenu();
        RecordingAdminMenuWindows.RecordedMenu mainMenu = fixture.lastMenu();

        fixture.click(MAIN_KIT_CHOICE_SLOT);

        assertSame(mainMenu, fixture.lastMenu());
        assertFalse(fixture.isMenuClosed());
        assertEquals(List.of(), fixture.messages());
    }

    private void createArcherAndKnight() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
    }

    private void beginTheChoice() {
        fixture.kitChoiceService().begin("round1-match1", "Alpha", "Bravo", () -> matchStartCount++);
    }
}
