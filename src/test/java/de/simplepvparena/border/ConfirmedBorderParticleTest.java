package de.simplepvparena.border;

import de.simplepvparena.particle.ParticleTypes;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmedBorderParticleTest {

    @Test
    void takesTheConfiguredParticle() {
        List<String> problems = new ArrayList<>();

        ConfirmedBorderParticle particle = ConfirmedBorderParticle.from(settingsOfType("FLAME"), problems::add);

        assertEquals(Particle.FLAME, particle.getParticle());
        assertTrue(problems.isEmpty(), "a valid particle name was reported as a problem: " + problems);
    }

    @Test
    void fallsBackToTheDefaultForANameThatIsNoParticle() {
        List<String> problems = new ArrayList<>();

        ConfirmedBorderParticle particle = ConfirmedBorderParticle.from(settingsOfType("NOT_A_PARTICLE"),
                problems::add);

        assertEquals(ConfirmedBorderParticle.DEFAULT_PARTICLE, particle.getParticle());
        assertEquals(1, problems.size(), "the unknown particle name was not reported exactly once");
        assertTrue(problems.getFirst().contains("NOT_A_PARTICLE"), "the report does not name the entry");
        assertTrue(problems.getFirst().contains(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH),
                "the report does not name the setting to fix");
    }

    @Test
    void fallsBackToTheDefaultForAParticleThatNeedsExtraData() {
        List<String> problems = new ArrayList<>();

        ConfirmedBorderParticle particle = ConfirmedBorderParticle.from(settingsOfType("ITEM"), problems::add);

        assertEquals(ConfirmedBorderParticle.DEFAULT_PARTICLE, particle.getParticle());
        assertEquals(1, problems.size(), "the unusable particle was not reported exactly once");
        assertTrue(problems.getFirst().contains(Particle.ITEM.name()), "the report does not name the particle");
    }

    @Test
    void coloursTheParticleThatCarriesAColour() {
        ConfirmedBorderParticle particle = ConfirmedBorderParticle.from(
                new ConfirmedBorderParticleSettings("DUST", 0.25D, 10, 20, 30), problem -> {
                });

        assertEquals(Particle.DUST, particle.getParticle());
        assertEquals(Optional.of(Color.fromRGB(10, 20, 30)), particle.getColour().map(Particle.DustOptions::getColor));
    }

    @Test
    void ignoresTheColourOfAParticleThatCarriesNone() {
        List<String> problems = new ArrayList<>();

        ConfirmedBorderParticle particle = ConfirmedBorderParticle.from(
                new ConfirmedBorderParticleSettings("END_ROD", 0.25D, 10, 20, 30), problems::add);

        assertTrue(particle.getColour().isEmpty(), "a colourless particle was given a colour");
        assertTrue(problems.isEmpty(), "the ignored colour was reported as a problem: " + problems);
    }

    @Test
    void supportsEveryParticleTheConfigurationOffers() {
        for (String particleName : List.of("END_ROD", "FLAME", "ELECTRIC_SPARK", "SOUL_FIRE_FLAME", "CRIT", "DUST")) {
            Optional<Particle> particle = ParticleTypes.byName(particleName);
            assertTrue(particle.isPresent(), particleName + " is no particle of this server version");
            assertTrue(ConfirmedBorderParticle.isSupported(particle.orElseThrow()),
                    particleName + " is offered by config.yml but cannot be drawn");
        }
    }

    @Test
    void tellsTheColourlessParticlesFromTheColouredOne() {
        assertTrue(ParticleTypes.carriesNoData(ConfirmedBorderParticle.DEFAULT_PARTICLE));
        assertFalse(ConfirmedBorderParticle.carriesColour(ConfirmedBorderParticle.DEFAULT_PARTICLE));
        assertTrue(ConfirmedBorderParticle.carriesColour(Particle.DUST));
        assertFalse(ParticleTypes.carriesNoData(Particle.DUST));
    }

    @Test
    void takesTheShapeOfTheStreamFromTheSettings() {
        ConfirmedBorderParticle defaults = ConfirmedBorderParticle.defaults();

        assertEquals(ConfirmedBorderParticleSettings.defaults().streamStepHeight(), defaults.getStepHeight());
        assertEquals(ConfirmedBorderParticleSettings.PARTICLE_COUNT, defaults.getParticleCount());
    }

    private static ConfirmedBorderParticleSettings settingsOfType(String particleName) {
        return new ConfirmedBorderParticleSettings(particleName, 0.25D, 255, 255, 255);
    }
}
