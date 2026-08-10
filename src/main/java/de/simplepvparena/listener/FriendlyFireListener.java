package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FriendlyFireListener implements Listener {

    private final MatchService matchService;

    public FriendlyFireListener(MatchService matchService) {
        this.matchService = matchService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Optional<UUID> attackerUniqueId = resolveCausingPlayer(event.getDamager());
        if (attackerUniqueId.isEmpty()) {
            return;
        }
        if (matchService.isFriendlyFire(attackerUniqueId.get(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        Optional<UUID> throwerUniqueId = resolveCausingPlayer(event.getPotion());
        if (throwerUniqueId.isEmpty() || !containsHarmfulEffect(event.getPotion().getEffects())) {
            return;
        }
        for (LivingEntity affectedEntity : List.copyOf(event.getAffectedEntities())) {
            if (isProtectedTeamMate(throwerUniqueId.get(), affectedEntity)) {
                event.setIntensity(affectedEntity, 0.0D);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud areaEffectCloud = event.getEntity();
        Optional<UUID> ownerUniqueId = resolveProjectileSource(areaEffectCloud.getSource());
        if (ownerUniqueId.isEmpty() || !containsHarmfulEffect(areaEffectCloud.getCustomEffects())) {
            return;
        }
        event.getAffectedEntities().removeIf(affectedEntity -> isProtectedTeamMate(ownerUniqueId.get(), affectedEntity));
    }

    private boolean isProtectedTeamMate(UUID attackerUniqueId, LivingEntity affectedEntity) {
        return affectedEntity instanceof Player victim
                && matchService.isFriendlyFire(attackerUniqueId, victim.getUniqueId());
    }

    private boolean containsHarmfulEffect(Collection<PotionEffect> potionEffects) {
        return potionEffects.stream()
                .anyMatch(potionEffect -> potionEffect.getType().getCategory() == PotionEffectTypeCategory.HARMFUL);
    }

    private Optional<UUID> resolveCausingPlayer(Entity damagingEntity) {
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

    private Optional<UUID> resolveProjectileSource(ProjectileSource projectileSource) {
        if (projectileSource instanceof Player player) {
            return Optional.of(player.getUniqueId());
        }
        return Optional.empty();
    }
}
