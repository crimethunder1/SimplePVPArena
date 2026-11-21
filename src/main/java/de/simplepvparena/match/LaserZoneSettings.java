package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

/**
 * Settings of the laser ring that replaces the damage of every fighter once sudden death starts: poles rise along a
 * circle inside the arena, lasers connect them and the circle shrinks. Fighters touching the lasers or standing
 * outside of them take damage, the farther outside the more.
 */
@NullMarked
public record LaserZoneSettings(boolean enabled,
                                int shrinkSeconds,
                                double minimumRadius,
                                int damageIntervalSeconds,
                                double minimumDamage,
                                double maximumDamage,
                                double maximumDamageDistance) {

    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_SHRINK_SECONDS = 90;
    public static final double DEFAULT_MINIMUM_RADIUS = 3.0D;
    public static final int DEFAULT_DAMAGE_INTERVAL_SECONDS = 3;
    public static final double DEFAULT_MINIMUM_DAMAGE = 1.0D;
    public static final double DEFAULT_MAXIMUM_DAMAGE = 4.0D;
    public static final double DEFAULT_MAXIMUM_DAMAGE_DISTANCE = 6.0D;

    public static final double SMALLEST_RADIUS = 1.5D;
    public static final double SMALLEST_MAXIMUM_DAMAGE_DISTANCE = 0.5D;

    public LaserZoneSettings {
        shrinkSeconds = Math.max(1, shrinkSeconds);
        minimumRadius = Math.max(SMALLEST_RADIUS, minimumRadius);
        damageIntervalSeconds = Math.max(1, damageIntervalSeconds);
        minimumDamage = Math.max(0.0D, minimumDamage);
        maximumDamage = Math.max(minimumDamage, maximumDamage);
        maximumDamageDistance = Math.max(SMALLEST_MAXIMUM_DAMAGE_DISTANCE, maximumDamageDistance);
    }

    public static LaserZoneSettings defaults() {
        return new LaserZoneSettings(DEFAULT_ENABLED, DEFAULT_SHRINK_SECONDS, DEFAULT_MINIMUM_RADIUS,
                DEFAULT_DAMAGE_INTERVAL_SECONDS, DEFAULT_MINIMUM_DAMAGE, DEFAULT_MAXIMUM_DAMAGE,
                DEFAULT_MAXIMUM_DAMAGE_DISTANCE);
    }
}
