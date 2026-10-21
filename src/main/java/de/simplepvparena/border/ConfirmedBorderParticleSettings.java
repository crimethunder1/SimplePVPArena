package de.simplepvparena.border;

import de.simplepvparena.particle.ParticleNames;

public record ConfirmedBorderParticleSettings(String particleName, double height, int red, int green, int blue) {

    public static final String SECTION_CONFIGURATION_PATH = "border.confirmed-particle";
    public static final String ENABLED_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".enabled";
    public static final String TYPE_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".type";
    public static final String HEIGHT_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".height";
    public static final String RED_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".colour.red";
    public static final String GREEN_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".colour.green";
    public static final String BLUE_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".colour.blue";

    public static final String DEFAULT_PARTICLE_NAME = "END_ROD";

    public static final double DEFAULT_HEIGHT = 0.25D;

    public static final int DEFAULT_RED = 255;
    public static final int DEFAULT_GREEN = 255;
    public static final int DEFAULT_BLUE = 255;

    public static final int MINIMUM_COLOUR_CHANNEL = 0;
    public static final int MAXIMUM_COLOUR_CHANNEL = 255;

    public static final int PARTICLE_COUNT = 3;

    public ConfirmedBorderParticleSettings {
        particleName = ParticleNames.normalize(particleName, DEFAULT_PARTICLE_NAME);
        height = normalizeHeight(height);
        red = clampColourChannel(red);
        green = clampColourChannel(green);
        blue = clampColourChannel(blue);
    }

    public static ConfirmedBorderParticleSettings defaults() {
        return new ConfirmedBorderParticleSettings(DEFAULT_PARTICLE_NAME, DEFAULT_HEIGHT,
                DEFAULT_RED, DEFAULT_GREEN, DEFAULT_BLUE);
    }

    public double streamStepHeight() {
        return height / (PARTICLE_COUNT - 1);
    }

    public int drawnParticleCount() {
        return height > 0.0D ? PARTICLE_COUNT : 1;
    }

    private static double normalizeHeight(double height) {
        if (!Double.isFinite(height)) {
            return DEFAULT_HEIGHT;
        }
        return Math.max(0.0D, height);
    }

    private static int clampColourChannel(int channel) {
        return Math.max(MINIMUM_COLOUR_CHANNEL, Math.min(MAXIMUM_COLOUR_CHANNEL, channel));
    }
}
