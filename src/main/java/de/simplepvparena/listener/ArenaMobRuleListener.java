package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaFightRegion;
import de.simplepvparena.arena.ArenaRegistry;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Objects;

public final class ArenaMobRuleListener implements Listener {

    private final ArenaRegistry arenaRegistry;

    public ArenaMobRuleListener(ArenaRegistry arenaRegistry) {
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (ArenaFightRegion.contains(arenaRegistry.getArena().getShape(), event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!isMob(event.getEntity())) {
            return;
        }
        Block changedBlock = event.getBlock();
        if (ArenaFightRegion.contains(arenaRegistry.getArena().getShape(), changedBlock.getWorld().getName(),
                changedBlock.getX(), changedBlock.getY(), changedBlock.getZ())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isMob(event.getEntity())) {
            return;
        }
        if (ArenaFightRegion.contains(arenaRegistry.getArena().getShape(), event.getLocation())) {
            event.blockList().clear();
        }
    }

    private static boolean isMob(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Player);
    }
}
