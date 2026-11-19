package de.simplepvparena.placement;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.UUID;

@NullMarked
public record AutoMatchWins(UUID playerUniqueId, String playerName, int wins) {

    public AutoMatchWins {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
