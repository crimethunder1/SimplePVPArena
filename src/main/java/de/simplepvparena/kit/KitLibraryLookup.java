package de.simplepvparena.kit;

import java.util.Objects;
import java.util.Optional;

public record KitLibraryLookup(Outcome outcome, Optional<SavedKitLibrary> savedKitLibrary, String fileName,
                               Optional<String> failureReason) {

    public enum Outcome {
        FOUND,
        MISSING,
        BROKEN
    }

    public KitLibraryLookup {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(savedKitLibrary, "savedKitLibrary");
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(failureReason, "failureReason");
    }

    public static KitLibraryLookup found(SavedKitLibrary savedKitLibrary, String fileName) {
        return new KitLibraryLookup(Outcome.FOUND, Optional.of(savedKitLibrary), fileName, Optional.empty());
    }

    public static KitLibraryLookup missing(String fileName) {
        return new KitLibraryLookup(Outcome.MISSING, Optional.empty(), fileName, Optional.empty());
    }

    public static KitLibraryLookup broken(String fileName, String failureReason) {
        return new KitLibraryLookup(Outcome.BROKEN, Optional.empty(), fileName, Optional.of(failureReason));
    }
}
