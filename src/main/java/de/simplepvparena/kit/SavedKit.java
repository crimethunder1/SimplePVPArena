package de.simplepvparena.kit;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public record SavedKit(String name, SerializedKit serializedKit) {

    public SavedKit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(serializedKit, "serializedKit");
    }
}
