package de.simplepvparena.input;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.UUID;

@NullMarked
public record ExpiredChatInput(UUID playerUniqueId, String description) {

    public ExpiredChatInput {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        Objects.requireNonNull(description, "description");
    }
}
