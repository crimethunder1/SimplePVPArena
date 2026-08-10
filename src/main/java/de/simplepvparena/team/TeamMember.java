package de.simplepvparena.team;

import java.util.Objects;
import java.util.UUID;

public record TeamMember(UUID uniqueId, String playerName) {

    public TeamMember {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
