package de.simplepvparena.config;

import de.simplepvparena.border.ConfirmedBorderParticleSettings;
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

class ConfirmedBorderParticleConfigurationWriterTest {

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void writesTheSwitchIntoConfigYmlAndKeepsEveryCommentOfTheFile(@TempDir Path dataFolder) throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        String shippedText = shippedConfigurationText();
        Files.writeString(configurationFile, shippedText, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new ConfirmedBorderParticleConfigurationWriter(() -> configuration, configurationFile.toFile(), logger)
                .write(false);

        List<String> writtenLines = Files.readAllLines(configurationFile, StandardCharsets.UTF_8);
        for (String shippedLine : shippedText.lines().toList()) {
            if (shippedLine.trim().startsWith("#")) {
                assertTrue(writtenLines.contains(shippedLine), "the comment is kept: " + shippedLine);
            }
        }
        int sectionIndex = writtenLines.indexOf("  confirmed-particle:");
        assertTrue(writtenLines.get(sectionIndex + 1).startsWith("    # false hides the marker"),
                "the comment stays above the entry: " + writtenLines.get(sectionIndex + 1));
        assertEquals("    enabled: false", writtenLines.get(sectionIndex + 2));
        assertTrue(writtenLines.contains("    enabled: true"), "the cleaning particles stay switched on");
        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile.toFile());
        PluginSettings settings = PluginSettings.read(readAgain, problem -> { });
        assertFalse(settings.confirmedBorderParticleEnabled());
        assertTrue(settings.arenaCleaningParticleSettings().enabled());
        assertEquals(List.of(), logRecords);
    }

    @Test
    void addsTheSwitchToAConfigYmlOfAnOlderVersionThatDoesNotHoldItYet(@TempDir Path dataFolder)
            throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        Files.writeString(configurationFile, """
                border:
                  confirmed-particle:
                    type: END_ROD
                """, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new ConfirmedBorderParticleConfigurationWriter(() -> configuration, configurationFile.toFile(), logger)
                .write(true);

        List<String> writtenLines = Files.readAllLines(configurationFile, StandardCharsets.UTF_8);
        assertEquals(List.of("border:", "  confirmed-particle:", "    type: END_ROD", "    enabled: true"),
                writtenLines);
    }

    @Test
    void writesIntoTheConfigurationThatIsCurrentAtTheMomentOfTheClick(@TempDir Path dataFolder) throws IOException {
        File configurationFile = dataFolder.resolve("config.yml").toFile();
        AtomicReference<YamlConfiguration> currentConfiguration = new AtomicReference<>(new YamlConfiguration());
        ConfirmedBorderParticleConfigurationWriter writer = new ConfirmedBorderParticleConfigurationWriter(
                currentConfiguration::get, configurationFile, logger);
        YamlConfiguration reloadedConfiguration = new YamlConfiguration();
        reloadedConfiguration.set(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH, "DUST");
        currentConfiguration.set(reloadedConfiguration);

        writer.write(false);

        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile);
        assertFalse(readAgain.getBoolean(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH, true));
        assertEquals("DUST", readAgain.getString(ConfirmedBorderParticleSettings.TYPE_CONFIGURATION_PATH));
    }

    @Test
    void logsAndHandsOnAFailureToWriteAndKeepsTheValueInMemory(@TempDir Path dataFolder) throws IOException {
        Path blockingFile = dataFolder.resolve("plugins");
        Files.writeString(blockingFile, "not a directory", StandardCharsets.UTF_8);
        File configurationFile = blockingFile.resolve("config.yml").toFile();
        YamlConfiguration configuration = new YamlConfiguration();
        ConfirmedBorderParticleConfigurationWriter writer = new ConfirmedBorderParticleConfigurationWriter(
                () -> configuration, configurationFile, logger);

        IOException failure = assertThrows(IOException.class, () -> writer.write(false));

        assertFalse(configuration.getBoolean(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH, true));
        assertEquals(1, logRecords.size());
        assertEquals(Level.SEVERE, logRecords.get(0).getLevel());
        assertEquals("Could not write config.yml, the value false of border.confirmed-particle.enabled holds only "
                + "until the next restart or reload of the configuration", logRecords.get(0).getMessage());
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
        try (InputStream shippedFile = ConfirmedBorderParticleConfigurationWriterTest.class
                .getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
