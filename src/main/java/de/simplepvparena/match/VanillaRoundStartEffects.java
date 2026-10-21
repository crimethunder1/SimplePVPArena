package de.simplepvparena.match;

import org.bukkit.EntityEffect;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class VanillaRoundStartEffects implements RoundStartEffects {

    private static final int BLINDNESS_AMPLIFIER = 0;
    private static final boolean AMBIENT = false;
    private static final boolean PARTICLES = false;

    @Override
    public void blind(Player player, int durationTicks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, BLINDNESS_AMPLIFIER,
                AMBIENT, PARTICLES));
    }

    @Override
    public void removeBlindness(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    @Override
    public void playFightStartAnimation(Player player) {
        player.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
    }

    @Override
    public void stopFightStartAnimation(Player player) {
    }

    @Override
    public void stopFightStartAnimations() {
    }
}
