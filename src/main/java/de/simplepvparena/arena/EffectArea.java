package de.simplepvparena.arena;

import org.bukkit.Location;

import java.util.Locale;
import java.util.Objects;

public final class EffectArea {

    public static final String MARGIN_CONFIGURATION_PATH = "arena.effect-margin";
    public static final int DEFAULT_MARGIN = 96;
    public static final int MINIMUM_MARGIN = 0;
    public static final int MAXIMUM_MARGIN = 512;

    private final String worldName;
    private final Center center;
    private final double radius;

    private EffectArea(String worldName, Center center, double radius) {
        this.worldName = worldName;
        this.center = center;
        this.radius = radius;
    }

    public static EffectArea around(ArenaShape fightArea, int margin) {
        Objects.requireNonNull(fightArea, "fightArea");
        RegionBoundary boundingBox = fightArea.getBoundingBox();
        double minimumX = boundingBox.getMinimumX();
        double minimumZ = boundingBox.getMinimumZ();
        double maximumX = boundingBox.getMaximumX() + 1.0D;
        double maximumZ = boundingBox.getMaximumZ() + 1.0D;
        Center center = new Center((minimumX + maximumX) / 2.0D, (minimumZ + maximumZ) / 2.0D);
        double distanceToTheFarthestCorner = Math.max(
                Math.max(center.distanceTo(minimumX, minimumZ), center.distanceTo(minimumX, maximumZ)),
                Math.max(center.distanceTo(maximumX, minimumZ), center.distanceTo(maximumX, maximumZ)));
        return new EffectArea(fightArea.getWorldName(), center, distanceToTheFarthestCorner + margin);
    }

    public String getWorldName() {
        return worldName;
    }

    public Center getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    public boolean contains(String otherWorldName, double x, double z) {
        return worldName.equals(otherWorldName) && center.distanceTo(x, z) <= radius;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return contains(location.getWorld().getName(), location.getX(), location.getZ());
    }

    public boolean containsBlock(String otherWorldName, int blockX, int blockZ) {
        return contains(otherWorldName, blockX + 0.5D, blockZ + 0.5D);
    }

    public double distanceToCenter(double x, double z) {
        return center.distanceTo(x, z);
    }

    public String describeDistanceOf(String otherWorldName, double x, double z) {
        if (!worldName.equals(otherWorldName)) {
            return "in the world " + otherWorldName + ", while the area lies in " + worldName + " with its "
                    + describe();
        }
        return formatBlocks(distanceToCenter(x, z)) + " blocks from its " + describe();
    }

    public String describe() {
        return "centre " + formatBlocks(center.x()) + "/" + formatBlocks(center.z())
                + ", radius " + formatBlocks(radius) + " blocks";
    }

    private static String formatBlocks(double blocks) {
        double roundedBlocks = Math.round(blocks * 10.0D) / 10.0D;
        if (roundedBlocks == Math.rint(roundedBlocks)) {
            return String.format(Locale.ROOT, "%d", (long) roundedBlocks);
        }
        return String.format(Locale.ROOT, "%.1f", roundedBlocks);
    }

    public record Center(double x, double z) {

        public double distanceTo(double otherX, double otherZ) {
            return Math.hypot(otherX - x, otherZ - z);
        }
    }
}
