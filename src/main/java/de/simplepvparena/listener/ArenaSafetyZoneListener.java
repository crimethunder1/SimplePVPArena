package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaSafetyZone;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.EnumSet;
import java.util.Set;

public final class ArenaSafetyZoneListener implements Listener {

    private static final Set<EntityDamageEvent.DamageCause> PROTECTED_CAUSES = EnumSet.of(
            EntityDamageEvent.DamageCause.FALL,
            EntityDamageEvent.DamageCause.DROWNING,
            EntityDamageEvent.DamageCause.SUFFOCATION,
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.STARVATION);

    private final ArenaSafetyZone safetyZone;

    public ArenaSafetyZoneListener(ArenaSafetyZone safetyZone) {
        this.safetyZone = safetyZone;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!PROTECTED_CAUSES.contains(event.getCause())) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isProtected(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }
        if (isProtected(player)) {
            event.setCancelled(true);
        }
    }

    private boolean isProtected(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        return world != null
                && safetyZone.protects(player.getUniqueId(), world.getName(), location.getX(), location.getZ());
    }
}
