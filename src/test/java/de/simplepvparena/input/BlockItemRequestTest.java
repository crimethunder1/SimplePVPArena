package de.simplepvparena.input;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockItemRequestTest {

    private static final Predicate<Material> EVERY_MATERIAL_IS_PLACEABLE = material -> true;
    private static final Predicate<Material> NO_MATERIAL_IS_PLACEABLE = material -> false;

    @Test
    void acceptsAPlaceableBlockWithAnAmount() {
        BlockItemRequestResult result = BlockItemRequest.parse("glass", "16", EVERY_MATERIAL_IS_PLACEABLE);

        assertTrue(result.isAccepted());
        assertEquals(Material.GLASS, result.getRequest().orElseThrow().material());
        assertEquals(16, result.getRequest().orElseThrow().amount());
    }

    @Test
    void acceptsAMaterialWithItsNamespace() {
        BlockItemRequestResult result = BlockItemRequest.parse("minecraft:white_concrete", "1",
                EVERY_MATERIAL_IS_PLACEABLE);

        assertTrue(result.isAccepted());
        assertEquals(Material.WHITE_CONCRETE, result.getRequest().orElseThrow().material());
    }

    @Test
    void acceptsTheBlocksAGateIsBuiltOutOf() {
        assertEquals(Material.BARRIER,
                BlockItemRequest.parseLine("barrier 32", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRequest().orElseThrow().material());
        assertEquals(Material.IRON_BARS,
                BlockItemRequest.parseLine("IRON_BARS 64", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRequest().orElseThrow().material());
        assertEquals(1, BlockItemRequest.parseLine("glass 1", EVERY_MATERIAL_IS_PLACEABLE)
                .getRequest().orElseThrow().amount());
    }

    @Test
    void rejectsAnUnknownMaterial() {
        BlockItemRequestResult result = BlockItemRequest.parse("cheese", "1", EVERY_MATERIAL_IS_PLACEABLE);

        assertFalse(result.isAccepted());
        assertEquals("Unknown material cheese", result.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAMaterialThatCannotBePlaced() {
        BlockItemRequestResult result = BlockItemRequest.parse("diamond_sword", "1", NO_MATERIAL_IS_PLACEABLE);

        assertFalse(result.isAccepted());
        assertEquals("DIAMOND_SWORD cannot be placed as a block", result.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnAmountThatIsNotANumber() {
        BlockItemRequestResult result = BlockItemRequest.parse("glass", "many", EVERY_MATERIAL_IS_PLACEABLE);

        assertFalse(result.isAccepted());
        assertEquals("The amount many is not a number", result.getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnAmountOutsideOfAStack() {
        assertEquals("The amount has to be between 1 and 64",
                BlockItemRequest.parse("glass", "0", EVERY_MATERIAL_IS_PLACEABLE).getRejectionMessage().orElseThrow());
        assertEquals("The amount has to be between 1 and 64",
                BlockItemRequest.parse("glass", "65", EVERY_MATERIAL_IS_PLACEABLE).getRejectionMessage().orElseThrow());
    }

    @Test
    void readsATypedLineAsTheMaterialAndTheAmount() {
        BlockItemRequestResult result = BlockItemRequest.parseLine("  glass   16 ", EVERY_MATERIAL_IS_PLACEABLE);

        assertTrue(result.isAccepted());
        assertEquals(Material.GLASS, result.getRequest().orElseThrow().material());
        assertEquals(16, result.getRequest().orElseThrow().amount());
    }

    @Test
    void rejectsALineThatDoesNotHoldBothValues() {
        assertEquals("Expected 2 values separated by spaces (<material> <amount>), but got 1: glass",
                BlockItemRequest.parseLine("glass", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
        assertEquals("Expected 2 values separated by spaces (<material> <amount>), but got 3: glass 16 please",
                BlockItemRequest.parseLine("glass 16 please", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
        assertEquals("Nothing was typed, expected <material> <amount>",
                BlockItemRequest.parseLine("   ", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
    }

    @Test
    void rejectsAnUnknownMaterialOfATypedLine() {
        assertEquals("Unknown material cheese",
                BlockItemRequest.parseLine("cheese 16", EVERY_MATERIAL_IS_PLACEABLE)
                        .getRejectionMessage().orElseThrow());
    }
}
