package de.simplepvparena.kit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitDefinitionTest {

    @Test
    void isEmptyWithoutASingleItem() {
        KitDefinition kitDefinition = KitDefinition.empty();

        assertTrue(kitDefinition.isEmpty());
        assertEquals(0, kitDefinition.itemCount());
        assertEquals(List.of(Map.of()), List.of(kitDefinition.equipmentItems()));
        assertEquals(List.of(Map.of()), List.of(kitDefinition.inventoryItems()));
    }

    @Test
    void namesAnEmptyKitAsOneInTheKitMenu() {
        List<String> lines = KitDefinition.empty().describe();

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("empty"));
    }

    @Test
    void refusesToBeBuiltWithoutItsMaps() {
        assertThrows(NullPointerException.class, () -> new KitDefinition(null, Map.of()));
        assertThrows(NullPointerException.class, () -> new KitDefinition(Map.of(), null));
    }
}
