package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@NullMarked
public final class ArenaBaselineRegistry {

    private @Nullable ArenaBaselineSnapshot snapshot;

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
        return findStored().filter(storedSnapshot -> storedSnapshot.covers(shape));
    }

    public Optional<ArenaBaselineSnapshot> findStored() {
        return Optional.ofNullable(snapshot);
    }
}
