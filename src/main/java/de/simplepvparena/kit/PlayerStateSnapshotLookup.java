package de.simplepvparena.kit;

import java.util.Objects;
import java.util.Optional;

public record PlayerStateSnapshotLookup(Outcome outcome, Optional<PlayerStateSnapshot> snapshot, String fileName) {

    public enum Outcome {
        FOUND,
        MISSING,
        BROKEN
    }

    public PlayerStateSnapshotLookup {
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(fileName, "fileName");
    }

    public static PlayerStateSnapshotLookup found(PlayerStateSnapshot snapshot, String fileName) {
        return new PlayerStateSnapshotLookup(Outcome.FOUND, Optional.of(snapshot), fileName);
    }

    public static PlayerStateSnapshotLookup missing(String fileName) {
        return new PlayerStateSnapshotLookup(Outcome.MISSING, Optional.empty(), fileName);
    }

    public static PlayerStateSnapshotLookup broken(String fileName) {
        return new PlayerStateSnapshotLookup(Outcome.BROKEN, Optional.empty(), fileName);
    }
}
