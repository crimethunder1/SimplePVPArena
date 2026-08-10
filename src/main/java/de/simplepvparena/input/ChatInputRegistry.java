package de.simplepvparena.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class ChatInputRegistry {

    public static final long DEFAULT_TIMEOUT_MILLIS = 60_000L;

    private final Map<UUID, PendingChatInput> pendingByPlayer = new ConcurrentHashMap<>();
    private final long timeoutMillis;
    private final LongSupplier clock;

    public ChatInputRegistry() {
        this(DEFAULT_TIMEOUT_MILLIS, System::currentTimeMillis);
    }

    public ChatInputRegistry(long timeoutMillis, LongSupplier clock) {
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("timeoutMillis must be at least one millisecond");
        }
        this.timeoutMillis = timeoutMillis;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public PendingChatInput await(UUID playerUniqueId, String description, Consumer<String> lineHandler) {
        Objects.requireNonNull(playerUniqueId, "playerUniqueId");
        PendingChatInput request = new PendingChatInput(description, lineHandler,
                clock.getAsLong() + timeoutMillis);
        pendingByPlayer.put(playerUniqueId, request);
        return request;
    }

    public Optional<PendingChatInput> take(UUID playerUniqueId) {
        if (playerUniqueId == null) {
            return Optional.empty();
        }
        PendingChatInput request = pendingByPlayer.remove(playerUniqueId);
        if (request == null || request.isExpiredAt(clock.getAsLong())) {
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public List<ExpiredChatInput> collectExpired() {
        long currentTimeMillis = clock.getAsLong();
        List<ExpiredChatInput> expiredRequests = new ArrayList<>();
        Iterator<Map.Entry<UUID, PendingChatInput>> entries = pendingByPlayer.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, PendingChatInput> entry = entries.next();
            if (entry.getValue().isExpiredAt(currentTimeMillis)) {
                entries.remove();
                expiredRequests.add(new ExpiredChatInput(entry.getKey(), entry.getValue().description()));
            }
        }
        return List.copyOf(expiredRequests);
    }

    public boolean forget(UUID playerUniqueId) {
        return playerUniqueId != null && pendingByPlayer.remove(playerUniqueId) != null;
    }

    public boolean isWaiting(UUID playerUniqueId) {
        if (playerUniqueId == null) {
            return false;
        }
        PendingChatInput request = pendingByPlayer.get(playerUniqueId);
        return request != null && !request.isExpiredAt(clock.getAsLong());
    }

    public int getPendingCount() {
        return pendingByPlayer.size();
    }

    public void clear() {
        pendingByPlayer.clear();
    }
}
