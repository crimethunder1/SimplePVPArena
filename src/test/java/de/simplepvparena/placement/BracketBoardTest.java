package de.simplepvparena.placement;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BracketBoardTest {

    @Test
    void coversSixBlocksSideBySideAndFourBlocksHigh() {
        BracketBoard board = new BracketBoard("world", 10, 64, 20, BlockFace.SOUTH);

        assertEquals(BracketBoard.WIDTH * BracketBoard.HEIGHT, board.blocks().size());
        assertTrue(board.contains("world", 7, 64, 20));
        assertTrue(board.contains("world", 12, 67, 20));
        assertFalse(board.contains("world", 13, 64, 20));
        assertFalse(board.contains("world", 10, 68, 20));
        assertFalse(board.contains("world", 10, 64, 21));
        assertFalse(board.contains("nether", 10, 64, 20));
    }

    @Test
    void centresItsFrontOnTheFacingSide() {
        BracketBoard south = new BracketBoard("world", 10, 64, 20, BlockFace.SOUTH);
        BracketBoard west = new BracketBoard("world", 10, 64, 20, BlockFace.WEST);

        assertEquals(10.0D, south.frontCentreX());
        assertEquals(21.0D, south.frontCentreZ());
        assertEquals(10.0D, west.frontCentreX());
        assertEquals(20.0D, west.frontCentreZ());
        assertTrue(west.contains("world", 10, 64, 17));
    }

    @Test
    void refusesAnUpwardFacing() {
        assertThrows(IllegalArgumentException.class, () -> new BracketBoard("world", 0, 0, 0, BlockFace.UP));
    }

    @Test
    void keepsTwoBoardsFromOverlapping() {
        BracketBoards boards = new BracketBoards();

        assertTrue(boards.add(new BracketBoard("world", 0, 64, 0, BlockFace.SOUTH)));
        assertFalse(boards.add(new BracketBoard("world", 5, 66, 0, BlockFace.NORTH)));
        assertTrue(boards.add(new BracketBoard("world", 6, 64, 0, BlockFace.SOUTH)));
        assertTrue(boards.findAt("world", 3, 68, 0).isEmpty());
        assertEquals(2, boards.boards().size());
    }
}
