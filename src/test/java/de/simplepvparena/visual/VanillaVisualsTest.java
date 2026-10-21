package de.simplepvparena.visual;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaVisualsTest {

    private final VanillaVisuals visuals = new VanillaVisuals();

    @Test
    void knowsNoFontImageSoEveryTitleStaysTheOwnOne() {
        assertEquals(Optional.empty(), visuals.findFontImage("simplepvparena:countdown_3"));
        assertEquals(Optional.empty(), visuals.findFontImage(""));
    }

    @Test
    void knowsNoItemSoEveryEffectStaysTheVanillaOne() {
        assertEquals(Optional.empty(), visuals.findItem("simplepvparena:totem"));
        assertEquals(Optional.empty(), visuals.findItem(""));
    }

    @Test
    void isReadyFromTheStart() {
        assertTrue(visuals.isReady());
    }
}
