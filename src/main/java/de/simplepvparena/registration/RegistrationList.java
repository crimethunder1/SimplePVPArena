package de.simplepvparena.registration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class RegistrationList {

    private final Map<UUID, RegistrationEntry> entriesByUniqueId = new LinkedHashMap<>();

    public boolean register(UUID uniqueId, String playerName) {
        RegistrationEntry entry = new RegistrationEntry(uniqueId, playerName);
        if (entriesByUniqueId.containsKey(uniqueId)) {
            return false;
        }
        entriesByUniqueId.put(uniqueId, entry);
        return true;
    }

    public boolean rememberName(UUID uniqueId, String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        RegistrationEntry entry = entriesByUniqueId.get(uniqueId);
        if (entry == null || entry.playerName().equals(playerName)) {
            return false;
        }
        entriesByUniqueId.put(uniqueId, new RegistrationEntry(uniqueId, playerName));
        return true;
    }

    public boolean unregister(UUID uniqueId) {
        return entriesByUniqueId.remove(uniqueId) != null;
    }

    public boolean isRegistered(UUID uniqueId) {
        return entriesByUniqueId.containsKey(uniqueId);
    }

    public List<RegistrationEntry> getEntries() {
        return List.copyOf(entriesByUniqueId.values());
    }

    public int getCount() {
        return entriesByUniqueId.size();
    }

    public void clear() {
        entriesByUniqueId.clear();
    }
}
