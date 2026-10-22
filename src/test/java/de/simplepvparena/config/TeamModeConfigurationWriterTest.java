package de.simplepvparena.config;

import de.simplepvparena.registration.TeamMode;
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

class TeamModeConfigurationWriterTest {

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void writesTheTeamModeIntoConfigYmlAndKeepsEveryCommentOfTheFile(@TempDir Path dataFolder) throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        String shippedText = shippedConfigurationText();
        Files.writeString(configurationFile, shippedText, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new TeamModeConfigurationWriter(() -> configuration, configurationFile.toFile(), logger)
                .write(TeamMode.RANDOM);

        List<String> writtenLines = Files.readAllLines(configurationFile, StandardCharsets.UTF_8);
        assertTrue(writtenLines.contains("  team-mode: random"), writtenLines.toString());
        assertFalse(writtenLines.contains("  team-mode: free-choice"), writtenLines.toString());
        for (String shippedLine : shippedText.lines().toList()) {
            if (shippedLine.trim().startsWith("#")) {
                assertTrue(writtenLines.contains(shippedLine), "the comment is kept: " + shippedLine);
            }
        }
        int entryIndex = writtenLines.indexOf("  team-mode: random");
        assertEquals("registration:", writtenLines.get(entryIndex - 2));
        assertTrue(writtenLines.get(entryIndex - 1).startsWith("  # free-choice"),
                "the comment stays above the entry: " + writtenLines.get(entryIndex - 1));
        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile.toFile());
        assertEquals(TeamMode.RANDOM, PluginSettings.read(readAgain, problem -> { }).teamMode());
        assertEquals(List.of(), logRecords);
    }

    @Test
    void writesTheFreeChoiceBackUnderItsConfigurationName(@TempDir Path dataFolder) throws IOException {
        File configurationFile = dataFolder.resolve("config.yml").toFile();
        YamlConfiguration configuration = new YamlConfiguration();
        TeamModeConfigurationWriter writer = new TeamModeConfigurationWriter(() -> configuration, configurationFile,
                logger);

        writer.write(TeamMode.RANDOM);
        writer.write(TeamMode.FREE_CHOICE);

        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile);
        assertEquals("free-choice", readAgain.getString(PluginSettings.REGISTRATION_TEAM_MODE_PATH));
    }

    @Test
    void addsTheTeamModeToAConfigYmlOfAnOlderVersionThatDoesNotHoldItYet(@TempDir Path dataFolder)
            throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        Files.writeString(configurationFile, """
                # Rounds of the match.
                match:
                  # Ticks between two rounds.
                  round-reset-delay-ticks: 60
                """, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new TeamModeConfigurationWriter(() -> configuration, configurationFile.toFile(), logger)
                .write(TeamMode.RANDOM);

        List<String> writtenLines = Files.readAllLines(configurationFile, StandardCharsets.UTF_8);
        assertEquals(List.of("# Rounds of the match.", "match:", "  # Ticks between two rounds.",
                "  round-reset-delay-ticks: 60", "registration:", "  team-mode: random"), writtenLines);
    }

    @Test
    void logsAndHandsOnAFailureToWriteAndKeepsTheValueInMemory(@TempDir Path dataFolder) throws IOException {
        Path blockingFile = dataFolder.resolve("plugins");
        Files.writeString(blockingFile, "not a directory", StandardCharsets.UTF_8);
        File configurationFile = blockingFile.resolve("config.yml").toFile();
        YamlConfiguration configuration = new YamlConfiguration();
        TeamModeConfigurationWriter writer = new TeamModeConfigurationWriter(() -> configuration,
                configurationFile, logger);

        IOException failure = assertThrows(IOException.class, () -> writer.write(TeamMode.RANDOM));

        assertEquals("random", configuration.getString(PluginSettings.REGISTRATION_TEAM_MODE_PATH));
        assertEquals(1, logRecords.size());
        assertEquals(Level.SEVERE, logRecords.get(0).getLevel());
        assertEquals("Could not write config.yml, the value random of registration.team-mode holds only until the "
                + "next restart or reload of the configuration", logRecords.get(0).getMessage());
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
        try (InputStream shippedFile = TeamModeConfigurationWriterTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
