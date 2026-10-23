package de.simplepvparena.arena;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public final class BukkitPlayerPotionEffects implements PlayerPotionEffects {

    @Override
    public List<SavedPotionEffect> read(Player player) {
        List<SavedPotionEffect> potionEffects = new ArrayList<>();
        for (PotionEffect activeEffect : player.getActivePotionEffects()) {
            potionEffects.add(new SavedPotionEffect(activeEffect.getType().getKey().toString(),
                    activeEffect.getAmplifier(), activeEffect.getDuration(), activeEffect.isAmbient(),
                    activeEffect.hasParticles(), activeEffect.hasIcon()));
        }
        return potionEffects;
    }

    @Override
    public void removeAll(Player player) {
        for (PotionEffect activeEffect : List.copyOf(player.getActivePotionEffects())) {
            player.removePotionEffect(activeEffect.getType());
        }
    }

    @Override
    public void apply(Player player, List<SavedPotionEffect> potionEffects) {
        for (SavedPotionEffect potionEffect : potionEffects) {
            NamespacedKey effectTypeKey = NamespacedKey.fromString(potionEffect.effectTypeKey());
            PotionEffectType effectType = effectTypeKey == null ? null
                    : RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(effectTypeKey);
            if (effectType != null) {
                player.addPotionEffect(new PotionEffect(effectType, potionEffect.durationTicks(),
                        potionEffect.amplifier(), potionEffect.ambient(), potionEffect.particles(),
                        potionEffect.icon()));
            }
        }
    }
}
