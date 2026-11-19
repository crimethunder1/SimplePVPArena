package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record TeamImportProblem(Kind kind, int lineNumber, String value) {

    public enum Kind {
        INVALID_NAME,
        NAMED_TWICE,
        GROUP_TOO_LARGE
    }

    public TeamImportProblem {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(value, "value");
    }
}
