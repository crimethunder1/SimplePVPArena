package de.simplepvparena.voice;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@NullMarked
public final class VoiceIsolation {

    public static final String BYPASS_PERMISSION_NAME = "tournament.voice.bypass";
    public static final boolean DEFAULT_ENABLED = true;
    public static final long REFRESH_PERIOD_TICKS = 10L;

    private final Supplier<VoiceRoster> rosterSource;
    private final VoiceNotices notices;
    private final AtomicReference<VoiceRoster> roster = new AtomicReference<>(VoiceRoster.EMPTY);
    private volatile boolean enabled = DEFAULT_ENABLED;
    private volatile @Nullable VoiceGroups groups;

    public VoiceIsolation(Supplier<VoiceRoster> rosterSource, VoiceNotices notices) {
        this.rosterSource = Objects.requireNonNull(rosterSource, "rosterSource");
        this.notices = Objects.requireNonNull(notices, "notices");
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
        for (UUID fighter : next.fighterTeams().keySet()) {
            if (!previous.isFighter(fighter)) {
                notices.isolated(fighter);
            }
            if (voiceGroups.leaveGroup(fighter)) {
                notices.leftGroup(fighter);
            }
            if (next.isEliminated(fighter) && !previous.isEliminated(fighter)) {
                notices.eliminated(fighter);
            }
        }
    }

    public void refuseGroup(UUID player) {
        notices.groupRefused(player);
    }
}
