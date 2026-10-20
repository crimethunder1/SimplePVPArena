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
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.SUDDEN_DEATH_IN_FINAL));
        assertEquals(4, liveConfiguration.intValueOf(RecordingLiveConfiguration.START_GATE_DURATION));
        assertEquals(new TeamColourSettings(NamedTextColor.GOLD, NamedTextColor.DARK_PURPLE),
                liveConfiguration.teamColourSettings());
        assertEquals(4, liveConfiguration.intValueOf(RecordingLiveConfiguration.TEAM_SIZE));
        assertEquals(1024, liveConfiguration.intValueOf(RecordingLiveConfiguration.CLEANING_BLOCKS_PER_TICK));
        assertEquals(2048, liveConfiguration.intValueOf(RecordingLiveConfiguration.BASELINE_BLOCKS_PER_TICK));
        assertEquals(20, liveConfiguration.intValueOf(RecordingLiveConfiguration.BASELINE_DEPTH_BELOW_FLOOR));
        assertEquals(new ArenaCleaningParticleSettings(false, "COMPOSTER", 8.0D, 3),
                liveConfiguration.arenaCleaningParticleSettings());
        assertEquals(12345, liveConfiguration.intValueOf(RecordingLiveConfiguration.MAXIMUM_CELL_COUNT));
        assertEquals(17, liveConfiguration.intValueOf(RecordingLiveConfiguration.SAFETY_ZONE_RADIUS));
        assertEquals(40, liveConfiguration.intValueOf(RecordingLiveConfiguration.EFFECT_MARGIN));
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.TELEPORT_ON_JOIN));
        assertEquals(11L, liveConfiguration.longValueOf(RecordingLiveConfiguration.PARTICLE_PERIOD));
        assertEquals(7, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_VIEW_RADIUS));
        assertEquals(9, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_MAXIMUM_BLOCKS));
        assertEquals(13, liveConfiguration.intValueOf(RecordingLiveConfiguration.PARTICLE_MAXIMUM_LINES));
        assertEquals(6L * PluginSettings.TICKS_PER_SECOND,
                liveConfiguration.longValueOf(RecordingLiveConfiguration.WALL_PREVIEW_DURATION));
        assertEquals(new ConfirmedBorderParticleSettings("DUST", 1.5D, 10, 20, 30),
                liveConfiguration.confirmedBorderParticleSettings());
        assertEquals("de", liveConfiguration.valueOf(RecordingLiveConfiguration.LANGUAGE));
    }

    @Test
    void reloadsTheStartGateDurationWhileTheServerRuns() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 10);
        ConfigurationReloadService reloadService = reloadServiceFor(configuration);

        reloadService.reloadFromFile();
        assertEquals(10, liveConfiguration.startGateDuration().getSeconds());

        configuration.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 4);
        reloadService.reloadFromFile();

        assertEquals(4, liveConfiguration.startGateDuration().getSeconds(),
                "the second reload reaches the running gate");
        assertEquals(80L, liveConfiguration.startGateDuration().getTotalTicks(),
                "and the next round start is four seconds long");
        assertEquals(List.of(), loggedProblems);
    }

    @Test
    void aStartGateDurationBelowASecondFallsBackAndIsReported() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 0);

        ConfigurationReloadReport report = reloadServiceFor(configuration).reloadFromFile();

        assertFalse(report.isClean());
        assertEquals(PluginSettings.defaults().startGateDurationSeconds(),
                liveConfiguration.startGateDuration().getSeconds());
        assertTrue(report.problems().get(0).startsWith(PluginSettings.START_GATE_DURATION_SECONDS_PATH),
                report.problems().toString());
    }

    @Test
    void reloadsWhetherTheFinalIsFoughtWithoutATimeLimit() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        ConfigurationReloadService reloadService = reloadServiceFor(configuration);

        reloadService.reloadFromFile();
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.SUDDEN_DEATH_IN_FINAL),
                "a file without the entry is the shipped switch");

        configuration.set(PluginSettings.DISABLE_SUDDEN_DEATH_IN_FINAL_PATH, false);
        reloadService.reloadFromFile();

        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.SUDDEN_DEATH_IN_FINAL),
                "the second reload reaches the running match service");
        assertEquals(List.of(), loggedProblems);
    }

    @Test
    void aKitSectionInTheFileIsNeitherReadNorReportedAsAProblem() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.KIT_SECTION_PATH + ".armor.head.material", "DIAMOND_HELMET");
        configuration.set(PluginSettings.KIT_SECTION_PATH + ".items", List.of());

        ConfigurationReloadReport report = reloadServiceFor(configuration).reloadFromFile();

        assertTrue(report.isClean(), report.problems().toString());
        assertEquals(RecordingLiveConfiguration.EVERY_APPLIED_VALUE, liveConfiguration.appliedValueNames());
        assertFalse(report.describe().stream().anyMatch(line -> line.contains("kit")), report.describe().toString());
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
        for (String category : ConfigurationReloadReport.describeReloadedCategories()) {
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
