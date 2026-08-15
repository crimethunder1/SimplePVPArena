package de.simplepvparena.border;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BorderSelectorModeRegistry {

    private final Set<UUID> activeAdmins = ConcurrentHashMap.newKeySet();

    public boolean activate(UUID playerUniqueId) {
        return playerUniqueId != null && activeAdmins.add(playerUniqueId);
    }

    public boolean deactivate(UUID playerUniqueId) {
        return playerUniqueId != null && activeAdmins.remove(playerUniqueId);
    }

    public boolean toggle(UUID playerUniqueId) {
        if (playerUniqueId == null) {
            return false;
        }
        if (activeAdmins.add(playerUniqueId)) {
            return true;
        }
        activeAdmins.remove(playerUniqueId);
        return false;
    }

    public boolean isActive(UUID playerUniqueId) {
        return playerUniqueId != null && activeAdmins.contains(playerUniqueId);
    }

    public int getActiveCount() {
        return activeAdmins.size();
    }

    public void clear() {
        activeAdmins.clear();
    }
}
