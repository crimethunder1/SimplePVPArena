package de.simplepvparena.message;

public final class FailureDescriptions {

    private FailureDescriptions() {
    }

    public static String describe(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
