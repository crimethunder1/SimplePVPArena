package de.simplepvparena.config;

import de.simplepvparena.message.ConfigMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.tournament.AutoMatchSettings;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheModeSwitchedOffWithTenSecondsBeforeTheStartAndTwentySecondsOfPause() {
        AutoMatchSettings defaults = PluginSettings.defaults().autoMatchSettings();

        assertEquals(new AutoMatchSettings(false, 10, 20), defaults);
        assertEquals(AutoMatchSettings.defaults(), defaults);
        assertEquals(PluginSettings.defaults(), PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheThreeEntriesWithTheirDefaultsAndACommentInAnOwnSection() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("auto-match:");
        int enabledIndex = lines.indexOf("  enabled: false");
        int startDelayIndex = lines.indexOf("  start-delay-seconds: 10");
        int pauseIndex = lines.indexOf("  pause-seconds: 20");

        assertTrue(sectionIndex > lines.indexOf("match:"), "config.yml has to hold the section auto-match: " + lines);
        assertTrue(lines.get(sectionIndex - 1).startsWith("# "), "a comment explains the section auto-match");
        assertTrue(sectionIndex < enabledIndex && enabledIndex < startDelayIndex && startDelayIndex < pauseIndex,
                "the three entries follow the section in this order: " + lines);
        assertTrue(pauseIndex < lines.indexOf("arena:"), "the section ends before the arena section");
        for (int entryIndex : new int[] {enabledIndex, startDelayIndex, pauseIndex}) {
            assertTrue(lines.get(entryIndex - 1).startsWith("  # "), "a comment explains " + lines.get(entryIndex));
        }
    }

    @Test
    void readsTheModeAndBothDurationsWithinTheirLimitsWithoutAProblem() {
        for (int[] values : new int[][] {{3, 0}, {10, 20}, {120, 300}}) {
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, true);
            configuration.set(PluginSettings.AUTO_MATCH_START_DELAY_SECONDS_PATH, values[0]);
            configuration.set(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH, values[1]);

            assertEquals(new AutoMatchSettings(true, values[0], values[1]),
                    PluginSettings.read(configuration, problems::add).autoMatchSettings());
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToTheSwitchedOffModeForAnEntryThatIsNoTruthValueAndReportsIt() {
        for (Object enabled : new Object[] {"maybe", 1, 2.5D}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, enabled);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(false, settings.autoMatchSettings().enabled(), "a value of " + enabled);
            assertEquals(List.of("auto-match.enabled must be true or false, falling back to false"), problems,
                    "a value of " + enabled + " is reported");
        }
    }

    @Test
    void fallsBackForAStartDelayOutsideOfThreeToOneHundredTwentyOrNoWholeNumberAndReportsIt() {
        for (Object startDelaySeconds : new Object[] {2, 121, -1, "soon", 12.5D, true}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.AUTO_MATCH_START_DELAY_SECONDS_PATH, startDelaySeconds);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(10, settings.autoMatchSettings().startDelaySeconds(), "a value of " + startDelaySeconds);
            assertEquals(List.of("auto-match.start-delay-seconds must be a whole number between 3 and 120 seconds, "
                    + "falling back to 10 seconds"), problems, "a value of " + startDelaySeconds + " is reported");
        }
    }

    @Test
    void fallsBackForAPauseOutsideOfZeroToThreeHundredOrNoWholeNumberAndReportsIt() {
        for (Object pauseSeconds : new Object[] {-1, 301, "short", 0.5D, false}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH, pauseSeconds);

            PluginSettings settings = PluginSettings.read(configuration, problems::add);

            assertEquals(20, settings.autoMatchSettings().pauseSeconds(), "a value of " + pauseSeconds);
            assertEquals(List.of("auto-match.pause-seconds must be a whole number between 0 and 300 seconds, "
                    + "falling back to 20 seconds"), problems, "a value of " + pauseSeconds + " is reported");
        }
    }

    @Test
    void writesAFallbackOfOneSecondInTheSingular() {
        assertEquals("auto-match.start-delay-seconds must be a whole number between 3 and 120 seconds, falling back "
                + "to 1 second", Messages.plural(ConfigMessage.AUTO_MATCH_START_DELAY_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.AUTO_MATCH_START_DELAY_SECONDS_PATH),
                Placeholder.of("minimum", 3), Placeholder.of("maximum", 120)));
        assertEquals("auto-match.pause-seconds must be a whole number between 0 and 300 seconds, falling back to "
                + "1 second", Messages.plural(ConfigMessage.AUTO_MATCH_PAUSE_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH),
                Placeholder.of("minimum", 0), Placeholder.of("maximum", 300)));
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntriesInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, "vielleicht");
        configuration.set(PluginSettings.AUTO_MATCH_START_DELAY_SECONDS_PATH, 1);
        configuration.set(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH, 999);

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("auto-match.enabled muss true oder false sein, stattdessen gilt false",
                "auto-match.start-delay-seconds muss eine ganze Zahl zwischen 3 und 120 Sekunden sein, stattdessen "
                        + "gelten 10 Sekunden",
                "auto-match.pause-seconds muss eine ganze Zahl zwischen 0 und 300 Sekunden sein, stattdessen gelten "
                        + "20 Sekunden"), problems);
        assertEquals("auto-match.pause-seconds muss eine ganze Zahl zwischen 0 und 300 Sekunden sein, stattdessen "
                + "gilt 1 Sekunde", Messages.plural(ConfigMessage.AUTO_MATCH_PAUSE_OUT_OF_RANGE, 1,
                Placeholder.of("path", PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH), Placeholder.of("minimum", 0),
                Placeholder.of("maximum", 300)));
    }

    @Test
    void theSettingsKeepBothDurationsWithinTheirLimits() {
        assertEquals(new AutoMatchSettings(true, 3, 0), new AutoMatchSettings(true, -5, -5));
        assertEquals(new AutoMatchSettings(false, 120, 300), new AutoMatchSettings(false, 9999, 9999));
        assertEquals(new AutoMatchSettings(true, 45, 7), new AutoMatchSettings(false, 45, 7).withEnabled(true));
    }

    @Test
    void theStartAndEveryReloadHandTheSettingsToTheRunningPlugin() {
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWith(true, 30, 5));
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                liveConfiguration, problems::add);

        assertTrue(reloadService.applyStoredConfiguration().isClean());

        assertEquals(new AutoMatchSettings(true, 30, 5), liveConfiguration.autoMatchSettings());

        fileOnDisk.set(configurationWith(false, 60, 120));
        assertTrue(reloadService.reloadFromFile().isClean());

        assertEquals(new AutoMatchSettings(false, 60, 120), liveConfiguration.autoMatchSettings());

        fileOnDisk.set(configurationWith(true, 500, -2));
        reloadService.reloadFromFile();

        assertEquals(new AutoMatchSettings(true, 10, 20), liveConfiguration.autoMatchSettings(),
                "broken durations fall back to their defaults");
        assertEquals(2, problems.size(), problems.toString());
    }

    @Test
    void theReloadAnswerNamesTheNewValuesRightAfterTheTeamSize() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        int autoMatchIndex = lines.indexOf("  whether the auto match mode is on, the seconds before an auto match "
                + "starts and the pause between two auto matches");
        assertTrue(autoMatchIndex > 0, lines.toString());
        assertTrue(lines.get(autoMatchIndex - 1).startsWith("  the team size"), lines.toString());
    }

    @Test
    @UseLanguage("de")
    void theReloadAnswerNamesTheNewValuesInGerman() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  ob der Auto-Match-Modus an ist, die Sekunden bis zum Beginn eines Auto-Matches "
                + "und die Pause zwischen zwei Auto-Matches"), lines.toString());
    }

    private static MemoryConfiguration configurationWith(boolean enabled, Object startDelaySeconds,
                                                         Object pauseSeconds) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, enabled);
        configuration.set(PluginSettings.AUTO_MATCH_START_DELAY_SECONDS_PATH, startDelaySeconds);
        configuration.set(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH, pauseSeconds);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = AutoMatchSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
