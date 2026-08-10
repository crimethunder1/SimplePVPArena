package de.simplepvparena.arena;

import java.util.Objects;

public record SpectatorAssignment(String arenaName, SpectatorMode spectatorMode) {

    public SpectatorAssignment {
        Objects.requireNonNull(arenaName, "arenaName");
        Objects.requireNonNull(spectatorMode, "spectatorMode");
    }

    public SpectatorAssignment withMode(SpectatorMode newSpectatorMode) {
        return new SpectatorAssignment(arenaName, newSpectatorMode);
    }
}
