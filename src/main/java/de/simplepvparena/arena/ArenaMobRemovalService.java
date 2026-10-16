package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

public final class ArenaMobRemovalService {

    public static final long PERIOD_TICKS = 20L;

    private final Plugin plugin;
    private final ArenaRegistry arenaRegistry;

    private BukkitTask activeTask;

    public ArenaMobRemovalService(Plugin plugin, ArenaRegistry arenaRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
    }

    public boolean isRunning() {
        return activeTask != null;
    }

    public void start() {
        if (isRunning()) {
            return;
        }
        activeTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::removeMobsAndReportFailures,
                PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }

    public int removeMobsInTheFightArea() {
        Optional<ArenaShape> fightArea = arenaRegistry.getArena().getShape();
        if (fightArea.isEmpty()) {
            return 0;
        }
        ArenaShape shape = fightArea.get();
        World world = plugin.getServer().getWorld(shape.getWorldName());
        if (world == null) {
            return 0;
        }
        int removedMobCount = 0;
        for (Entity entity : world.getNearbyEntities(ArenaFightRegion.createSearchedBox(shape))) {
            if (!isMob(entity) || !isInsideTheFightArea(entity.getLocation(), world, shape)) {
                continue;
            }
            entity.remove();
            removedMobCount++;
        }
        return removedMobCount;
    }

    private void removeMobsAndReportFailures() {
        try {
            removeMobsInTheFightArea();
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "The mobs in the fight area could not be removed, "
                    + "the next attempt follows in " + PERIOD_TICKS + " ticks", failure);
        }
    }

    private static boolean isMob(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof Player)
                && !(entity instanceof ArmorStand);
    }

    private static boolean isInsideTheFightArea(Location location, World world, ArenaShape shape) {
        return ArenaFightRegion.contains(shape, world.getName(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
    }
}
