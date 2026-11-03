package de.simplepvparena.placement;

import java.util.Objects;
import java.util.UUID;

public record AutoMatchWins(UUID playerUniqueId, String playerName, int wins) {

    public AutoMatchWins {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        Objects.requireNonNull(playerName, "playerName");
    }
}
