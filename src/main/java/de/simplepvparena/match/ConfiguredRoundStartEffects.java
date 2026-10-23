package de.simplepvparena.match;

import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Predicate;

public final class ConfiguredRoundStartEffects implements RoundStartEffects {

    private final RoundStartEffects vanillaEffects;
    private final Predicate<Player> customTotemAnimation;

    public ConfiguredRoundStartEffects(RoundStartEffects vanillaEffects, Predicate<Player> customTotemAnimation) {
        this.vanillaEffects = Objects.requireNonNull(vanillaEffects, "vanillaEffects");
        this.customTotemAnimation = Objects.requireNonNull(customTotemAnimation, "customTotemAnimation");
    }

    @Override
    public void blind(Player player, int durationTicks) {
        vanillaEffects.blind(player, durationTicks);
    }

    @Override
    public void removeBlindness(Player player) {
        vanillaEffects.removeBlindness(player);
    }

    @Override
    public void playFightStartAnimation(Player player) {
        if (!customTotemAnimation.test(player)) {
            vanillaEffects.playFightStartAnimation(player);
        }
    }
}
