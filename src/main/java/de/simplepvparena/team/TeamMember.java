package de.simplepvparena.team;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.UUID;

@NullMarked
public record TeamMember(UUID uniqueId, String playerName) {

    public TeamMember {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
