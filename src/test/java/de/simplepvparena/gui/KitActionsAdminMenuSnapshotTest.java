package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitActionsAdminMenuSnapshotTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int FIRST_KIT_SLOT = 0;
    private static final int SECOND_KIT_SLOT = 1;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheOptionsOfTheOnlyKitWhichIsTheDefaultKit() {
        fixture.createKit("Archer");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(FIRST_KIT_SLOT);

        assertEquals("""
                Kit Archer | 27
                4 | BOOK | Archer | Worn items: 2 of 5 / Inventory items: 3 of 36 / \
                Default kit: every fighter is given it when a round starts
                11 | GRAY_DYE | This is the default kit | Kit Archer is the default kit already
                13 | CHEST | Replace with your inventory | Your inventory becomes the kit Archer / \
                Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / Asks for a confirmation first
                15 | GRAY_DYE | Delete the kit | Kit Archer is the last kit and cannot be deleted
                22 | OAK_DOOR | Back | Returns to the kit list
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheOptionsOfTheDefaultKitBesideAFurtherKit() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(FIRST_KIT_SLOT);

        assertEquals("15 | GRAY_DYE | Delete the kit | Kit Archer is the default kit and cannot be deleted",
                lineOfSlot(fixture.describeLastMenu(), 15));
        assertEquals("11 | GRAY_DYE | This is the default kit | Kit Archer is the default kit already",
                lineOfSlot(fixture.describeLastMenu(), 11));
    }

    @Test
    void showsTheOptionsOfAKitThatIsNotTheDefaultKit() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(SECOND_KIT_SLOT);

        assertEquals("""
                Kit Knight | 27
                4 | BOOK | Knight | Worn items: 2 of 5 / Inventory items: 3 of 36 / \
                Not the default kit, no fighter is given it
                11 | NETHER_STAR | Use as the default kit | Every fighter is given it from the next round on / \
                A running round keeps the kit its fighters were given
                13 | CHEST | Replace with your inventory | Your inventory becomes the kit Knight / \
                Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / Asks for a confirmation first
                15 | RED_WOOL | Delete the kit | Removes the kit Knight / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the kit list
                """, fixture.describeLastMenu());
    }

    @Test
    void namesTheRunningMatchOnTheReplacementOfTheDefaultKit() {
        fixture.configureArena();
        fixture.registerTeams("Alpha", "Bravo");
        fixture.startMatchWithoutATournament("Alpha", "Bravo");
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(FIRST_KIT_SLOT);

        assertEquals("13 | CHEST | Replace with your inventory | Your inventory becomes the kit Archer / "
                + "Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / "
                + "Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / "
                + "The item on your cursor is not taken, it lies in no slot / "
                + "A match is running: its fighters keep their items, / "
                + "the new kit is handed out from the next round on / Asks for a confirmation first",
                lineOfSlot(fixture.describeLastMenu(), 13));
    }

    @Test
    void showsAKitThatWasDeletedMeanwhile() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);
        fixture.click(SECOND_KIT_SLOT);
        fixture.kitLibrary().deleteKit("Knight");

        fixture.refillLastMenu();

        String description = fixture.describeLastMenu();
        assertEquals("4 | BOOK | Knight | This kit does not exist any more", lineOfSlot(description, 4));
        assertEquals("11 | GRAY_DYE | Use as the default kit | Unknown kit Knight", lineOfSlot(description, 11));
        assertEquals("15 | GRAY_DYE | Delete the kit | Unknown kit Knight", lineOfSlot(description, 15));
    }

    private static String lineOfSlot(String description, int slot) {
        return description.lines().filter(line -> line.startsWith(slot + " | ")).findFirst().orElse("");
    }
}
