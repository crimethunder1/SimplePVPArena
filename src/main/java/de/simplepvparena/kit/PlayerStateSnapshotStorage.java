package de.simplepvparena.kit;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.UUID;

@NullMarked
public interface PlayerStateSnapshotStorage {

    void save(UUID playerUniqueId, PlayerStateSnapshot snapshot) throws IOException;

    PlayerStateSnapshotLookup find(UUID playerUniqueId);

    void delete(UUID playerUniqueId) throws IOException;

    void setAsideAsRestored(UUID playerUniqueId) throws IOException;
}
