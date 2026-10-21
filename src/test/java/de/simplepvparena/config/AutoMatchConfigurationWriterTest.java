package de.simplepvparena.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoMatchConfigurationWriterTest {

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void writesTheModeIntoConfigYmlAndKeepsEveryCommentOfTheFile(@TempDir Path dataFolder) throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        String shippedText = shippedConfigurationText();
        Files.writeString(configurationFile, shippedText, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new AutoMatchConfigurationWriter(() -> configuration, configurationFile.toFile(), logger).write(true);

        List<String> writtenLines = Files.readAllLines(configurationFile, StandardCharsets.UTF_8);
        assertTrue(writtenLines.contains("  enabled: true"), writtenLines.toString());
        assertFalse(writtenLines.contains("  enabled: false"), writtenLines.toString());
        for (String shippedLine : shippedText.lines().toList()) {
            if (shippedLine.trim().startsWith("#")) {
                assertTrue(writtenLines.contains(shippedLine), "the comment is kept: " + shippedLine);
            }
        }
        int entryIndex = writtenLines.indexOf("  enabled: true");
        assertEquals("auto-match:", writtenLines.get(entryIndex - 2));
        assertTrue(writtenLines.get(entryIndex - 1).startsWith("  # When true"),
                "the comment stays above the entry: " + writtenLines.get(entryIndex - 1));
        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile.toFile());
        assertTrue(PluginSettings.read(readAgain, problem -> { }).autoMatchSettings().enabled());
        assertEquals(List.of(), logRecords);
    }

    @Test
    void addsTheModeToAConfigYmlOfAnOlderVersionThatDoesNotHoldItYet(@TempDir Path dataFolder)
            throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        Files.writeString(configurationFile, """
                # Rounds of the match.
                match:
                  # Ticks between two rounds.
                  round-reset-delay-ticks: 60
                """, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new AutoMatchConfigurationWriter(() -> configuration, configurationFile.toFile(), logger).write(false);

        List<String> writtenLines = Files.readAllLines(configurationFile, StandardCharsets.UTF_8);
        assertEquals(List.of("# Rounds of the match.", "match:", "  # Ticks between two rounds.",
                "  round-reset-delay-ticks: 60", "auto-match:", "  enabled: false"), writtenLines);
    }

    @Test
    void writesIntoTheConfigurationThatIsCurrentAtTheMomentOfTheClick(@TempDir Path dataFolder) throws IOException {
        File configurationFile = dataFolder.resolve("config.yml").toFile();
        AtomicReference<YamlConfiguration> currentConfiguration = new AtomicReference<>(new YamlConfiguration());
        AutoMatchConfigurationWriter writer = new AutoMatchConfigurationWriter(currentConfiguration::get,
                configurationFile, logger);
        YamlConfiguration reloadedConfiguration = new YamlConfiguration();
        reloadedConfiguration.set(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH, 45);
        currentConfiguration.set(reloadedConfiguration);

        writer.write(true);

        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile);
        assertTrue(readAgain.getBoolean(PluginSettings.AUTO_MATCH_ENABLED_PATH));
        assertEquals(45, readAgain.getInt(PluginSettings.AUTO_MATCH_PAUSE_SECONDS_PATH));
    }

    @Test
    void logsAndHandsOnAFailureToWriteAndKeepsTheValueInMemory(@TempDir Path dataFolder) throws IOException {
        Path blockingFile = dataFolder.resolve("plugins");
        Files.writeString(blockingFile, "not a directory", StandardCharsets.UTF_8);
        File configurationFile = blockingFile.resolve("config.yml").toFile();
        YamlConfiguration configuration = new YamlConfiguration();
        AutoMatchConfigurationWriter writer = new AutoMatchConfigurationWriter(() -> configuration,
                configurationFile, logger);

        IOException failure = assertThrows(IOException.class, () -> writer.write(true));

        assertTrue(configuration.getBoolean(PluginSettings.AUTO_MATCH_ENABLED_PATH));
        assertEquals(1, logRecords.size());
        assertEquals(Level.SEVERE, logRecords.get(0).getLevel());
        assertEquals("Could not write config.yml, the value true of auto-match.enabled holds only until the next "
                + "restart or reload of the configuration", logRecords.get(0).getMessage());
        assertSame(failure, logRecords.get(0).getThrown());
    }

    private Logger createRecordingLogger() {
        Logger recordingLogger = Logger.getAnonymousLogger();
        recordingLogger.setUseParentHandlers(false);
        recordingLogger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return recordingLogger;
    }

    private static String shippedConfigurationText() throws IOException {
        try (InputStream shippedFile = AutoMatchConfigurationWriterTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
