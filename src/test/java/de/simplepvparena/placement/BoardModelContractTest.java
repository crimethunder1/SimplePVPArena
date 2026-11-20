package de.simplepvparena.placement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the numbers the ItemsAdder models of CraftCoreTools/simplepvparena_board_gen.py are built on. A change here
 * moves the text and the flames away from the models: change the generator in the same step and generate again.
 */
class BoardModelContractTest {

    @Test
    void theStandingsBoardModelIsCentredOnTheCubeAtFourTimesItsSize() {
        assertEquals("standings_board", PaperPlacementBoardEntities.MODEL);
        assertEquals("standings_board_glow", PaperPlacementBoardEntities.GLOW_MODEL);
        assertEquals(4.0F, PaperPlacementBoardEntities.MODEL_SCALE);
    }

    @Test
    void theBracketBoardModelSitsOnTheAnchorTheGeneratorUses() {
        assertEquals("bracket_board", BracketBoardDisplays.MODEL);
        assertEquals("bracket_board_glow", BracketBoardDisplays.GLOW_MODEL);
        assertEquals(4.0F, BracketBoardDisplays.MODEL_SCALE);
        assertEquals(2.0F, BracketBoardDisplays.MODEL_ANCHOR_Y);
        assertEquals(-0.85F, BracketBoardDisplays.MODEL_ANCHOR_Z);
    }

    @Test
    void theBracketBoardKeepsTheSizeAndTheDividerTheTexturesAreDrawnFor() {
        assertEquals(6, BracketBoard.WIDTH);
        assertEquals(4, BracketBoard.HEIGHT);
        assertEquals(0.1F, BracketBoardGeometry.FRAME);
        assertEquals(2.92F, BracketBoardGeometry.DIVIDER_BOTTOM);
    }
}
