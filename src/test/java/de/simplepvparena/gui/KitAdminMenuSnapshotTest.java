package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitAdminMenuSnapshotTest {

    private static final int MAIN_KIT_SLOT = 24;

    private final AdminMenuFixture fixture = new AdminMenuFixture();

    @Test
    void showsTheKitMenuWithAnEmptyKit() {
        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertEquals("""
                Kit | 27
                4 | BOOK | Kit | The kit is empty, no fighter is given anything / \
                Every fighter is given it when a round starts
                13 | CHEST | Save your inventory as the kit | Takes your helmet, chestplate, leggings, boots, / \
                your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / \
                It replaces the kit section of config.yml / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }

    @Test
    void showsTheKitMenuWithAFilledKit() {
        fixture.fillKit();

        fixture.openMenuFromTheMainMenu(MAIN_KIT_SLOT);

        assertEquals("""
                Kit | 27
                4 | BOOK | Kit | Worn items: 2 of 5 / Inventory items: 3 of 36 slots / \
                Every fighter is given it when a round starts
                13 | CHEST | Save your inventory as the kit | Takes your helmet, chestplate, leggings, boots, / \
                your offhand and the 36 slots of your inventory / \
                Enchantments, names, lore and damage are kept exactly / An empty slot stays empty in the kit / \
                The item on your cursor is not taken, it lies in no slot / \
                It replaces the kit section of config.yml / Asks for a confirmation first
                22 | OAK_DOOR | Back | Returns to the main menu
                """, fixture.describeLastMenu());
    }
}
