package de.simplepvparena.kit;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitItemCodecTest {

    @Test
    void handsEveryByteBackUnchanged() {
        byte[] itemBytes = new byte[256];
        for (int index = 0; index < itemBytes.length; index++) {
            itemBytes[index] = (byte) (index - 128);
        }

        Optional<byte[]> decodedBytes = KitItemCodec.decode(KitItemCodec.encode(itemBytes));

        assertArrayEquals(itemBytes, decodedBytes.orElseThrow());
    }

    @Test
    void survivesBytesThatAreNoText() {
        byte[] itemBytes = {0, 10, 13, -1, 0, 27};

        assertArrayEquals(itemBytes, KitItemCodec.decode(KitItemCodec.encode(itemBytes)).orElseThrow());
    }

    @Test
    void writesTheLineAsOneWordWithoutLineBreaks() {
        byte[] itemBytes = new byte[512];

        String encodedItem = KitItemCodec.encode(itemBytes);

        assertTrue(encodedItem.chars().noneMatch(character -> character == '\n' || character == '\r'),
                "a stored item has to be a single yaml line");
    }

    @Test
    void readsALineWithSurroundingWhitespaceAnAdminLeftBehind() {
        String encodedItem = KitItemCodec.encode("kit".getBytes(StandardCharsets.UTF_8));

        assertArrayEquals("kit".getBytes(StandardCharsets.UTF_8),
                KitItemCodec.decode("  " + encodedItem + "  ").orElseThrow());
    }

    @Test
    void handsOutNothingForALineThatIsNoStoredItem() {
        assertTrue(KitItemCodec.decode("not an item at all!").isEmpty());
        assertTrue(KitItemCodec.decode("").isEmpty());
        assertTrue(KitItemCodec.decode("   ").isEmpty());
        assertTrue(KitItemCodec.decode(null).isEmpty());
    }
}
