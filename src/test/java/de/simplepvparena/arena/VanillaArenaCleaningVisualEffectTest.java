package de.simplepvparena.arena;

import de.simplepvparena.particle.ParticleTypes;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaArenaCleaningVisualEffectTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsAParticleThatCarriesTheColourOfThePhases() {
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE.name());
        assertTrue(ParticleTypes.carriesColour(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE));

        VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();
        effect.applyParticleSettings(ArenaCleaningParticleSettings.defaults(), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, effect.getParticle());
        assertEquals(Color.fromRGB(ArenaCleaningPhase.FLOOD.red(), ArenaCleaningPhase.FLOOD.green(),
                        ArenaCleaningPhase.FLOOD.blue()),
                effect.findColour(ArenaCleaningPhase.FLOOD).orElseThrow().getColor());
        assertEquals(Color.fromRGB(ArenaCleaningPhase.CLEAR.red(), ArenaCleaningPhase.CLEAR.green(),
                        ArenaCleaningPhase.CLEAR.blue()),
                effect.findColour(ArenaCleaningPhase.CLEAR).orElseThrow().getColor());
        assertEquals(List.of(), problems, "the shipped default must never report a problem");
    }

    @Test
    void announcesInRedAndClearsInGreen() {
        assertTrue(ArenaCleaningPhase.FLOOD.red() > ArenaCleaningPhase.FLOOD.green()
                        && ArenaCleaningPhase.FLOOD.red() > ArenaCleaningPhase.FLOOD.blue(),
                "the announcing phase is red");
        assertTrue(ArenaCleaningPhase.CLEAR.green() > ArenaCleaningPhase.CLEAR.red()
                        && ArenaCleaningPhase.CLEAR.green() > ArenaCleaningPhase.CLEAR.blue(),
                "the clearing phase is green");
        assertFalse(ArenaCleaningPhase.FLOOD.writesBlocks(), "the announcement changes nothing at all");
        assertTrue(ArenaCleaningPhase.CLEAR.writesBlocks());
    }

    @Test
    void takesEveryParticleTheConfigurationOffers() {
        for (String particleName : List.of("DUST", "WITCH", "END_ROD", "FLAME", "COMPOSTER",
                "SOUL_FIRE_FLAME", "CRIT")) {
            VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();

            effect.applyParticleSettings(settingsWith(particleName), problems::add);

            assertEquals(particleName, effect.getParticle().name(),
                    particleName + " is offered by config.yml but cannot be drawn: " + problems);
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void leavesAColourlessParticleWithoutTheColourOfThePhases() {
        VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();

        effect.applyParticleSettings(settingsWith("WITCH"), problems::add);

        assertEquals(Particle.WITCH, effect.getParticle());
        assertTrue(effect.findColour(ArenaCleaningPhase.FLOOD).isEmpty());
        assertTrue(effect.findColour(ArenaCleaningPhase.CLEAR).isEmpty());
        assertEquals(List.of(), problems, "a colourless particle is a choice, not a broken entry");
    }

    @Test
    void fallsBackAndReportsAParticleThatCannotBeDrawn() {
        VanillaArenaCleaningVisualEffect unknownName = new VanillaArenaCleaningVisualEffect();
        unknownName.applyParticleSettings(settingsWith("NOT_A_PARTICLE"), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, unknownName.getParticle());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH),
                "the problem has to name the entry an admin fixes: " + problems);
        assertTrue(problems.get(0).contains("no particle of this server version"), problems.get(0));

        problems.clear();
        VanillaArenaCleaningVisualEffect needsData = new VanillaArenaCleaningVisualEffect();
        needsData.applyParticleSettings(settingsWith("ITEM"), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, needsData.getParticle(),
                "a cleaning has no item to give ITEM");
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("needs extra data"), problems.get(0));
    }

    @Test
    void appliesTheWholeSettingsAgainOnEveryReload() {
        VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();

        effect.applyParticleSettings(new ArenaCleaningParticleSettings(false, "flame", 3.0D, 2), problems::add);

        assertEquals(Particle.FLAME, effect.getParticle());
        assertEquals(new ArenaCleaningParticleSettings(false, "FLAME", 3.0D, 2), effect.getSettings());
        assertTrue(effect.findColour(ArenaCleaningPhase.FLOOD).isEmpty());

        effect.applyParticleSettings(ArenaCleaningParticleSettings.defaults(), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, effect.getParticle());
        assertEquals(ArenaCleaningParticleSettings.defaults(), effect.getSettings());
        assertTrue(effect.findColour(ArenaCleaningPhase.FLOOD).isPresent(),
                "the colours come back with the particle that carries them");
        assertEquals(List.of(), problems);
    }

    private static ArenaCleaningParticleSettings settingsWith(String particleName) {
        return new ArenaCleaningParticleSettings(true, particleName,
                ArenaCleaningParticleSettings.DEFAULT_TOTAL_DURATION_SECONDS,
                ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK);
    }
}
