package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GlobalPvpLockListener implements Listener {

    private final MatchService matchService;

    public GlobalPvpLockListener(MatchService matchService) {
        this.matchService = matchService;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Optional<UUID> attackerUniqueId = PlayerDamageSource.resolveCausingPlayer(event.getDamager());
        if (attackerUniqueId.isEmpty()) {
            return;
        }
        if (!matchService.isPvpAllowed(attackerUniqueId.get(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        Optional<UUID> throwerUniqueId = PlayerDamageSource.resolveCausingPlayer(event.getPotion());
        if (throwerUniqueId.isEmpty() || !PlayerDamageSource.containsHarmfulEffect(event.getPotion().getEffects())) {
            return;
        }
        for (LivingEntity affectedEntity : List.copyOf(event.getAffectedEntities())) {
            if (isLockedTarget(throwerUniqueId.get(), affectedEntity)) {
                event.setIntensity(affectedEntity, 0.0D);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud areaEffectCloud = event.getEntity();
        Optional<UUID> ownerUniqueId = PlayerDamageSource.resolveProjectileSource(areaEffectCloud.getSource());
        if (ownerUniqueId.isEmpty() || !PlayerDamageSource.containsHarmfulEffect(areaEffectCloud.getCustomEffects())) {
            return;
        }
        event.getAffectedEntities().removeIf(affectedEntity -> isLockedTarget(ownerUniqueId.get(), affectedEntity));
    }

    private boolean isLockedTarget(UUID attackerUniqueId, LivingEntity affectedEntity) {
        return affectedEntity instanceof Player victim
                && !matchService.isPvpAllowed(attackerUniqueId, victim.getUniqueId());
    }
}
