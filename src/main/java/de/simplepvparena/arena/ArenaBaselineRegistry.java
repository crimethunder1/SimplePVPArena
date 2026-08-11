package de.simplepvparena.arena;

import java.util.Optional;

public final class ArenaBaselineRegistry {

    private ArenaBaselineSnapshot snapshot;

    public void store(ArenaBaselineSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public void discard() {
        snapshot = null;
    }

    public boolean hasSnapshot() {
        return snapshot != null;
    }

    public Optional<ArenaBaselineSnapshot> find(ArenaShape shape) {
        return Optional.ofNullable(snapshot).filter(storedSnapshot -> storedSnapshot.covers(shape));
    }
}
