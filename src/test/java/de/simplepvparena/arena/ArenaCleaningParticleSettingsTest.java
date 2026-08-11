package de.simplepvparena.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArenaCleaningParticleSettingsTest {

    @Test
    void readsAParticleNameInEveryFormItIsWrittenIn() {
        assertEquals("WITCH", settingsWithParticleName("minecraft:witch").particleName());
        assertEquals("SOUL_FIRE_FLAME", settingsWithParticleName(" soul-fire-flame ").particleName());
        assertEquals("SOUL_FIRE_FLAME", settingsWithParticleName("soul fire flame").particleName());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                settingsWithParticleName(null).particleName());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                settingsWithParticleName("   ").particleName());
    }

    @Test
    void answersATimeThatCannotBeOne() {
        assertEquals(0.0D, settingsWithDuration(-4.0D).totalDurationSeconds());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_TOTAL_DURATION_SECONDS,
                settingsWithDuration(Double.NaN).totalDurationSeconds());
        assertEquals(ArenaCleaningParticleSettings.MAXIMUM_TOTAL_DURATION_SECONDS,
                settingsWithDuration(100000.0D).totalDurationSeconds(),
                "a time typed with one zero too many is capped instead of blocking the arena for an hour");
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_TOTAL_DURATION_SECONDS,
                settingsWithDuration(Double.POSITIVE_INFINITY).totalDurationSeconds(),
                "an entry that is no number at all is a broken one, not the longest animation there is");
    }

    @Test
    void keepsTheRateInsideItsLimits() {
        assertEquals(0, settingsWithColumnsPerTick(-2).columnsPerTick(), "a negative rate draws nothing");
        assertEquals(ArenaCleaningParticleSettings.MAXIMUM_COLUMNS_PER_TICK,
                settingsWithColumnsPerTick(10000).columnsPerTick());
        assertEquals(12, settingsWithColumnsPerTick(12).columnsPerTick());
    }

    @Test
    void isShownOnlyWhileThereIsSomethingToDraw() {
        assertTrue(ArenaCleaningParticleSettings.defaults().isShown());
        assertFalse(settingsWithColumnsPerTick(0).isShown());
        assertFalse(new ArenaCleaningParticleSettings(false, "DUST", 6.0D, 24).isShown());
    }

    @Test
    void turnsTheConfiguredSecondsIntoTicks() {
        assertEquals(120L, settingsWithDuration(6.0D).totalDurationTicks());
        assertEquals(10L, settingsWithDuration(0.5D).totalDurationTicks());
        assertEquals(1L, settingsWithDuration(0.04D).totalDurationTicks(),
                "a time shorter than a tick is rounded to the shortest thing a server can show");
        assertEquals(0L, settingsWithDuration(0.0D).totalDurationTicks());
    }

    @Test
    void pacesTheCleaningOnlyWhileTheAnimationIsDrawn() {
        assertEquals(120L, ArenaCleaningParticleSettings.defaults().animationDurationTicks());
        assertEquals(0L, new ArenaCleaningParticleSettings(false, "DUST", 6.0D, 24).animationDurationTicks(),
                "an effect that is switched off holds no layer back");
        assertEquals(0L, settingsWithColumnsPerTick(0).animationDurationTicks(),
                "and neither does one that draws no cell");
        assertEquals(0L, settingsWithDuration(0.0D).animationDurationTicks(),
                "a time of zero is the cleaning without the two phases");
    }

    @Test
    void shipsAnEffectThatIsDrawn() {
        ArenaCleaningParticleSettings defaults = ArenaCleaningParticleSettings.defaults();

        assertTrue(defaults.enabled());
        assertTrue(defaults.isShown());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME, defaults.particleName());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_TOTAL_DURATION_SECONDS,
                defaults.totalDurationSeconds());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK, defaults.columnsPerTick());
    }

    private static ArenaCleaningParticleSettings settingsWithParticleName(String particleName) {
        return new ArenaCleaningParticleSettings(true, particleName,
                ArenaCleaningParticleSettings.DEFAULT_TOTAL_DURATION_SECONDS,
                ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK);
    }

    private static ArenaCleaningParticleSettings settingsWithDuration(double totalDurationSeconds) {
        return new ArenaCleaningParticleSettings(true, ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                totalDurationSeconds, ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK);
    }

    private static ArenaCleaningParticleSettings settingsWithColumnsPerTick(int columnsPerTick) {
        return new ArenaCleaningParticleSettings(true, ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                ArenaCleaningParticleSettings.DEFAULT_TOTAL_DURATION_SECONDS, columnsPerTick);
    }
}
