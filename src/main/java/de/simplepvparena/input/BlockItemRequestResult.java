package de.simplepvparena.input;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@NullMarked
public record BlockItemRequestResult(@Nullable BlockItemRequest request, @Nullable String rejectionMessage) {

    public static BlockItemRequestResult accepted(BlockItemRequest request) {
        return new BlockItemRequestResult(request, null);
    }

    public static BlockItemRequestResult rejected(String rejectionMessage) {
        return new BlockItemRequestResult(null, rejectionMessage);
    }

    public boolean isAccepted() {
        return request != null;
    }

    public Optional<BlockItemRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }
}
