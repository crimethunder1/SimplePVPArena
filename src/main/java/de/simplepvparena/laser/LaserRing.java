package de.simplepvparena.laser;

import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.RegionBoundary;
import de.simplepvparena.match.LaserZoneSettings;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * The shrinking circle of the laser zone: where it is after some time and how much a fighter at a position takes.
 */
@NullMarked
public final class LaserRing {

    /** Half the width of a player, a fighter touches the lasers once the body reaches them. */
    public static final double CONTACT_TOLERANCE = 0.3D;

    static final double MARGIN_TO_THE_ARENA_EDGE = 0.5D;
    static final double POLE_SPACING = 3.5D;
    static final int MINIMUM_POLE_COUNT = 8;
    static final int MAXIMUM_POLE_COUNT = 20;

    private static final long TICKS_PER_SECOND = 20L;

    private final double centreX;
    private final double centreZ;
    private final double startRadius;
    private final LaserZoneSettings settings;

    public LaserRing(double centreX, double centreZ, double startRadius, LaserZoneSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.centreX = centreX;
        this.centreZ = centreZ;
        this.startRadius = Math.max(settings.minimumRadius(), startRadius);
    }

    public double centreX() {
        return centreX;
    }

    public double centreZ() {
        return centreZ;
    }

    public double startRadius() {
        return startRadius;
    }

    public long shrinkTicks() {
        return settings.shrinkSeconds() * TICKS_PER_SECOND;
    }

    public long damageIntervalTicks() {
        return settings.damageIntervalSeconds() * TICKS_PER_SECOND;
    }

    /** Radius after the circle shrank for the given number of ticks, it stops at the minimum radius. */
    public double radiusAfter(long shrunkTicks) {
        double progress = Math.min(1.0D, Math.max(0.0D, (double) shrunkTicks / shrinkTicks()));
        return startRadius + (settings.minimumRadius() - startRadius) * progress;
    }

    /**
     * Damage of one hit for a fighter at the given position, empty while the fighter stays clear of the lasers.
     * Touching them gives the minimum damage, it grows in steps of half a heart up to the maximum damage at the
     * maximum damage distance outside the circle.
     */
    public OptionalDouble damageAt(double x, double z, double radius) {
        double distance = Math.hypot(x - centreX, z - centreZ);
        double overshoot = distance - (radius - CONTACT_TOLERANCE);
        if (overshoot < 0.0D) {
            return OptionalDouble.empty();
        }
        double share = Math.min(1.0D, overshoot / settings.maximumDamageDistance());
        double damage = settings.minimumDamage() + (settings.maximumDamage() - settings.minimumDamage()) * share;
        double inHalfHearts = Math.round(damage);
        return OptionalDouble.of(Math.max(settings.minimumDamage(), Math.min(settings.maximumDamage(), inHalfHearts)));
    }

    public static int poleCountFor(double radius) {
        int count = (int) Math.round(2.0D * Math.PI * radius / POLE_SPACING);
        return Math.max(MINIMUM_POLE_COUNT, Math.min(MAXIMUM_POLE_COUNT, count));
    }

    /**
     * Radius of the largest circle around the centre that stays on the arena floor, so the poles stand inside the
     * arena and not in its wall or on the tribune.
     */
    public static double largestRadiusInside(ArenaShape shape, double centreX, double centreZ) {
        RegionBoundary boundary = shape.getBoundingBox();
        double closestOutside = Double.MAX_VALUE;
        for (int blockX = boundary.getMinimumX() - 1; blockX <= boundary.getMaximumX() + 1; blockX++) {
            for (int blockZ = boundary.getMinimumZ() - 1; blockZ <= boundary.getMaximumZ() + 1; blockZ++) {
                if (shape.containsCell(blockX, blockZ)) {
                    continue;
                }
                double nearestX = Math.max(blockX, Math.min(centreX, blockX + 1.0D));
                double nearestZ = Math.max(blockZ, Math.min(centreZ, blockZ + 1.0D));
                closestOutside = Math.min(closestOutside, Math.hypot(nearestX - centreX, nearestZ - centreZ));
            }
        }
        return Math.max(0.0D, closestOutside - MARGIN_TO_THE_ARENA_EDGE);
    }
}
