package de.simplepvparena.registration;

import java.util.Objects;
import java.util.UUID;

public record RegistrationEntry(UUID uniqueId, String playerName) {

    public RegistrationEntry {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
