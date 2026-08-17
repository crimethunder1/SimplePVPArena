package de.simplepvparena.input;

import java.util.Optional;

public record BlockItemRequestResult(BlockItemRequest request, String rejectionMessage) {

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
