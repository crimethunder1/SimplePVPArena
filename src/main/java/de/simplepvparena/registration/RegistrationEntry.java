package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.UUID;

@NullMarked
public record RegistrationEntry(UUID uniqueId, String playerName) {

    public RegistrationEntry {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
