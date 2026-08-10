package de.simplepvparena.border;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderItemRequestTest {

    private static final Predicate<Material> EVERY_MATERIAL_IS_PLACEABLE = material -> true;
    private static final Predicate<Material> NO_MATERIAL_IS_PLACEABLE = material -> false;

    @Test
    void acceptsAPlaceableBlockWithAnAmount() {
        BorderItemRequestResult result = BorderItemRequest.parse("glass", "16", EVERY_MATERIAL_IS_PLACEABLE);

        assertTrue(result.isAccepted());
        assertEquals(Material.GLASS, result.getRequest().orElseThrow().material());
        assertEquals(16, result.getRequest().orElseThrow().amount());
    }

    @Test
    void acceptsAMaterialWithItsNamespace() {
        BorderItemRequestResult result = BorderItemRequest.parse("minecraft:white_concrete", "1",
                EVERY_MATERIAL_IS_PLACEABLE);

        assertTrue(result.isAccepted());
        assertEquals(Material.WHITE_CONCRETE, result.getRequest().orElseThrow().material());
    }

    @Test
    void rejectsAnUnknownMaterial() {
        BorderItemRequestResult result = BorderItemRequest.parse("cheese", "1", EVERY_MATERIAL_IS_PLACEABLE);

        assertFalse(result.isAccepted());
        assertEquals("Unknown material cheese", result.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAMaterialThatCannotBePlaced() {
        BorderItemRequestResult result = BorderItemRequest.parse("diamond_sword", "1", NO_MATERIAL_IS_PLACEABLE);

        assertFalse(result.isAccepted());
        assertEquals("DIAMOND_SWORD cannot be placed as a block", result.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnAmountThatIsNotANumber() {
        BorderItemRequestResult result = BorderItemRequest.parse("glass", "many", EVERY_MATERIAL_IS_PLACEABLE);

        assertFalse(result.isAccepted());
        assertEquals("The amount many is not a number", result.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnAmountOutsideOfAStack() {
        assertEquals("The amount has to be between 1 and 64",
                BorderItemRequest.parse("glass", "0", EVERY_MATERIAL_IS_PLACEABLE).getRejectionMessage().orElseThrow());
        assertEquals("The amount has to be between 1 and 64",
                BorderItemRequest.parse("glass", "65", EVERY_MATERIAL_IS_PLACEABLE).getRejectionMessage().orElseThrow());
    }

    @Test
    void readsATypedLineAsTheMaterialAndTheAmount() {
        BorderItemRequestResult result = BorderItemRequest.parseLine("  glass   16 ", EVERY_MATERIAL_IS_PLACEABLE);

        assertTrue(result.isAccepted());
        assertEquals(Material.GLASS, result.getRequest().orElseThrow().material());
        assertEquals(16, result.getRequest().orElseThrow().amount());
    }

    @Test
    void rejectsALineThatDoesNotHoldBothValues() {
        assertEquals("Expected 2 values separated by spaces (<material> <amount>), but got 1: glass",
                BorderItemRequest.parseLine("glass", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
        assertEquals("Expected 2 values separated by spaces (<material> <amount>), but got 3: glass 16 please",
                BorderItemRequest.parseLine("glass 16 please", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
        assertEquals("Nothing was typed, expected <material> <amount>",
                BorderItemRequest.parseLine("   ", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnUnknownMaterialOfATypedLine() {
        assertEquals("Unknown material cheese",
                BorderItemRequest.parseLine("cheese 16", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
    }

}
