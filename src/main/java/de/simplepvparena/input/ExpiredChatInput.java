package de.simplepvparena.input;

import java.util.Objects;
import java.util.UUID;

public record ExpiredChatInput(UUID playerUniqueId, String description) {

    public ExpiredChatInput {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        Objects.requireNonNull(description, "description");
    }
}
