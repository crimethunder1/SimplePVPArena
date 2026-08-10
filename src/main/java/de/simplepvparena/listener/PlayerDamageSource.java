package de.simplepvparena.listener;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

final class PlayerDamageSource {

    private PlayerDamageSource() {
    }

    static Optional<UUID> resolveCausingPlayer(Entity damagingEntity) {
        if (damagingEntity instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        if (damagingEntity instanceof Projectile projectile) {
            return resolveProjectileSource(projectile.getShooter());
        }
        if (damagingEntity instanceof AreaEffectCloud areaEffectCloud) {
            return resolveProjectileSource(areaEffectCloud.getSource());
        }
        if (damagingEntity instanceof TNTPrimed primedTnt && primedTnt.getSource() instanceof Player igniter) {
            return Optional.of(igniter.getUniqueId());
        }
        return Optional.empty();
    }

    static Optional<UUID> resolveProjectileSource(ProjectileSource projectileSource) {
        if (projectileSource instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        return Optional.empty();
    }

    static boolean containsHarmfulEffect(Collection<PotionEffect> potionEffects) {
        return potionEffects.stream()
                .anyMatch(potionEffect -> potionEffect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL);
    }
}
