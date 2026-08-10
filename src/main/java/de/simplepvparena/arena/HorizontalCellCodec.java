package de.simplepvparena.arena;

public final class HorizontalCellCodec {

    private static final long LOWER_MASK = 0xFFFFFFFFL;
    private static final int X_SHIFT = 32;

    private HorizontalCellCodec() {
    }

    public static long encode(int blockX, int blockZ) {
        return ((long) blockX << X_SHIFT) | (blockZ & LOWER_MASK);
    }

    public static int decodeX(long encodedCell) {
        return (int) (encodedCell >> X_SHIFT);
    }

    public static int decodeZ(long encodedCell) {
        return (int) encodedCell;
    }

    public static String describe(long encodedCell) {
        return decodeX(encodedCell) + "/" + decodeZ(encodedCell);
    }
}
