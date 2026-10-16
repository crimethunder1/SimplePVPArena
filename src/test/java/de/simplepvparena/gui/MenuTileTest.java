package de.simplepvparena.gui;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuTileTest {

    @Test
    void equalsATileWithTheSameMaterialTitleAndLore() {
        MenuTile tile = MenuItems.create(Material.PAPER, "Tournament status", "First line", "Second line");

        assertEquals(new MenuTile(Material.PAPER, "Tournament status", List.of("First line", "Second line")), tile);
        assertEquals(tile.hashCode(), new MenuTile(Material.PAPER, "Tournament status",
                List.of("First line", "Second line")).hashCode());
        assertNotEquals(new MenuTile(Material.BOOK, "Tournament status", List.of("First line", "Second line")),
                tile);
        assertNotEquals(new MenuTile(Material.PAPER, "Tournament status", List.of("First line")), tile);
    }

    @Test
    void keepsItsLoreWhenTheGivenListChangesAndRefusesChangesItself() {
        List<String> loreLines = new ArrayList<>(List.of("First line"));
        MenuTile tile = MenuItems.create(Material.PAPER, "Tournament status", loreLines);

        loreLines.add("Added afterwards");

        assertEquals(List.of("First line"), tile.loreLines());
        assertThrows(UnsupportedOperationException.class, () -> tile.loreLines().add("Added to the tile"));
    }
}
