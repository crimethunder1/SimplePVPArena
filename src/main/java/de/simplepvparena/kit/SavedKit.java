package de.simplepvparena.kit;

import java.util.Objects;

public record SavedKit(String name, SerializedKit serializedKit) {

    public SavedKit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(serializedKit, "serializedKit");
    }
}
