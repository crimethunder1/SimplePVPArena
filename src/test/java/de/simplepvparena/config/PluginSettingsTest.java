package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaSafetyZone;
import de.simplepvparena.arena.ArenaShapeFloodFill;
import de.simplepvparena.border.BorderParticleService;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.RoundTimeoutSettings;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void readsEveryValueOfTheFile() {
        MemoryConfiguration configuration = configurationWithEveryValueChanged();

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(List.of(), problems, "a fully valid file must not report a single problem");
        assertEquals(60L, settings.roundResetDelayTicks());
        assertEquals(new RoundTimeoutSettings(120, 30, 3.5D, 0.5D), settings.roundTimeoutSettings());
        assertEquals(1024, settings.arenaCleaningBlocksPerTick());
        assertEquals(2048, settings.arenaBaselineBlocksPerTick());
        assertEquals(12345, settings.maximumArenaCellCount());
        assertEquals(17, settings.safetyZoneRadius());
        assertEquals(11L, settings.borderParticlePeriodTicks());
        assertEquals(7, settings.borderParticleViewRadius());
        assertEquals(9, settings.borderParticleMaximumBlocksPerPlayer());
        assertEquals(13, settings.borderParticleMaximumLinesPerPlayer());
        assertEquals(6L * PluginSettings.TICKS_PER_SECOND, settings.borderWallPreviewDurationTicks());
        assertEquals(new ConfirmedBorderParticleSettings("DUST", 1.5D, 10, 20, 30),
                settings.confirmedBorderParticleSettings());
    }

    @Test
    void everyValueOfTheFileDiffersFromTheShippedDefault() {
        PluginSettings shipped = PluginSettings.defaults();

        PluginSettings settings = PluginSettings.read(configurationWithEveryValueChanged(), problems::add);

        assertTrue(shipped.roundResetDelayTicks() != settings.roundResetDelayTicks());
        assertTrue(!shipped.roundTimeoutSettings().equals(settings.roundTimeoutSettings()));
        assertTrue(shipped.arenaCleaningBlocksPerTick() != settings.arenaCleaningBlocksPerTick());
        assertTrue(shipped.arenaBaselineBlocksPerTick() != settings.arenaBaselineBlocksPerTick());
        assertTrue(shipped.maximumArenaCellCount() != settings.maximumArenaCellCount());
        assertTrue(shipped.safetyZoneRadius() != settings.safetyZoneRadius());
        assertTrue(shipped.borderParticlePeriodTicks() != settings.borderParticlePeriodTicks());
        assertTrue(shipped.borderParticleViewRadius() != settings.borderParticleViewRadius());
        assertTrue(shipped.borderParticleMaximumBlocksPerPlayer() != settings.borderParticleMaximumBlocksPerPlayer());
        assertTrue(shipped.borderParticleMaximumLinesPerPlayer() != settings.borderParticleMaximumLinesPerPlayer());
        assertTrue(shipped.borderWallPreviewDurationTicks() != settings.borderWallPreviewDurationTicks());
        assertTrue(!shipped.confirmedBorderParticleSettings().equals(settings.confirmedBorderParticleSettings()));
    }

    @Test
    void anEmptyFileIsTheShippedDefault() {
        PluginSettings settings = PluginSettings.read(new MemoryConfiguration(), problems::add);

        assertEquals(List.of(), problems);
        assertEquals(PluginSettings.defaults(), settings);
        assertEquals(ArenaCleaningService.DEFAULT_BLOCKS_PER_TICK, settings.arenaCleaningBlocksPerTick());
        assertEquals(ArenaBaselineService.DEFAULT_BLOCKS_PER_TICK, settings.arenaBaselineBlocksPerTick());
        assertEquals(ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, settings.maximumArenaCellCount());
        assertEquals(ArenaSafetyZone.DEFAULT_RADIUS, settings.safetyZoneRadius());
        assertEquals(BorderParticleService.DEFAULT_PERIOD_TICKS, settings.borderParticlePeriodTicks());
    }

    @Test
    void aFileThatIsNotThereAtAllIsTheShippedDefault() {
        assertEquals(PluginSettings.defaults(), PluginSettings.read(null, problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void aCellLimitBelowOneCellFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, 0);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(ArenaShapeFloodFill.DEFAULT_MAXIMUM_CELL_COUNT, settings.maximumArenaCellCount());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.ARENA_MAXIMUM_CELLS_PATH),
                "the problem has to name the entry an admin fixes: " + problems.get(0));
    }

    @Test
    void aNegativeSafetyZoneRadiusFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(ArenaSafetyZone.DEFAULT_RADIUS, settings.safetyZoneRadius());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH), problems.get(0));
    }

    @Test
    void aSafetyZoneRadiusOfZeroSwitchesTheZoneOffWithoutAProblem() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, 0);

        assertEquals(0, PluginSettings.read(configuration, problems::add).safetyZoneRadius());
        assertEquals(List.of(), problems);
    }

    @Test
    void aRoundThatIsOverBeforeItStartsFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, 0);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(RoundTimeoutSettings.DEFAULT_ROUND_TIMEOUT_SECONDS,
                settings.roundTimeoutSettings().roundTimeoutSeconds());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH), problems.get(0));
    }

    @Test
    void aNegativeParticleHeightFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, -0.5D);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(ConfirmedBorderParticleSettings.DEFAULT_HEIGHT,
                settings.confirmedBorderParticleSettings().height());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH),
                problems.get(0));
    }

    @Test
    void aParticleHeightOfZeroIsTheShortestMarkerAndNoProblem() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, 0.0D);

        assertEquals(0.0D, PluginSettings.read(configuration, problems::add)
                .confirmedBorderParticleSettings().height());
        assertEquals(List.of(), problems);
    }

    @Test
    void everyBrokenEntryOfOneFileIsReportedAndAnswered() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, -5);
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -5);
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, -5);
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, -5.0D);
        configuration.set(PluginSettings.BORDER_PARTICLE_VIEW_RADIUS_PATH, 21);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(4, problems.size(), "every broken entry is reported: " + problems);
        assertEquals(PluginSettings.defaults().maximumArenaCellCount(), settings.maximumArenaCellCount());
        assertEquals(PluginSettings.defaults().safetyZoneRadius(), settings.safetyZoneRadius());
        assertEquals(PluginSettings.defaults().roundTimeoutSettings(), settings.roundTimeoutSettings());
        assertEquals(PluginSettings.defaults().confirmedBorderParticleSettings(),
                settings.confirmedBorderParticleSettings());
        assertEquals(21, settings.borderParticleViewRadius(), "the usable entries of the same file still arrive");
    }

    @Test
    void anEntryThatIsMissingKeepsItsDefaultNextToTheOnesThatAreThere() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.BORDER_PARTICLE_MAXIMUM_LINES_PATH, 0);

        PluginSettings settings = PluginSettings.read(configuration, problems::add);

        assertEquals(0, settings.borderParticleMaximumLinesPerPlayer());
        assertEquals(PluginSettings.defaults().borderParticleMaximumBlocksPerPlayer(),
                settings.borderParticleMaximumBlocksPerPlayer());
        assertEquals(List.of(), problems);
    }

    static MemoryConfiguration configurationWithEveryValueChanged() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ROUND_RESET_DELAY_PATH, 60);
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, 120);
        configuration.set(PluginSettings.SUDDEN_DEATH_WARNING_SECONDS_PATH, 30);
        configuration.set(PluginSettings.SUDDEN_DEATH_DAMAGE_PER_SECOND_PATH, 3.5D);
        configuration.set(PluginSettings.SUDDEN_DEATH_DAMAGE_INCREASE_PATH, 0.5D);
        configuration.set(PluginSettings.ARENA_CLEAN_BLOCKS_PER_TICK_PATH, 1024);
        configuration.set(PluginSettings.ARENA_BASELINE_BLOCKS_PER_TICK_PATH, 2048);
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, 12345);
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, 17);
        configuration.set(PluginSettings.BORDER_PARTICLE_PERIOD_PATH, 11);
        configuration.set(PluginSettings.BORDER_PARTICLE_VIEW_RADIUS_PATH, 7);
        configuration.set(PluginSettings.BORDER_PARTICLE_MAXIMUM_BLOCKS_PATH, 9);
        configuration.set(PluginSettings.BORDER_PARTICLE_MAXIMUM_LINES_PATH, 13);
        configuration.set(PluginSettings.BORDER_WALL_PREVIEW_SECONDS_PATH, 6);
        configuration.set(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH, "dust");
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, 1.5D);
        configuration.set(ConfirmedBorderParticleSettings.RED_CONFIGURATION_PATH, 10);
        configuration.set(ConfirmedBorderParticleSettings.GREEN_CONFIGURATION_PATH, 20);
        configuration.set(ConfirmedBorderParticleSettings.BLUE_CONFIGURATION_PATH, 30);
        configuration.set(PluginSettings.KIT_SECTION_PATH + ".items", List.of());
        return configuration;
    }
}
