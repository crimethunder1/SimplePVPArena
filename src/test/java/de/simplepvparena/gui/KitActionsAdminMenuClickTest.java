package de.simplepvparena.gui;

import de.simplepvparena.support.EncodableItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitActionsAdminMenuClickTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_FIRST_KIT_SLOT = 0;
    private static final int KITS_SECOND_KIT_SLOT = 1;
    private static final int KITS_NEXT_PAGE_SLOT = 53;

    private static final int EMPTY_SLOT = 0;
    private static final int INFO_SLOT = 4;
    private static final int DEFAULT_SLOT = 11;
    private static final int REPLACE_SLOT = 13;
    private static final int DELETE_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void leavesTheMenuAsItIsWhenTheInfoTileOrAnEmptySlotIsClicked() {
        openTheOptionsOfTheSecondOfTwoKits();

        assertNothingHappensOnAClickOn(INFO_SLOT);
        assertNothingHappensOnAClickOn(EMPTY_SLOT);
    }

    @Test
    void usesTheKitAsTheDefaultKitAndShowsItAtOnce() {
        openTheOptionsOfTheSecondOfTwoKits();
        AdminMenuHolder optionsHolder = fixture.lastMenu().holder();

        fixture.click(DEFAULT_SLOT);

        assertEquals(List.of("Kit Knight is the default kit now, every fighter is given it from the next round on"),
                fixture.messages());
        assertEquals(Optional.of("Knight"), fixture.kitLibrary().getDefaultKitName());
        assertEquals(Optional.of("Knight"), fixture.kitStorage().lastSavedLibrary().defaultKitName());
        assertSame(optionsHolder, fixture.lastMenu().holder());
        assertEquals(1, fixture.lastMenu().refreshCount());
        assertEquals("This is the default kit", fixture.lastMenu().findTile(DEFAULT_SLOT).orElseThrow().displayName());
    }

    @Test
    void reportsWhyTheGreyDefaultTileOfTheDefaultKitChangesNothing() {
        openTheOptionsOfTheFirstOfTwoKits();
        int savedLibraryCount = fixture.kitStorage().savedLibraries().size();

        fixture.click(DEFAULT_SLOT);

        assertEquals(List.of("Kit Archer is the default kit already"), fixture.messages());
        assertEquals(savedLibraryCount, fixture.kitStorage().savedLibraries().size());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertEquals(AdminMenuType.KIT_ACTIONS, fixture.lastMenu().holder().getMenuType());
    }

    @Test
    void asksForTheConfirmationBeforeTheKitIsReplacedWithAFilledInventory() {
        openTheOptionsOfTheSecondOfTwoKits();
        fixture.placeInAdminInventory(0, new EncodableItemStack("axe"));

        fixture.click(REPLACE_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.CONFIRMATION, menu.holder().getMenuType());
        assertEquals(Optional.of(ConfirmationRequest.REPLACE_KIT), menu.holder().getConfirmationRequest());
        assertEquals(Optional.of("Knight"), menu.holder().getSubject());
        assertEquals(0, menu.holder().getPageIndex());
        assertEquals("Replace the kit with your inventory?", menu.title());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(3, fixture.kitLibrary().findKit("Knight").orElseThrow().inventoryItemCount(),
                "the kit stays as it is until the confirmation");
    }

    @Test
    void refusesTheReplacementWithAnEmptyInventoryAlreadyOnTheClick() {
        openTheOptionsOfTheSecondOfTwoKits();
        AdminMenuHolder optionsHolder = fixture.lastMenu().holder();

        fixture.click(REPLACE_SLOT);

        assertEquals(List.of("Your inventory is empty, the kit was not changed"), fixture.messages());
        assertSame(optionsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(2, fixture.kitStorage().savedLibraries().size());
    }

    @Test
    void asksForTheConfirmationBeforeAKitThatIsNeitherTheDefaultNorTheLastKitIsDeleted() {
        openTheOptionsOfTheSecondOfTwoKits();

        fixture.click(DELETE_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(Optional.of(ConfirmationRequest.DELETE_KIT), menu.holder().getConfirmationRequest());
        assertEquals(Optional.of("Knight"), menu.holder().getSubject());
        assertEquals("Delete the kit?", menu.title());
        assertEquals(List.of("Archer", "Knight"), fixture.kitLibrary().getKitNames());
    }

    @Test
    void reportsWhyTheGreyDeleteTileOfTheDefaultKitChangesNothing() {
        openTheOptionsOfTheFirstOfTwoKits();
        AdminMenuHolder optionsHolder = fixture.lastMenu().holder();

        fixture.click(DELETE_SLOT);

        assertEquals(List.of("Kit Archer is the default kit and cannot be deleted"), fixture.messages());
        assertSame(optionsHolder, fixture.lastMenu().holder());
        assertEquals(List.of("Archer", "Knight"), fixture.kitLibrary().getKitNames());
    }

    @Test
    void reportsWhyTheGreyDeleteTileOfTheLastKitChangesNothing() {
        fixture.createKit("Archer");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);
        AdminMenuHolder optionsHolder = fixture.lastMenu().holder();

        fixture.click(DELETE_SLOT);

        assertEquals(List.of("Kit Archer is the last kit and cannot be deleted"), fixture.messages());
        assertSame(optionsHolder, fixture.lastMenu().holder());
        assertEquals(List.of("Archer"), fixture.kitLibrary().getKitNames());
    }

    @Test
    void reportsAKitThatWasDeletedMeanwhileOnEveryAction() {
        openTheOptionsOfTheSecondOfTwoKits();
        fixture.kitLibrary().deleteKit("Knight");
        fixture.placeInAdminInventory(0, new EncodableItemStack("axe"));

        fixture.click(DEFAULT_SLOT);
        fixture.click(REPLACE_SLOT);
        fixture.click(DELETE_SLOT);

        assertEquals(List.of("Unknown kit Knight", "Unknown kit Knight", "Unknown kit Knight"), fixture.messages());
        assertEquals(AdminMenuType.KIT_ACTIONS, fixture.lastMenu().holder().getMenuType());
        assertEquals(List.of("Archer"), fixture.kitLibrary().getKitNames());
    }

    @Test
    void returnsToTheKitList() {
        openTheOptionsOfTheSecondOfTwoKits();

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.KIT, menu.holder().getMenuType());
        assertEquals("Kits (page 1)", menu.title());
        assertEquals(0, menu.holder().getPageIndex());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }

    @Test
    void returnsToTheSecondPageOfTheKitListItCameFrom() {
        fixture.createNumberedKits(46);
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_NEXT_PAGE_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);

        fixture.click(BACK_SLOT);

        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.KIT, menu.holder().getMenuType());
        assertEquals("Kits (page 2)", menu.title());
        assertEquals(1, menu.holder().getPageIndex());
        assertEquals("Kit46", menu.findTile(KITS_FIRST_KIT_SLOT).orElseThrow().displayName());
    }

    @Test
    void refusesTheClickOfAPlayerWithoutThePermissionAndClosesTheMenu() {
        openTheOptionsOfTheSecondOfTwoKits();
        AdminMenuHolder optionsHolder = fixture.lastMenu().holder();
        fixture.revokeAdminPermission();

        fixture.click(DEFAULT_SLOT);

        assertEquals(List.of("You are not allowed to use the tournament menu"), fixture.messages());
        assertTrue(fixture.isMenuClosed());
        assertSame(optionsHolder, fixture.lastMenu().holder());
        assertEquals(Optional.of("Archer"), fixture.kitLibrary().getDefaultKitName());
    }

    private void openTheOptionsOfTheFirstOfTwoKits() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_FIRST_KIT_SLOT);
    }

    private void openTheOptionsOfTheSecondOfTwoKits() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_SECOND_KIT_SLOT);
    }

    private void assertNothingHappensOnAClickOn(int slot) {
        AdminMenuHolder optionsHolder = fixture.lastMenu().holder();

        fixture.click(slot);

        assertSame(optionsHolder, fixture.lastMenu().holder());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.lastMenu().refreshCount());
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
    }
}
