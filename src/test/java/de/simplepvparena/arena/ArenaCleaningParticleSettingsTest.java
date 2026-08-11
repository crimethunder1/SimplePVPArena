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
    void answersAHeightThatCannotBeOne() {
        assertEquals(0.0D, settingsWithHeight(-4.0D).height());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_HEIGHT, settingsWithHeight(Double.NaN).height());
        assertEquals(ArenaCleaningParticleSettings.MAXIMUM_HEIGHT, settingsWithHeight(100000.0D).height(),
                "a height typed with one zero too many is capped instead of costing packets by the thousand");
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_HEIGHT,
                settingsWithHeight(Double.POSITIVE_INFINITY).height(),
                "an entry that is no number at all is a broken one, not the tallest column there is");
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
        assertFalse(new ArenaCleaningParticleSettings(false, "WITCH", 20.0D, 6).isShown());
    }

    @Test
    void spendsOneDotEveryFixedDistanceAndAlwaysAtLeastOne() {
        assertEquals(9, settingsWithHeight(20.0D).dotCount());
        assertEquals(1, settingsWithHeight(0.0D).dotCount(), "the shortest column is a single dot");
        assertEquals(1, settingsWithHeight(-1.0D).dotCount());
        assertEquals(3, settingsWithHeight(2 * ArenaCleaningParticleSettings.DOT_DISTANCE).dotCount());
    }

    @Test
    void shipsAnEffectThatIsDrawn() {
        ArenaCleaningParticleSettings defaults = ArenaCleaningParticleSettings.defaults();

        assertTrue(defaults.enabled());
        assertTrue(defaults.isShown());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME, defaults.particleName());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_HEIGHT, defaults.height());
        assertEquals(ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK, defaults.columnsPerTick());
    }

    private static ArenaCleaningParticleSettings settingsWithParticleName(String particleName) {
        return new ArenaCleaningParticleSettings(true, particleName,
                ArenaCleaningParticleSettings.DEFAULT_HEIGHT,
                ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK);
    }

    private static ArenaCleaningParticleSettings settingsWithHeight(double height) {
        return new ArenaCleaningParticleSettings(true, ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                height, ArenaCleaningParticleSettings.DEFAULT_COLUMNS_PER_TICK);
    }

    private static ArenaCleaningParticleSettings settingsWithColumnsPerTick(int columnsPerTick) {
        return new ArenaCleaningParticleSettings(true, ArenaCleaningParticleSettings.DEFAULT_PARTICLE_NAME,
                ArenaCleaningParticleSettings.DEFAULT_HEIGHT, columnsPerTick);
    }
}
