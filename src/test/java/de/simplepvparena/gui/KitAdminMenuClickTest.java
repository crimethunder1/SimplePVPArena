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

    private static final int INFO_SLOT = 4;
    private static final int EMPTY_SLOT = 0;
    private static final int SAVE_SLOT = 13;
    private static final int BACK_SLOT = 22;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileIsClicked() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertNothingHappensOnAClickOn(INFO_SLOT);
    }

    @Test
    void leavesTheMenuAsItIsWhenAnEmptySlotIsClicked() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void asksForTheConfirmationBeforeTheInventoryIsSavedAsTheKit() {
        fixture.fillKit();
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(SAVE_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.SAVE_KIT), menu.holder().getConfirmationRequest());
        assertTrue(menu.holder().getSubject().isEmpty());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals("Save your inventory as the kit?", menu.title());
        assertFalse(fixture.isMenuClosed());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(List.of("Worn items: 2 of 5", "Inventory items: 3 of 36 slots"), fixture.describeKit(),
                "the kit stays as it is until the confirmation");
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

        fixture.click(SAVE_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(kitHolder, fixture.lastMenu().holder());
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
