package de.simplepvparena.border;

import java.util.Optional;

public record BorderItemRequestResult(BorderItemRequest request, String rejectionMessage) {

    public static BorderItemRequestResult accepted(BorderItemRequest request) {
        return new BorderItemRequestResult(request, null);
    }

    public static BorderItemRequestResult rejected(String rejectionMessage) {
        return new BorderItemRequestResult(null, rejectionMessage);
    }

    public boolean isAccepted() {
        return request != null;
    }

    public Optional<BorderItemRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    public Optional<String> getRejectionMessage() {
        return Optional.ofNullable(rejectionMessage);
    }
}
