package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;

public final class BukkitArenaTemporaryEntityRemover implements ArenaTemporaryEntityRemover {

    @Override
    public int removeTemporaryEntities(World world, ArenaShape shape) {
        if (world == null) {
            return 0;
        }
        int removedEntityCount = 0;
        for (Entity entity : world.getNearbyEntities(ArenaFightRegion.createSearchedBox(shape))) {
            if (!isTemporary(entity) || isProtected(entity)
                    || !isInsideTheFightArea(entity.getLocation(), world, shape)) {
                continue;
            }
            entity.remove();
            removedEntityCount++;
        }
        return removedEntityCount;
    }

    private static boolean isTemporary(Entity entity) {
        return entity instanceof Item
                || entity instanceof ExperienceOrb
                || entity instanceof Projectile
                || entity instanceof TNTPrimed
                || entity instanceof FallingBlock
                || entity instanceof EnderCrystal
                || entity instanceof AreaEffectCloud;
    }

    private static boolean isProtected(Entity entity) {
        return entity instanceof Player
                || entity instanceof ArmorStand
                || entity instanceof ItemFrame
                || entity instanceof Painting
                || entity instanceof Boat
                || entity instanceof Minecart
                || entity instanceof LivingEntity;
    }

    private static boolean isInsideTheFightArea(Location location, World world, ArenaShape shape) {
        return ArenaFightRegion.contains(shape, world.getName(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
    }
}
