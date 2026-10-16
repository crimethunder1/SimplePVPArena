package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

import java.util.Optional;

public final class ArenaFightRegion {

    public static final int SEARCHED_BLOCKS_ABOVE_THE_WALL_TOP = 20;

    private ArenaFightRegion() {
    }

    public static boolean contains(Optional<ArenaShape> fightArea, String worldName, int blockX, int blockY,
                                   int blockZ) {
        return fightArea.isPresent() && contains(fightArea.get(), worldName, blockX, blockY, blockZ);
    }

    public static boolean contains(ArenaShape fightArea, String worldName, int blockX, int blockY, int blockZ) {
        return fightArea.getWorldName().equals(worldName)
                && blockY >= fightArea.getFloorY()
                && blockY <= findHighestBlockY(fightArea)
                && fightArea.containsCell(blockX, blockZ);
    }

    public static boolean contains(Optional<ArenaShape> fightArea, Location location) {
        if (location == null) {
            return false;
        }
        World world = location.getWorld();
        return world != null && contains(fightArea, world.getName(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ());
    }

    public static int findHighestBlockY(ArenaShape fightArea) {
        return fightArea.getWallTopY() + SEARCHED_BLOCKS_ABOVE_THE_WALL_TOP;
    }

    public static BoundingBox createSearchedBox(ArenaShape fightArea) {
        RegionBoundary boundary = fightArea.getBoundingBox();
        return new BoundingBox(boundary.getMinimumX(), fightArea.getFloorY(), boundary.getMinimumZ(),
                boundary.getMaximumX() + 1, findHighestBlockY(fightArea) + 1, boundary.getMaximumZ() + 1);
    }
}
