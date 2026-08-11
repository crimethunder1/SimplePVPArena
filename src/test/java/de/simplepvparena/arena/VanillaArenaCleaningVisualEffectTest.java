package de.simplepvparena.arena;

import de.simplepvparena.particle.ParticleTypes;
import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VanillaArenaCleaningVisualEffectTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsAParticleThatNeedsNoExtraData() {
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE.name());
        assertTrue(ParticleTypes.carriesNoData(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE));

        VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();
        effect.applyParticleSettings(ArenaCleaningParticleSettings.defaults(), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, effect.getParticle());
        assertEquals(List.of(), problems, "the shipped default must never report a problem");
    }

    @Test
    void takesEveryParticleTheConfigurationOffers() {
        for (String particleName : List.of("WITCH", "END_ROD", "FLAME", "COMPOSTER", "SOUL_FIRE_FLAME", "CRIT")) {
            VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();

            effect.applyParticleSettings(settingsWith(particleName), problems::add);

            assertEquals(particleName, effect.getParticle().name(),
                    particleName + " is offered by config.yml but cannot be drawn: " + problems);
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackAndReportsAParticleThatCannotBeAColumn() {
        VanillaArenaCleaningVisualEffect unknownName = new VanillaArenaCleaningVisualEffect();
        unknownName.applyParticleSettings(settingsWith("NOT_A_PARTICLE"), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, unknownName.getParticle());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(ArenaCleaningParticleSettings.TYPE_CONFIGURATION_PATH),
                "the problem has to name the entry an admin fixes: " + problems);
        assertTrue(problems.get(0).contains("no particle of this server version"), problems.get(0));

        problems.clear();
        VanillaArenaCleaningVisualEffect needsData = new VanillaArenaCleaningVisualEffect();
        needsData.applyParticleSettings(settingsWith("DUST"), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, needsData.getParticle(),
                "a cleaning column has no colour to give DUST");
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("needs extra data"), problems.get(0));
    }

    @Test
    void appliesTheWholeSettingsAgainOnEveryReload() {
        VanillaArenaCleaningVisualEffect effect = new VanillaArenaCleaningVisualEffect();

        effect.applyParticleSettings(new ArenaCleaningParticleSettings(false, "flame", 6.0D, 2), problems::add);

        assertEquals(Particle.FLAME, effect.getParticle());
        assertEquals(new ArenaCleaningParticleSettings(false, "FLAME", 6.0D, 2), effect.getSettings());

        effect.applyParticleSettings(ArenaCleaningParticleSettings.defaults(), problems::add);

        assertEquals(VanillaArenaCleaningVisualEffect.DEFAULT_PARTICLE, effect.getParticle());
        assertEquals(ArenaCleaningParticleSettings.defaults(), effect.getSettings());
        assertEquals(List.of(), problems);
    }

    private static ArenaCleaningParticleSettings settingsWith(String particleName) {
        return new ArenaCleaningParticleSettings(true, particleName,
                ArenaCleaningParticleSettings.DEFAULT_HEIGHT,
                ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK);
    }
}
