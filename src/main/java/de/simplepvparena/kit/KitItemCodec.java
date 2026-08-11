package de.simplepvparena.kit;

import java.util.Base64;
import java.util.Optional;

public final class KitItemCodec {

    private KitItemCodec() {
    }

    public static String encode(byte[] itemBytes) {
        return Base64.getEncoder().encodeToString(itemBytes);
    }

    public static Optional<byte[]> decode(String encodedItem) {
        if (encodedItem == null || encodedItem.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] itemBytes = Base64.getDecoder().decode(encodedItem.trim());
            return itemBytes.length == 0 ? Optional.empty() : Optional.of(itemBytes);
        } catch (IllegalArgumentException notBase64) {
            return Optional.empty();
        }
    }
}
