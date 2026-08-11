package de.simplepvparena.arena;

import de.simplepvparena.particle.ParticleNames;

public record ArenaCleaningParticleSettings(boolean enabled, String particleName, double height,
                                            int columnsPerTick) {

    public static final String SECTION_CONFIGURATION_PATH = "arena.clean-particle";
    public static final String ENABLED_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".enabled";
    public static final String TYPE_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".type";
    public static final String HEIGHT_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".height";
    public static final String COLUMNS_PER_TICK_CONFIGURATION_PATH =
            SECTION_CONFIGURATION_PATH + ".columns-per-tick";

    public static final String DEFAULT_PARTICLE_NAME = "WITCH";

    public static final double DEFAULT_HEIGHT = 20.0D;

    public static final int DEFAULT_COLUMNS_PER_TICK = 6;

    public static final double DOT_DISTANCE = 2.5D;

    public static final double MAXIMUM_HEIGHT = 256.0D;

    public static final int MAXIMUM_COLUMNS_PER_TICK = 64;

    public ArenaCleaningParticleSettings {
        particleName = ParticleNames.normalize(particleName, DEFAULT_PARTICLE_NAME);
        height = normalizeHeight(height);
        columnsPerTick = Math.max(0, Math.min(MAXIMUM_COLUMNS_PER_TICK, columnsPerTick));
    }

    public static ArenaCleaningParticleSettings defaults() {
        return new ArenaCleaningParticleSettings(true, DEFAULT_PARTICLE_NAME, DEFAULT_HEIGHT,
                DEFAULT_COLUMNS_PER_TICK);
    }

    public boolean isShown() {
        return enabled && columnsPerTick > 0;
    }

    public int dotCount() {
        return (int) (height / DOT_DISTANCE) + 1;
    }

    private static double normalizeHeight(double height) {
        if (!Double.isFinite(height)) {
            return DEFAULT_HEIGHT;
        }
        return Math.max(0.0D, Math.min(MAXIMUM_HEIGHT, height));
    }
}
