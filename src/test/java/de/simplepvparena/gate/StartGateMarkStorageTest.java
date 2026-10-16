package de.simplepvparena.gate;

import de.simplepvparena.arena.BlockPositionCodec;
import org.bukkit.Material;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateMarkStorageTest {

    private static final String WORLD = "event";
    private static final String OTHER_WORLD = "event.copy";

    private final List<String> loggedMessages = new ArrayList<>();
    private final Logger recordingLogger = recordingLogger(loggedMessages);

    @Test
    void writesTheGateAndReadsItBack(@TempDir Path pluginFolder) {
        StartGateMarks gateMarks = new StartGateMarks();
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);
        gateMarks.mark(WORLD, 10, 66, 20, Material.GLASS);
        gateMarks.mark(WORLD, 40, 65, 20, Material.IRON_BARS);
        StartGateMarkStorage storage = storageIn(pluginFolder);

        storage.save(gateMarks);
        StartGateMarks reloadedMarks = new StartGateMarks();
        storage.load(reloadedMarks);

        assertEquals(3, reloadedMarks.getMarkedBlockCount());
        assertEquals(Optional.of(Material.BARRIER), reloadedMarks.findMaterial(WORLD, 10, 65, 20));
        assertEquals(Optional.of(Material.GLASS), reloadedMarks.findMaterial(WORLD, 10, 66, 20));
        assertEquals(Optional.of(Material.IRON_BARS), reloadedMarks.findMaterial(WORLD, 40, 65, 20));
    }

    @Test
    void keepsAWorldNameThatContainsADot(@TempDir Path pluginFolder) {
        StartGateMarks gateMarks = new StartGateMarks();
        gateMarks.mark(OTHER_WORLD, 1, 2, 3, Material.BARRIER);
        StartGateMarkStorage storage = storageIn(pluginFolder);

        storage.save(gateMarks);
        StartGateMarks reloadedMarks = new StartGateMarks();
        storage.load(reloadedMarks);

        assertEquals(List.of(OTHER_WORLD), List.copyOf(reloadedMarks.getWorldNames()));
        assertEquals(Optional.of(Material.BARRIER), reloadedMarks.findMaterial(OTHER_WORLD, 1, 2, 3));
    }

    @Test
    void startsWithoutAGateWhenNoFileIsThere(@TempDir Path pluginFolder) {
        StartGateMarks gateMarks = new StartGateMarks();
        gateMarks.mark(WORLD, 10, 65, 20, Material.BARRIER);

        storageIn(pluginFolder).load(gateMarks);

        assertEquals(0, gateMarks.getMarkedBlockCount(), "the load replaces whatever was in memory");
    }

    @Test
    void skipsTheLinesItCannotReadAndKeepsTheRest(@TempDir Path pluginFolder) throws IOException {
        Files.writeString(pluginFolder.resolve(StartGateMarkStorage.FILE_NAME), """
                gate:
                  - world: event
                    marks:
                      - '%s BARRIER'
                      - 'not a position BARRIER'
                      - '%s NO_SUCH_MATERIAL'
                      - '%s'
                """.formatted(BlockPositionCodec.encode(10, 65, 20), BlockPositionCodec.encode(11, 65, 20),
                BlockPositionCodec.encode(12, 65, 20)), StandardCharsets.UTF_8);
        StartGateMarks gateMarks = new StartGateMarks();

        storageIn(pluginFolder).load(gateMarks);

        assertEquals(1, gateMarks.getMarkedBlockCount());
        assertTrue(gateMarks.isMarked(WORLD, 10, 65, 20));
        assertFalse(gateMarks.isMarked(WORLD, 11, 65, 20));
    }

    @Test
    void readsAndWritesTheLineOfASingleMark() {
        long encodedPosition = BlockPositionCodec.encode(-8, 65, 320);

        String line = StartGateMarkLine.format(encodedPosition, Material.BARRIER);
        StartGateMarkLine parsedMark = StartGateMarkLine.parse(line).orElseThrow();

        assertEquals(encodedPosition + " BARRIER", line);
        assertEquals(Material.BARRIER, parsedMark.material());
        assertEquals(-8, parsedMark.blockX());
        assertEquals(65, parsedMark.blockY());
        assertEquals(320, parsedMark.blockZ());
        assertEquals(line, parsedMark.format());
    }

    @Test
    void readsALineThatWasEditedByHand() {
        long encodedPosition = BlockPositionCodec.encode(1, 2, 3);

        assertEquals(Material.BARRIER,
                StartGateMarkLine.parse("  " + encodedPosition + "   barrier ").orElseThrow().material());
        assertTrue(StartGateMarkLine.parse(encodedPosition + " minecraft:barrier").isEmpty(),
                "a namespaced name is not a material name");
        assertTrue(StartGateMarkLine.parse("").isEmpty());
        assertTrue(StartGateMarkLine.parse(null).isEmpty());
        assertTrue(StartGateMarkLine.parse(encodedPosition + " BARRIER 3").isEmpty());
    }

    private StartGateMarkStorage storageIn(Path pluginFolder) {
        return new StartGateMarkStorage(
                new File(pluginFolder.toFile(), StartGateMarkStorage.FILE_NAME),
                recordingLogger);
    }

    private static Logger recordingLogger(List<String> loggedMessages) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                loggedMessages.add(logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
