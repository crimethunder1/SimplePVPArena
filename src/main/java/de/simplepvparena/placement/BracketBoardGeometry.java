package de.simplepvparena.placement;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BracketBoardGeometry {

    public static final float HALF_WIDTH = BracketBoard.WIDTH / 2.0F;
    public static final float HEIGHT = BracketBoard.HEIGHT;
    public static final float FRAME = 0.1F;
    public static final float TREE_LEFT = -HALF_WIDTH + 0.2F;
    public static final float TREE_RIGHT = HALF_WIDTH - 0.2F;
    public static final float TREE_BOTTOM = 0.2F;
    public static final float TREE_TOP = 2.85F;
    public static final float DIVIDER_BOTTOM = 2.92F;
    public static final float HEADER_TOP = HEIGHT - FRAME - 0.08F;
    public static final float HEADER_SCALE = 0.5F;
    public static final float MAXIMUM_TREE_SCALE = 0.6F;

    static final float PIXELS_PER_BLOCK = 40.0F;
    static final int LINE_PIXELS = 10;
    static final int PADDING_PIXELS = 2;
    static final int AVERAGE_CHARACTER_PIXELS = 6;
    static final int PREFIX_CHARACTERS = 3;
    static final int MINIMUM_NAME_LENGTH = 3;

    private BracketBoardGeometry() {
    }

    public static float textHeight(int lineCount, float scale) {
        return (lineCount * LINE_PIXELS + PADDING_PIXELS) * scale / PIXELS_PER_BLOCK;
    }

    public static float treeScale(int lineCount) {
        if (lineCount <= 0) {
            return MAXIMUM_TREE_SCALE;
        }
        return Math.min(MAXIMUM_TREE_SCALE, (TREE_TOP - TREE_BOTTOM) / textHeight(lineCount, 1.0F));
    }

    public static float columnWidth(int columnCount) {
        return (TREE_RIGHT - TREE_LEFT) / Math.max(1, columnCount);
    }

    public static float columnCentre(int column, int columnCount) {
        return TREE_LEFT + columnWidth(columnCount) * (column + 0.5F);
    }

    public static int lineWidthPixels(int columnCount, int lineCount) {
        return Math.max(1, (int) (columnWidth(columnCount) * PIXELS_PER_BLOCK / treeScale(lineCount)) - 2);
    }

    public static int maximumNameLength(int columnCount, int lineCount) {
        return Math.max(MINIMUM_NAME_LENGTH,
                lineWidthPixels(columnCount, lineCount) / AVERAGE_CHARACTER_PIXELS - PREFIX_CHARACTERS);
    }
}
