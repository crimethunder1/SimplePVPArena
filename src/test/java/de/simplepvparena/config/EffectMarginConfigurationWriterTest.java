package de.simplepvparena.config;

import de.simplepvparena.arena.EffectArea;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EffectMarginConfigurationWriterTest {

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void writesTheMarginIntoConfigYmlAndKeepsTheOtherValues(@TempDir Path dataFolder) throws IOException {
        Path configurationFile = dataFolder.resolve("config.yml");
        Files.writeString(configurationFile, """
                arena:
                  effect-margin: 96
                  teleport-on-join: false
                """, StandardCharsets.UTF_8);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile.toFile());

        new EffectMarginConfigurationWriter(() -> configuration, configurationFile.toFile(), logger).write(104);

        assertEquals(List.of("arena:", "  effect-margin: 104", "  teleport-on-join: false"),
                Files.readAllLines(configurationFile, StandardCharsets.UTF_8));
        YamlConfiguration readAgain = YamlConfiguration.loadConfiguration(configurationFile.toFile());
        assertEquals(104, PluginSettings.read(readAgain, problem -> { }).arenaEffectMargin());
        assertEquals(List.of(), logRecords);
    }

    @Test
    void logsAndHandsOnAFailureToWriteAndKeepsTheValueInMemory(@TempDir Path dataFolder) throws IOException {
        Path blockingFile = dataFolder.resolve("plugins");
        Files.writeString(blockingFile, "not a directory", StandardCharsets.UTF_8);
        File configurationFile = blockingFile.resolve("config.yml").toFile();
        YamlConfiguration configuration = new YamlConfiguration();
        EffectMarginConfigurationWriter writer = new EffectMarginConfigurationWriter(() -> configuration,
                configurationFile, logger);

        IOException failure = assertThrows(IOException.class, () -> writer.write(0));

        assertEquals(0, configuration.getInt(EffectArea.MARGIN_CONFIGURATION_PATH, -1));
        assertEquals(1, logRecords.size());
        assertEquals(Level.SEVERE, logRecords.get(0).getLevel());
        assertEquals("Could not write config.yml, the value 0 of arena.effect-margin holds only until the next "
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
}
