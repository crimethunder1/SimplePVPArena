package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.RoundTimeoutSettings;
import de.simplepvparena.match.TeamColourSettings;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationReloadServiceTest {

    private final RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
    private final List<String> loggedProblems = new ArrayList<>();

    @Test
    void handsEverySettingOfTheFileToTheRunningPlugin() {
        ConfigurationReloadService reloadService = reloadServiceFor(
                PluginSettingsTest.configurationWithEveryValueChanged());

        ConfigurationReloadReport report = reloadService.reloadFromFile();

        assertTrue(report.isClean(), "a valid file reports no problem: " + report.problems());
        assertEquals(60L, liveConfiguration.longValueOf(RecordingLiveConfiguration.ROUND_RESET_DELAY));
        assertEquals(new RoundTimeoutSettings(120, 30, 3.5D, 0.5D), liveConfiguration.roundTimeoutSettings());
        assertEquals(new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE),
                liveConfiguration.teamColourSettings());
        assertEquals(1024, liveConfiguration.intValueOf(RecordingLiveConfiguration.CLEANING_BLOCKS_PER_TICK));
        assertEquals(2048, liveConfiguration.intValueOf(RecordingLiveConfiguration.BASELINE_BLOCKS_PER_TICK));
        assertEquals(new ArenaCleaningParticleSettings(false, "COMPOSTER", 8.0D, 3),
                liveConfiguration.arenaCleaningParticleSettings());
        assertEquals(12345, liveConfiguration.intValueOf(RecordingLiveConfiguration.MAXIMUM_CELL_COUNT));
        assertEquals(17, liveConfiguration.intValueOf(RecordingLiveConfiguration.SAFETY_ZONE_RADIUS));
        assertEquals(11L, liveConfiguration.longValueOf(RecordingLiveConfiguration.PARTICLE_PERIOD));
        assertEquals(7, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_VIEW_RADIUS));
        assertEquals(9, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_MAXIMUM_BLOCKS));
        assertEquals(13, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_MAXIMUM_LINES));
        assertEquals(6L * PluginSettings.TICKS_PER_SECOND,
                liveConfiguration.longValueOf(RecordingLiveConfiguration.WALL_PREVIEW_DURATION));
        assertEquals(new ConfirmedBorderParticleSettings("DUST", 1.5D, 10, 20, 30),
                liveConfiguration.confirmedBorderParticleSettings());
    }

    @Test
    void reloadsTheKitOutOfTheSameFile() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.KIT_SECTION_PATH + ".armor.head.material", "DIAMOND_HELMET");
        configuration.set(PluginSettings.KIT_SECTION_PATH + ".items", List.of());

        reloadServiceFor(configuration).reloadFromFile();

        assertEquals(PluginSettings.KIT_SECTION_PATH,
                liveConfiguration.valueOf(RecordingLiveConfiguration.KIT));
        assertEquals(List.of("armor", "items"), liveConfiguration.kitSectionKeys());
    }

    @Test
    void aFileWithoutAKitSectionReloadsTheKitAsAnEmptyOne() {
        reloadServiceFor(new MemoryConfiguration()).reloadFromFile();

        assertEquals("no kit section", liveConfiguration.valueOf(RecordingLiveConfiguration.KIT));
    }

    @Test
    void everyKnownSettingIsAppliedByASingleReload() {
        reloadServiceFor(new MemoryConfiguration()).reloadFromFile();

        assertEquals(RecordingLiveConfiguration.EVERY_APPLIED_VALUE, liveConfiguration.appliedValueNames());
    }

    @Test
    void readsTheFileFromDiskAgainBeforeApplyingIt() {
        MemoryConfiguration fileOnDisk = new MemoryConfiguration();
        fileOnDisk.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, 8);
        AtomicReference<ConfigurationSection> loadedConfiguration = new AtomicReference<>(new MemoryConfiguration());
        AtomicInteger fileReadCount = new AtomicInteger();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(loadedConfiguration::get,
                () -> {
                    fileReadCount.incrementAndGet();
                    loadedConfiguration.set(fileOnDisk);
                },
                liveConfiguration, loggedProblems::add);

        reloadService.reloadFromFile();

        assertEquals(1, fileReadCount.get());
        assertEquals(8, liveConfiguration.intValueOf(RecordingLiveConfiguration.SAFETY_ZONE_RADIUS),
                "the value of the file that was read again has to be the applied one");
    }

    @Test
    void theStartAppliesTheLoadedConfigurationWithoutReadingTheFileAgain() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, 3);
        AtomicInteger fileReadCount = new AtomicInteger();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(() -> configuration,
                fileReadCount::incrementAndGet, liveConfiguration, loggedProblems::add);

        reloadService.applyStoredConfiguration();

        assertEquals(0, fileReadCount.get());
        assertEquals(3, liveConfiguration.intValueOf(RecordingLiveConfiguration.SAFETY_ZONE_RADIUS));
    }

    @Test
    void aBrokenEntryFallsBackAndIsReportedToTheAdminAndToTheLog() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -20);
        configuration.set(PluginSettings.BORDER_PARTICLE_VIEW_RADIUS_PATH, 30);

        ConfigurationReloadReport report = reloadServiceFor(configuration).reloadFromFile();

        assertFalse(report.isClean());
        assertEquals(1, report.problems().size());
        assertEquals(report.problems(), loggedProblems, "a broken entry belongs into the log as well");
        assertEquals(PluginSettings.defaults().safetyZoneRadius(),
                liveConfiguration.intValueOf(RecordingLiveConfiguration.SAFETY_ZONE_RADIUS));
        assertEquals(30, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_VIEW_RADIUS));
        assertTrue(report.describe().stream().anyMatch(line -> line.contains(
                PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH)), "the admin reads which entry was refused");
    }

    @Test
    void theAnswerNamesEveryReloadedCategoryAndWhatStaysUntouched() {
        List<String> lines = reloadServiceFor(new MemoryConfiguration()).reloadFromFile().describe();

        assertEquals("config.yml was reloaded and applied", lines.get(0));
        for (String category : ConfigurationReloadReport.RELOADED_CATEGORIES) {
            assertTrue(lines.stream().anyMatch(line -> line.contains(category)),
                    "the answer has to name the category " + category);
        }
        assertTrue(lines.stream().anyMatch(line -> line.contains("stay untouched")),
                "the answer has to say what a reload does not touch: " + lines);
    }

    @Test
    void asecondReloadAppliesTheSameValuesAgain() {
        ConfigurationReloadService reloadService = reloadServiceFor(
                PluginSettingsTest.configurationWithEveryValueChanged());

        reloadService.reloadFromFile();
        RecordingLiveConfiguration secondRun = new RecordingLiveConfiguration();
        new ConfigurationReloadService(PluginSettingsTest::configurationWithEveryValueChanged, () -> { },
                secondRun, loggedProblems::add).reloadFromFile();

        assertEquals(liveConfiguration.appliedValueNames(), secondRun.appliedValueNames());
        assertEquals(liveConfiguration.confirmedBorderParticleSettings(),
                secondRun.confirmedBorderParticleSettings());
    }

    private ConfigurationReloadService reloadServiceFor(ConfigurationSection configuration) {
        return new ConfigurationReloadService(() -> configuration, () -> { }, liveConfiguration,
                loggedProblems::add);
    }
}
