package de.simplepvparena.border;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfirmedBorderParticleSettingsTest {

    @Test
    void shipsAQuarterBlockHighEndRodStream() {
        ConfirmedBorderParticleSettings defaults = ConfirmedBorderParticleSettings.defaults();

        assertEquals("END_ROD", defaults.particleName());
        assertEquals(0.25D, defaults.height());
    }

    @Test
    void spreadsTheDotsOverTheConfiguredHeight() {
        ConfirmedBorderParticleSettings settings = settingsOfHeight(1.0D);

        double topDotOffset = (ConfirmedBorderParticleSettings.PARTICLE_COUNT - 1) * settings.streamStepHeight();

        assertEquals(1.0D, topDotOffset, 1.0E-9D, "the top dot does not sit at the configured height");
    }

    @Test
    void drawsASingleDotForAHeightOfZero() {
        assertEquals(1, settingsOfHeight(0.0D).drawnParticleCount());
        assertEquals(ConfirmedBorderParticleSettings.PARTICLE_COUNT, settingsOfHeight(0.25D).drawnParticleCount());
    }

    @Test
    void refusesANegativeHeight() {
        assertEquals(0.0D, settingsOfHeight(-3.0D).height());
    }

    @Test
    void fallsBackToTheDefaultHeightForAValueThatIsNoNumber() {
        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_HEIGHT, settingsOfHeight(Double.NaN).height());
        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_HEIGHT,
                settingsOfHeight(Double.POSITIVE_INFINITY).height());
    }

    @Test
    void acceptsAParticleNameInTheFormsItIsWrittenIn() {
        assertEquals("END_ROD", nameOf("end_rod"));
        assertEquals("END_ROD", nameOf("  End_Rod  "));
        assertEquals("END_ROD", nameOf("end-rod"));
        assertEquals("END_ROD", nameOf("end rod"));
        assertEquals("END_ROD", nameOf("minecraft:end_rod"));
    }

    @Test
    void fallsBackToTheDefaultNameForAnEmptyEntry() {
        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_PARTICLE_NAME, nameOf(null));
        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_PARTICLE_NAME, nameOf(""));
        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_PARTICLE_NAME, nameOf("   "));
    }

    @Test
    void clampsEveryColourChannelIntoItsRange() {
        ConfirmedBorderParticleSettings tooHigh = new ConfirmedBorderParticleSettings("DUST", 0.25D, 300, 300, 300);
        ConfirmedBorderParticleSettings tooLow = new ConfirmedBorderParticleSettings("DUST", 0.25D, -1, -20, -300);

        assertEquals(ConfirmedBorderParticleSettings.MAXIMUM_COLOUR_CHANNEL, tooHigh.red());
        assertEquals(ConfirmedBorderParticleSettings.MAXIMUM_COLOUR_CHANNEL, tooHigh.green());
        assertEquals(ConfirmedBorderParticleSettings.MAXIMUM_COLOUR_CHANNEL, tooHigh.blue());
        assertEquals(ConfirmedBorderParticleSettings.MINIMUM_COLOUR_CHANNEL, tooLow.red());
        assertEquals(ConfirmedBorderParticleSettings.MINIMUM_COLOUR_CHANNEL, tooLow.green());
        assertEquals(ConfirmedBorderParticleSettings.MINIMUM_COLOUR_CHANNEL, tooLow.blue());
    }

    private static String nameOf(String configuredName) {
        return new ConfirmedBorderParticleSettings(configuredName, 0.25D, 255, 255, 255).particleName();
    }

    private static ConfirmedBorderParticleSettings settingsOfHeight(double height) {
        return new ConfirmedBorderParticleSettings("END_ROD", height, 255, 255, 255);
    }
}
