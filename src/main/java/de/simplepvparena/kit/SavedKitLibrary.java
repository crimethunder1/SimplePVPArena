package de.simplepvparena.kit;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SavedKitLibrary(List<SavedKit> kits, Optional<String> defaultKitName) {

    public SavedKitLibrary {
        kits = List.copyOf(kits);
        Objects.requireNonNull(defaultKitName, "defaultKitName");
    }

    public static SavedKitLibrary empty() {
        return new SavedKitLibrary(List.of(), Optional.empty());
    }
}
