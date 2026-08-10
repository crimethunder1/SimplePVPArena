package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockPositionCodecTest {

    @Test
    void encodesAndDecodesAPosition() {
        long encodedPosition = BlockPositionCodec.encode(123, 64, -456);

        assertEquals(123, BlockPositionCodec.decodeX(encodedPosition));
        assertEquals(64, BlockPositionCodec.decodeY(encodedPosition));
        assertEquals(-456, BlockPositionCodec.decodeZ(encodedPosition));
    }

    @Test
    void keepsNegativeCoordinates() {
        long encodedPosition = BlockPositionCodec.encode(-30000000, -64, -30000000);

        assertEquals(-30000000, BlockPositionCodec.decodeX(encodedPosition));
        assertEquals(-64, BlockPositionCodec.decodeY(encodedPosition));
        assertEquals(-30000000, BlockPositionCodec.decodeZ(encodedPosition));
    }

    @Test
    void encodesEveryPositionIntoADifferentValue() {
        assertNotEquals(BlockPositionCodec.encode(1, 0, 0), BlockPositionCodec.encode(0, 1, 0));
        assertNotEquals(BlockPositionCodec.encode(0, 1, 0), BlockPositionCodec.encode(0, 0, 1));
        assertEquals(BlockPositionCodec.encode(7, 8, 9), BlockPositionCodec.encode(7, 8, 9));
    }

    @Test
    void rejectsCoordinatesOutsideOfTheSupportedRange() {
        assertThrows(IllegalArgumentException.class,
                () -> BlockPositionCodec.encode(BlockPositionCodec.MAXIMUM_HORIZONTAL_COORDINATE + 1, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BlockPositionCodec.encode(0, BlockPositionCodec.MINIMUM_VERTICAL_COORDINATE - 1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> BlockPositionCodec.encode(0, 0, BlockPositionCodec.MINIMUM_HORIZONTAL_COORDINATE - 1));
    }

    @Test
    void describesAPositionWithItsCoordinates() {
        assertEquals("10/64/-5", BlockPositionCodec.describe(BlockPositionCodec.encode(10, 64, -5)));
    }
}
