package de.simplepvparena.arena;

import org.bukkit.potion.PotionEffect;

import java.util.Objects;

public record SavedPotionEffect(String effectTypeKey, int amplifier, int durationTicks, boolean ambient,
                                boolean particles, boolean icon) {

    public static final int INFINITE_DURATION_TICKS = PotionEffect.INFINITE_DURATION;

    public SavedPotionEffect {
        Objects.requireNonNull(effectTypeKey, "effectTypeKey");
    }

    public boolean isRestorable() {
        return durationTicks > 0 || durationTicks == INFINITE_DURATION_TICKS;
    }
}
