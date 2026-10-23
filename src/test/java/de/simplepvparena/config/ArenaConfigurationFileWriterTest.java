package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaSpawnType;
import de.simplepvparena.arena.PositionSnapshot;
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
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArenaConfigurationFileWriterTest {

    private static final PositionSnapshot TRIBUNE = new PositionSnapshot("event", 1.5D, 64.0D, 2.5D, 0.0F, 0.0F);

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void writesTheArenaIntoConfigYml(@TempDir Path dataFolder) throws IOException {
        File configurationFile = dataFolder.resolve("config.yml").toFile();
        ArenaRegistry arenaRegistry = new ArenaRegistry();
        assertEquals(Optional.empty(), arenaRegistry.getArena().setSpawn(ArenaSpawnType.TRIBUNE, TRIBUNE));

        new ArenaConfigurationFileWriter(arenaRegistry, YamlConfiguration::new, configurationFile, logger).write();

        ArenaRegistry readAgain = new ArenaRegistry();
        readAgain.loadFromConfiguration(YamlConfiguration.loadConfiguration(configurationFile));
        assertEquals(Optional.of(TRIBUNE), readAgain.getArena().getTribuneSpawn());
        assertEquals(List.of(), logRecords);
    }

    @Test
    void logsAndHandsOnAFailureToWrite(@TempDir Path dataFolder) throws IOException {
        Path blockingFile = dataFolder.resolve("plugins");
        Files.writeString(blockingFile, "not a directory", StandardCharsets.UTF_8);
        ArenaConfigurationFileWriter writer = new ArenaConfigurationFileWriter(new ArenaRegistry(),
                YamlConfiguration::new, blockingFile.resolve("config.yml").toFile(), logger);

        IOException failure = assertThrows(IOException.class, writer::write);

        assertEquals(1, logRecords.size());
        assertEquals(Level.SEVERE, logRecords.get(0).getLevel());
        assertEquals("Could not write config.yml, the arena configuration holds only until the next restart or "
                + "reload of the configuration", logRecords.get(0).getMessage());
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
