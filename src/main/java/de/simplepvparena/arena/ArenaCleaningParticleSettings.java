package de.simplepvparena.arena;

import de.simplepvparena.particle.ParticleNames;

public record ArenaCleaningParticleSettings(boolean enabled, String particleName,
                                            double totalDurationSeconds, int columnsPerTick) {

    public static final String SECTION_CONFIGURATION_PATH = "arena.clean-particle";
    public static final String ENABLED_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".enabled";
    public static final String TYPE_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".type";
    public static final String TOTAL_DURATION_CONFIGURATION_PATH =
            SECTION_CONFIGURATION_PATH + ".total-duration-seconds";
    public static final String COLUMNS_PER_TICK_CONFIGURATION_PATH =
            SECTION_CONFIGURATION_PATH + ".columns-per-tick";

    public static final String DEFAULT_PARTICLE_NAME = "DUST";

    public static final double DEFAULT_TOTAL_DURATION_SECONDS = 6.0D;

    public static final double MAXIMUM_TOTAL_DURATION_SECONDS = 120.0D;

    public static final int DEFAULT_COLUMNS_PER_TICK = 24;

    public static final int MAXIMUM_COLUMNS_PER_TICK = 64;

    private static final long TICKS_PER_SECOND = 20L;

    public ArenaCleaningParticleSettings {
        particleName = ParticleNames.normalize(particleName, DEFAULT_PARTICLE_NAME);
        totalDurationSeconds = normalizeDuration(totalDurationSeconds);
        columnsPerTick = Math.max(0, Math.min(MAXIMUM_COLUMNS_PER_TICK, columnsPerTick));
    }

    public static ArenaCleaningParticleSettings defaults() {
        return new ArenaCleaningParticleSettings(true, DEFAULT_PARTICLE_NAME,
                DEFAULT_TOTAL_DURATION_SECONDS, DEFAULT_COLUMNS_PER_TICK);
    }

    public boolean isShown() {
        return enabled && columnsPerTick > 0;
    }

    public long totalDurationTicks() {
        return Math.round(totalDurationSeconds * TICKS_PER_SECOND);
    }

    public long animationDurationTicks() {
        return isShown() ? totalDurationTicks() : 0L;
    }

    private static double normalizeDuration(double totalDurationSeconds) {
        if (!Double.isFinite(totalDurationSeconds)) {
            return DEFAULT_TOTAL_DURATION_SECONDS;
        }
        return Math.max(0.0D, Math.min(MAXIMUM_TOTAL_DURATION_SECONDS, totalDurationSeconds));
    }
}
