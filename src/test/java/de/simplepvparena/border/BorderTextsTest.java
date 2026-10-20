package de.simplepvparena.border;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.BorderColumnReader;
import de.simplepvparena.arena.HorizontalCellCodec;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BorderTextsTest {

    private static final String WORLD = "event";
    private static final int FLOOR_Y = 64;

    @Test
    @UseLanguage("en")
    void describesTheMarksAndTheOutlineInEnglish() {
        assertEquals(List.of("Marked as a border block", "Border mark removed"),
                List.of(BorderMarkToggle.MARKED.describe(), BorderMarkToggle.UNMARKED.describe()));
        assertEquals(List.of("1 border block connected by 0 lines", "2 border blocks connected by 1 line",
                "2 border blocks connected by 0 lines, the line limit was reached and only a part of the wall is "
                        + "shown"), describeEveryOutline());
    }

    @Test
    @UseLanguage("de")
    void describesTheMarksAndTheOutlineInGerman() {
        assertEquals(List.of("Als Wandblock markiert", "Wandmarkierung entfernt"),
                List.of(BorderMarkToggle.MARKED.describe(), BorderMarkToggle.UNMARKED.describe()));
        assertEquals(List.of("1 Wandblock, verbunden durch 0 Linien", "2 Wandblöcke, verbunden durch 1 Linie",
                "2 Wandblöcke, verbunden durch 0 Linien, das Linienlimit wurde erreicht und nur ein Teil der Wand "
                        + "wird angezeigt"), describeEveryOutline());
    }

    private static List<String> describeEveryOutline() {
        ArenaShape room = new ArenaShape(WORLD, FLOOR_Y, FLOOR_Y + 1, new long[] {HorizontalCellCodec.encode(1, 1)});
        BorderColumnReader oneBlock = (blockX, blockZ) -> blockX == 0 && blockZ == 1
                ? new int[] {FLOOR_Y} : BorderColumnReader.NO_BORDER_BLOCKS;
        BorderColumnReader twoBlocks = (blockX, blockZ) -> blockX == 0 && blockZ == 1
                ? new int[] {FLOOR_Y, FLOOR_Y + 1} : BorderColumnReader.NO_BORDER_BLOCKS;
        return List.of(BorderWallOutline.around(room, oneBlock).describe(),
                BorderWallOutline.around(room, twoBlocks).describe(),
                BorderWallOutline.around(room, twoBlocks, 0).describe());
    }
}
