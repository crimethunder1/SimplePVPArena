package de.simplepvparena.voice;

import de.simplepvparena.config.PluginSettings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

@NullMarked
public final class VoiceIsolation {

    public static final String BYPASS_PERMISSION_NAME = "tournament.voice.bypass";
    public static final long REFRESH_PERIOD_TICKS = 10L;
    public static final long GROUP_REFUSAL_NOTICE_PAUSE_MILLIS = 3_000L;

    private final Supplier<VoiceRoster> rosterSource;
    private final VoiceNotices notices;
    private final LongSupplier clockMillis;
    private final AtomicReference<VoiceRoster> roster = new AtomicReference<>(VoiceRoster.EMPTY);
    private final Map<UUID, Long> lastGroupRefusalNotice = new HashMap<>();
    private volatile boolean enabled = PluginSettings.DEFAULT_VOICE_CHAT_ENABLED;
    private volatile @Nullable VoiceGroups groups;

    public VoiceIsolation(Supplier<VoiceRoster> rosterSource, VoiceNotices notices) {
        this(rosterSource, notices, System::currentTimeMillis);
    }

    public VoiceIsolation(Supplier<VoiceRoster> rosterSource, VoiceNotices notices, LongSupplier clockMillis) {
        this.rosterSource = Objects.requireNonNull(rosterSource, "rosterSource");
        this.notices = Objects.requireNonNull(notices, "notices");
        this.clockMillis = Objects.requireNonNull(clockMillis, "clockMillis");
    }

    public void attach(VoiceGroups voiceGroups) {
        groups = Objects.requireNonNull(voiceGroups, "voiceGroups");
    }

    public void detach() {
        groups = null;
        roster.set(VoiceRoster.EMPTY);
    }

    public boolean isAttached() {
        return groups != null;
    }

    public boolean isConnected() {
        VoiceGroups voiceGroups = groups;
        return voiceGroups != null && voiceGroups.isConnected();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        refresh();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public VoiceRoster getRoster() {
        return roster.get();
    }

    public boolean canHear(@Nullable UUID speaker, @Nullable UUID listener) {
        return roster.get().canHear(speaker, listener);
    }

    public boolean mayJoinGroup(@Nullable UUID player) {
        return !roster.get().isFighter(player);
    }

    public void refresh() {
        VoiceGroups voiceGroups = groups;
        if (voiceGroups == null) {
            roster.set(VoiceRoster.EMPTY);
            return;
        }
        VoiceRoster next = enabled ? rosterSource.get() : VoiceRoster.EMPTY;
        VoiceRoster previous = roster.getAndSet(next);
        if (next.isEmpty()) {
            lastGroupRefusalNotice.clear();
        }
        for (UUID fighter : next.fighterTeams().keySet()) {
            if (!previous.isFighter(fighter)) {
                notices.isolated(fighter);
            }
            if (voiceGroups.leaveGroup(fighter)) {
                notices.leftGroup(fighter);
            }
            if (next.isEliminated(fighter) && !previous.isEliminated(fighter)) {
                notices.eliminated(fighter);
            } else if (!next.isEliminated(fighter) && previous.isEliminated(fighter)) {
                notices.backInTheFight(fighter);
            }
        }
    }

    public void refuseGroup(UUID player) {
        long now = clockMillis.getAsLong();
        Long lastNotice = lastGroupRefusalNotice.get(player);
        if (lastNotice != null && now - lastNotice < GROUP_REFUSAL_NOTICE_PAUSE_MILLIS) {
            return;
        }
        lastGroupRefusalNotice.put(player, now);
        notices.groupRefused(player);
    }
}
