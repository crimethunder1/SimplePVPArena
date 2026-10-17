package de.simplepvparena.gui;

import de.simplepvparena.support.EncodableItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitAdminMenuChatInputTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_CREATE_SLOT = 51;
    private static final int KITS_NEXT_PAGE_SLOT = 53;

    private static final String NOT_ALLOWED = "You are not allowed to use the tournament menu";

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void createsTheFirstKitAsTheDefaultKitOnlyOnTheNextTickAndReopensTheKitList() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        assertTrue(fixture.typeInChat(" Archer "));

        assertNothingDoneBeforeTheNextTick();
        assertEquals(0, fixture.kitLibrary().getKitCount());

        fixture.runNextTick();

        assertEquals(List.of("Kit Archer saved: 1 item, 0 worn and 1 in the inventory",
                "It is written into kits.yml and is handed out from the next round on, "
                        + "a running round keeps the kit its fighters were given"), fixture.messages());
        assertEquals(List.of("Archer"), fixture.kitLibrary().getKitNames());
        assertEquals(Optional.of("Archer"), fixture.kitLibrary().getDefaultKitName());
        assertEquals(1, fixture.kitStorage().savedLibraries().size());
        assertEquals(List.of("Worn items: 0 of 5", "Inventory items: 1 of 36 slots"), fixture.describeKit());
        assertKitListReopened(0, "Kits (page 1)");
        assertEquals("Archer", fixture.lastMenu().findTile(0).orElseThrow().displayName());
    }

    @Test
    void createsAFurtherKitThatIsNotHandedOut() {
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("axe"));
        askForTheNameOfTheNewKit();

        fixture.typeInChat("Knight");
        fixture.runNextTick();

        assertEquals(List.of("Kit Knight saved: 1 item, 0 worn and 1 in the inventory",
                "It is written into kits.yml and is handed out once it is used as the default kit"),
                fixture.messages());
        assertEquals(List.of("Archer", "Knight"), fixture.kitLibrary().getKitNames());
        assertEquals(Optional.of("Archer"), fixture.kitLibrary().getDefaultKitName());
        assertEquals(List.of("Worn items: 2 of 5", "Inventory items: 3 of 36 slots"), fixture.describeKit());
        assertKitListReopened(0, "Kits (page 1)");
    }

    @Test
    void createsAKitWhoseNameHoldsUmlauts() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("bow"));
        askForTheNameOfTheNewKit();

        fixture.typeInChat("Bogenschütze");
        fixture.runNextTick();

        assertEquals(List.of("Bogenschütze"), fixture.kitLibrary().getKitNames());
        assertEquals(Optional.of("Bogenschütze"), fixture.kitLibrary().getDefaultKitName());
        assertEquals("Kit Bogenschütze saved: 1 item, 0 worn and 1 in the inventory", fixture.messages().get(0));
        assertKitListReopened(0, "Kits (page 1)");
        assertEquals("Bogenschütze", fixture.lastMenu().findTile(0).orElseThrow().displayName());
    }

    @Test
    void reopensTheKitListOnThePageTheNameWasAskedFrom() {
        fixture.createNumberedKits(46);
        fixture.placeInAdminInventory(0, new EncodableItemStack("axe"));
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_NEXT_PAGE_SLOT);
        fixture.click(KITS_CREATE_SLOT);
        fixture.messages().clear();

        fixture.typeInChat("Knight");
        fixture.runNextTick();

        assertEquals(47, fixture.kitLibrary().getKitCount());
        assertKitListReopened(1, "Kits (page 2)");
        assertEquals("Knight", fixture.lastMenu().findTile(1).orElseThrow().displayName());
    }

    @Test
    void rejectsANameThatIsNotAllowedAndReopensTheKitList() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        fixture.typeInChat("my kit");
        fixture.runNextTick();

        assertEquals(List.of("The kit name my kit is not allowed, a kit name has 1 to 32 letters, digits, "
                + "hyphens or underscores"), fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
        assertEquals(List.of(), fixture.kitStorage().savedLibraries());
        assertKitListReopened(0, "Kits (page 1)");
    }

    @Test
    void rejectsANameThatIsTooLongAndReopensTheKitList() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        fixture.typeInChat("a".repeat(33));
        fixture.runNextTick();

        assertEquals(List.of("The kit name " + "a".repeat(33)
                + " is not allowed, a kit name has 1 to 32 letters, digits, hyphens or underscores"),
                fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
        assertKitListReopened(0, "Kits (page 1)");
    }

    @Test
    void rejectsANameThatExistsAlreadyInAnotherCaseAndReopensTheKitList() {
        fixture.createKit("Archer");
        fixture.placeInAdminInventory(0, new EncodableItemStack("axe"));
        askForTheNameOfTheNewKit();

        fixture.typeInChat("ARCHER");
        fixture.runNextTick();

        assertEquals(List.of("A kit named Archer exists already"), fixture.messages());
        assertEquals(List.of("Archer"), fixture.kitLibrary().getKitNames());
        assertEquals(3, fixture.kitLibrary().findKit("Archer").orElseThrow().inventoryItemCount());
        assertEquals(1, fixture.kitStorage().savedLibraries().size());
        assertKitListReopened(0, "Kits (page 1)");
    }

    @Test
    void rejectsAnEmptyInventoryWithoutCreatingAKitAndReopensTheKitList() {
        askForTheNameOfTheNewKit();

        fixture.typeInChat("Archer");
        fixture.runNextTick();

        assertEquals(List.of("Your inventory is empty, no kit was created"), fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
        assertEquals(List.of(), fixture.kitStorage().savedLibraries());
        assertTrue(fixture.describeKit().contains("The kit is empty, no fighter is given anything"));
        assertKitListReopened(0, "Kits (page 1)");
    }

    @Test
    void cancelsTheNameInputOnTheNextTickWithoutReopeningTheMenu() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        assertTrue(fixture.typeInChat("cancel"));
        fixture.runNextTick();

        assertEquals(List.of("The input for the name of the new kit was cancelled, nothing was changed"),
                fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
        assertTrue(fixture.isMenuClosed());
        assertEquals(0, fixture.pendingChatInputCount());
    }

    @Test
    void refusesTheTypedNameWhenThePermissionWasRevokedBeforeTheAnswer() {
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();
        fixture.revokeAdminPermission();

        fixture.typeInChat("Archer");
        fixture.runNextTick();

        assertEquals(List.of(NOT_ALLOWED), fixture.messages());
        assertEquals(0, fixture.kitLibrary().getKitCount());
        assertEquals(List.of(), fixture.kitStorage().savedLibraries());
        assertTrue(fixture.isMenuClosed());
    }

    @Test
    void keepsTheNewKitAndReportsItWhenKitsYmlCannotBeWritten() {
        fixture.kitStorage().failSaves(true);
        fixture.placeInAdminInventory(0, new EncodableItemStack("sword"));
        askForTheNameOfTheNewKit();

        fixture.typeInChat("Archer");
        fixture.runNextTick();

        assertEquals(List.of("Archer"), fixture.kitLibrary().getKitNames());
        assertEquals(List.of("The kits could not be written into kits.yml (the disk is full), the change counts "
                + "until the server stops and is written again with the next change of a kit"),
                fixture.kitWriteFailures());
        assertKitListReopened(0, "Kits (page 1)");
    }

    private void askForTheNameOfTheNewKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(KITS_CREATE_SLOT);
        fixture.messages().clear();
    }

    private void assertNothingDoneBeforeTheNextTick() {
        assertTrue(fixture.messages().isEmpty(), fixture.messages().toString());
        assertEquals(0, fixture.pendingChatInputCount());
        assertEquals(1, fixture.scheduledTaskCount());
        assertTrue(fixture.isMenuClosed());
    }

    private void assertKitListReopened(int pageIndex, String title) {
        RecordingAdminMenuWindows.RecordedMenu menu = fixture.lastMenu();
        assertEquals(AdminMenuType.KIT, menu.holder().getMenuType());
        assertEquals(pageIndex, menu.holder().getPageIndex());
        assertEquals(title, menu.title());
        assertFalse(fixture.isMenuClosed());
        assertEquals(0, fixture.pendingChatInputCount());
    }
}
