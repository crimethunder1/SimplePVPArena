package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitAdminMenuSnapshotTest {

    private static final int MAIN_KIT_SLOT = 24;
    private static final int KITS_NEXT_PAGE_SLOT = 53;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheKitListWithoutAKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertEquals("""
                Kits (page 1) | 54
                47 | BOOK | Kits | Kits: 0 / Default kit: none / Page 1 of 1 / A click on a kit opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Create a kit from your inventory | Asks for the name of the new kit in the chat / \
                Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / The first kit becomes the default kit / \
                The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheKitListWithOneKit() {
        fixture.createKit("Archer");

        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertEquals("""
                Kits (page 1) | 54
                0 | CHEST | Archer | Worn items: 2 of 5 / Inventory items: 3 of 36 / Default kit / \
                Click for the options of this kit
                47 | BOOK | Kits | Kits: 1 / Default kit: Archer / Page 1 of 1 / A click on a kit opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Create a kit from your inventory | Asks for the name of the new kit in the chat / \
                Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / The first kit becomes the default kit / \
                The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    @Test
    void marksOnlyTheDefaultKitAsTheDefaultKit() {
        fixture.createKit("Archer");
        fixture.createKit("Knight");
        fixture.kitLibrary().setDefaultKit("Knight");

        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertEquals("Archer [Worn items: 2 of 5, Inventory items: 3 of 36, Click for the options of this kit]",
                describeKitTile(0));
        assertEquals("Knight [Worn items: 2 of 5, Inventory items: 3 of 36, Default kit, "
                + "Click for the options of this kit]", describeKitTile(1));
        assertEquals("Default kit: Knight", fixture.lastMenu().findTile(47).orElseThrow().loreLines().get(1));
    }

    @Test
    void showsTheFirstPageOfFortySixKits() {
        fixture.createNumberedKits(46);

        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        String description = fixture.describeLastMenu();
        assertEquals("Kits (page 1) | 54", description.lines().findFirst().orElseThrow());
        assertEquals("0 | CHEST | Kit01 | Worn items: 0 of 5 / Inventory items: 1 of 36 / Default kit / "
                + "Click for the options of this kit", lineOfSlot(description, 0));
        assertEquals("44 | CHEST | Kit45 | Worn items: 0 of 5 / Inventory items: 1 of 36 / "
                + "Click for the options of this kit", lineOfSlot(description, 44));
        assertEquals("", lineOfSlot(description, 45));
        assertEquals("47 | BOOK | Kits | Kits: 46 / Default kit: Kit01 / Page 1 of 2 / "
                + "A click on a kit opens its options", lineOfSlot(description, 47));
        assertEquals("53 | ARROW | Next page | Page 2 of 2", lineOfSlot(description, 53));
    }

    @Test
    void showsTheSecondPageOfFortySixKits() {
        fixture.createNumberedKits(46);
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        fixture.click(KITS_NEXT_PAGE_SLOT);

        assertEquals("""
                Kits (page 2) | 54
                0 | CHEST | Kit46 | Worn items: 0 of 5 / Inventory items: 1 of 36 / Click for the options of this kit
                45 | ARROW | Previous page | Page 1 of 2
                47 | BOOK | Kits | Kits: 46 / Default kit: Kit01 / Page 2 of 2 / A click on a kit opens its options
                49 | OAK_DOOR | Back | Returns to the main menu
                51 | LIME_WOOL | Create a kit from your inventory | Asks for the name of the new kit in the chat / \
                Takes your helmet, chestplate, leggings, boots, / your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / The first kit becomes the default kit / \
                The menu closes so you can type
                """, fixture.describeLastMenu());
    }

    private String describeKitTile(int slot) {
        MenuTile tile = fixture.lastMenu().findTile(slot).orElseThrow();
        return tile.displayName() + " " + tile.loreLines();
    }

    private static String lineOfSlot(String description, int slot) {
        return description.lines().filter(line -> line.startsWith(slot + " | ")).findFirst().orElse("");
    }
}
