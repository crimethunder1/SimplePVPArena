package de.simplepvparena.arena;

public final class BlockPositionCodec {

    public static final int MINIMUM_HORIZONTAL_COORDINATE = -(1 << 25);
    public static final int MAXIMUM_HORIZONTAL_COORDINATE = (1 << 25) - 1;
    public static final int MINIMUM_VERTICAL_COORDINATE = -(1 << 11);
    public static final int MAXIMUM_VERTICAL_COORDINATE = (1 << 11) - 1;

    private static final int HORIZONTAL_BIT_COUNT = 26;
    private static final int VERTICAL_BIT_COUNT = 12;
    private static final long HORIZONTAL_MASK = (1L << HORIZONTAL_BIT_COUNT) - 1L;
    private static final long VERTICAL_MASK = (1L << VERTICAL_BIT_COUNT) - 1L;
    private static final int X_SHIFT = HORIZONTAL_BIT_COUNT + VERTICAL_BIT_COUNT;
    private static final int Z_SHIFT = VERTICAL_BIT_COUNT;

    private BlockPositionCodec() {
    }

    public static long encode(int blockX, int blockY, int blockZ) {
        requireHorizontalRange(blockX, "blockX");
        requireVerticalRange(blockY);
        requireHorizontalRange(blockZ, "blockZ");
        return ((long) blockX & HORIZONTAL_MASK) << X_SHIFT
                | ((long) blockZ & HORIZONTAL_MASK) << Z_SHIFT
                | ((long) blockY & VERTICAL_MASK);
    }

    public static int decodeX(long encodedPosition) {
        return (int) (encodedPosition >> X_SHIFT);
    }

    public static int decodeY(long encodedPosition) {
        return (int) (encodedPosition << (Long.SIZE - VERTICAL_BIT_COUNT) >> (Long.SIZE - VERTICAL_BIT_COUNT));
    }

    public static int decodeZ(long encodedPosition) {
        return (int) (encodedPosition << (Long.SIZE - X_SHIFT) >> (Long.SIZE - HORIZONTAL_BIT_COUNT));
    }

    public static String describe(long encodedPosition) {
        return decodeX(encodedPosition) + "/" + decodeY(encodedPosition) + "/" + decodeZ(encodedPosition);
    }

    private static void requireHorizontalRange(int coordinate, String coordinateName) {
        if (coordinate < MINIMUM_HORIZONTAL_COORDINATE || coordinate > MAXIMUM_HORIZONTAL_COORDINATE) {
            throw new IllegalArgumentException(coordinateName + " " + coordinate + " is outside of "
                    + MINIMUM_HORIZONTAL_COORDINATE + " to " + MAXIMUM_HORIZONTAL_COORDINATE);
        }
    }

    private static void requireVerticalRange(int blockY) {
        if (blockY < MINIMUM_VERTICAL_COORDINATE || blockY > MAXIMUM_VERTICAL_COORDINATE) {
            throw new IllegalArgumentException("blockY " + blockY + " is outside of "
                    + MINIMUM_VERTICAL_COORDINATE + " to " + MAXIMUM_VERTICAL_COORDINATE);
        }
    }
}
