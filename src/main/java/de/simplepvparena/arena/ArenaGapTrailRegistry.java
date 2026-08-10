package de.simplepvparena.arena;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class ArenaGapTrailRegistry {

    private final Map<UUID, Runnable> trailCancellersByPlayer = new HashMap<>();

    void replace(UUID playerUniqueId, Runnable trailCanceller) {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        Objects.requireNonNull(trailCanceller, "trailCanceller");
        Runnable previousCanceller = trailCancellersByPlayer.put(playerUniqueId, trailCanceller);
        if (previousCanceller != null) {
            previousCanceller.run();
        }
    }

    void stop(UUID playerUniqueId) {
        Runnable trailCanceller = trailCancellersByPlayer.remove(playerUniqueId);
        if (trailCanceller != null) {
            trailCanceller.run();
        }
    }

    void stopAll() {
        for (Runnable trailCanceller : trailCancellersByPlayer.values()) {
            trailCanceller.run();
        }
        trailCancellersByPlayer.clear();
    }

    boolean isRunningFor(UUID playerUniqueId) {
        return trailCancellersByPlayer.containsKey(playerUniqueId);
    }

    int getRunningCount() {
        return trailCancellersByPlayer.size();
    }
}
