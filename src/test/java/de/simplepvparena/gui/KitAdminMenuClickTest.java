package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitAdminMenuClickTest {

    private static final int MAIN_KIT_SLOT = 24;

    private static final int FIRST_KIT_SLOT = 0;
    private static final int SECOND_KIT_SLOT = 1;
    private static final int PREVIOUS_PAGE_SLOT = 45;
    private static final int INFO_SLOT = 47;
    private static final int BACK_SLOT = 49;
    private static final int CREATE_SLOT = 51;
    private static final int NEXT_PAGE_SLOT = 53;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        fixture.createKit("Archer");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenASlotWithoutAKitIsClicked() {
        fixture.createKit("Archer");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertNothingHappensOnAClickOn(SECOND_KIT_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenTheMissingPageArrowsAreClicked() {
        fixture.createKit("Archer");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertNothingHappensOnAClickOn(PREVIOUS_PAGE_SLOT);
        assertNothingHappensOnAClickOn(NEXT_PAGE_SLOT);
    }

    @Test
    void opensTheOptionsOfTheClickedKit() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(SECOND_KIT_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.KIT_ACTIONS, menu.holder().getMenuType());
        assertEquals("Kit Knight", menu.title());
        assertEquals(Optional.of("Knight"), menu.holder().getSubject());
        assertEquals(0, menu.holder().getPageIndex());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void opensTheOptionsOfAKitOnTheSecondPageWithThatPage() {
        fixture.createNumberedKits(46);
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(NEXT_PAGE_SLOT);

        fixture.click(FIRST_KIT_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.KIT_ACTIONS, menu.holder().getMenuType());
        assertEquals("Kit Kit46", menu.title());
        assertEquals(1, menu.holder().getPageIndex());
    }

    @Test
    void turnsThePagesForwardAndBack() {
        fixture.createNumberedKits(46);
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(NEXT_PAGE_SLOT);

        assertEquals("Kits (page 2)", fixture.lastMenu().title());
        assertEquals(1, fixture.lastMenu().holder().getPageIndex());

        fixture.click(PREVIOUS_PAGE_SLOT);

        assertEquals("Kits (page 1)", fixture.lastMenu().title());
        assertEquals(0, fixture.lastMenu().holder().getPageIndex());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void closesTheMenuAndAsksForTheNameOfTheNewKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(CREATE_SLOT);

        assertTrue(fixture.isMenuClosed());
        assertEquals(1, fixture.pendingChatInputCount());
        assertEquals(List.of(
                "Type the name of the new kit in chat",
                "A kit name has 1 to 32 letters, digits, hyphens or underscores",
                "Your inventory as it is then becomes the kit",
                "Type cancel to abort, the request expires in 60 seconds"), fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
    }

    @Test
    void returnsToTheMainMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.MAIN, menu.holder().getMenuType());
        assertEquals("Tournament administration", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        AdminMenuHolder kitHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(CREATE_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(kitHolder, fixture.lastMenu().holder());
        assertEquals(0, fixture.pendingChatInputCount());
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder kitHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(kitHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }
}
