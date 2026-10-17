package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.Server;
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
    private final ArenaRegistry arenaRegistry;
    private final Server server;

    public GlobalPvpLockListener(MatchService matchService, ArenaRegistry arenaRegistry, Server server) {
        this.matchService = matchService;
        this.arenaRegistry = arenaRegistry;
        this.server = server;
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
        if (isLocked(attackerUniqueId.get(), victim)) {
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

    boolean isLockedTarget(UUID attackerUniqueId, LivingEntity affectedEntity) {
        return affectedEntity instanceof Player victim
                && isLocked(attackerUniqueId, victim);
    }

    private boolean isLocked(UUID attackerUniqueId, Player victim) {
        if (!isInsideTheEffectArea(victim)) {
            return false;
        }
        UUID victimUniqueId = victim.getUniqueId();
        if (matchService.isPvpAllowed(attackerUniqueId, victimUniqueId)) {
            return false;
        }
        return !isAdminAgainstActiveFighter(isAdmin(attackerUniqueId), matchService.isActiveFighter(victimUniqueId));
    }

    private boolean isInsideTheEffectArea(Player victim) {
        return arenaRegistry.findEffectArea()
                .filter(effectArea -> effectArea.contains(victim.getLocation()))
                .isPresent();
    }

    static boolean isAdminAgainstActiveFighter(boolean attackerAdmin, boolean victimActiveFighter) {
        return attackerAdmin && victimActiveFighter;
    }

    private boolean isAdmin(UUID playerUniqueId) {
        Player player = server.getPlayer(playerUniqueId);
        return player != null && AdminPermission.isAdmin(player);
    }
}
