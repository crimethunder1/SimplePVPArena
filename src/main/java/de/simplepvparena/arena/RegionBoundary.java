package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

public final class RegionBoundary {

    private final String worldName;
    private final int minimumX;
    private final int minimumY;
    private final int minimumZ;
    private final int maximumX;
    private final int maximumY;
    private final int maximumZ;

    public RegionBoundary(String worldName,
                          int firstCornerX, int firstCornerY, int firstCornerZ,
                          int secondCornerX, int secondCornerY, int secondCornerZ) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.minimumX = Math.min(firstCornerX, secondCornerX);
        this.minimumY = Math.min(firstCornerY, secondCornerY);
        this.minimumZ = Math.min(firstCornerZ, secondCornerZ);
        this.maximumX = Math.max(firstCornerX, secondCornerX);
        this.maximumY = Math.max(firstCornerY, secondCornerY);
        this.maximumZ = Math.max(firstCornerZ, secondCornerZ);
    }

    public static RegionBoundary fromCorners(Location firstCorner, Location secondCorner) {
        World firstWorld = firstCorner.getWorld();
        World secondWorld = secondCorner.getWorld();
        if (firstWorld == null || secondWorld == null || !firstWorld.getName().equals(secondWorld.getName())) {
            throw new IllegalArgumentException("both corners must be located in the same world");
        }
        return new RegionBoundary(firstWorld.getName(),
                firstCorner.getBlockX(), firstCorner.getBlockY(), firstCorner.getBlockZ(),
                secondCorner.getBlockX(), secondCorner.getBlockY(), secondCorner.getBlockZ());
    }

    public boolean contains(String otherWorldName, double x, double y, double z) {
        if (!worldName.equals(otherWorldName)) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        return blockX >= minimumX && blockX <= maximumX
                && blockY >= minimumY && blockY <= maximumY
                && blockZ >= minimumZ && blockZ <= maximumZ;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    public PositionSnapshot getCenterPosition() {
        double centerX = (minimumX + maximumX) / 2.0 + 0.5;
        double centerZ = (minimumZ + maximumZ) / 2.0 + 0.5;
        return new PositionSnapshot(worldName, centerX, minimumY, centerZ, 0.0f, 0.0f);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinimumX() {
        return minimumX;
    }

    public int getMinimumY() {
        return minimumY;
    }

    public int getMinimumZ() {
        return minimumZ;
    }

    public int getMaximumX() {
        return maximumX;
    }

    public int getMaximumY() {
        return maximumY;
    }

    public int getMaximumZ() {
        return maximumZ;
    }

    public String describe() {
        return worldName + " from " + minimumX + "/" + minimumY + "/" + minimumZ
                + " to " + maximumX + "/" + maximumY + "/" + maximumZ;
    }
}
