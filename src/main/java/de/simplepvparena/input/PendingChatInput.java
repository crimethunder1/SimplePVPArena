package de.simplepvparena.input;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.function.Consumer;

@NullMarked
public record PendingChatInput(String description, Consumer<String> lineHandler, long expiresAtMillis) {

    public PendingChatInput {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(lineHandler, "lineHandler");
    }

    public boolean isExpiredAt(long currentTimeMillis) {
        return currentTimeMillis >= expiresAtMillis;
    }
}
