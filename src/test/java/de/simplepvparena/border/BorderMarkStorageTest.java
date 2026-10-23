package de.simplepvparena.border;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BorderMarkStorageTest {

    private static final String FILE_NAME = "borders.yml";
    private static final String WORLD = "event";
    private static final String OTHER_WORLD = "lobby";

    private final List<String> loggedMessages = new ArrayList<>();
    private final Logger recordingLogger = recordingLogger(loggedMessages);

    @Test
    void writesTheMarksAndReadsThemBack(@TempDir Path pluginFolder) {
        BorderMarkIndex borderMarks = markedIndex();
        BorderMarkStorage storage = storageIn(pluginFolder);

        storage.save(borderMarks);
        BorderMarkIndex reloadedMarks = new BorderMarkIndex();
        storage.load(reloadedMarks);

        assertEquals(3, reloadedMarks.getMarkedBlockCount());
        assertArrayEquals(borderMarks.getMarkedPositions(WORLD), reloadedMarks.getMarkedPositions(WORLD));
        assertArrayEquals(borderMarks.getMarkedPositions(OTHER_WORLD), reloadedMarks.getMarkedPositions(OTHER_WORLD));
    }

    @Test
    void forgetsEveryMarkInMemoryAndInTheFile(@TempDir Path pluginFolder) throws IOException {
        BorderMarkIndex borderMarks = markedIndex();
        BorderMarkStorage storage = storageIn(pluginFolder);
        storage.save(borderMarks);

        storage.clearEveryMark(borderMarks);

        assertEquals(0, borderMarks.getMarkedBlockCount());
        assertTrue(borderMarks.getWorldNames().isEmpty());
        BorderMarkIndex reloadedMarks = markedIndex();
        storage.load(reloadedMarks);
        assertEquals(0, reloadedMarks.getMarkedBlockCount());
        assertEquals("worlds: []\n", readFile(pluginFolder));
    }

    @Test
    void writesTheClearedMarksTheWayAnEmptyIndexIsSaved(@TempDir Path pluginFolder) throws IOException {
        Path clearedFolder = Files.createDirectory(pluginFolder.resolve("cleared"));
        Path emptyFolder = Files.createDirectory(pluginFolder.resolve("empty"));
        BorderMarkStorage clearedStorage = storageIn(clearedFolder);
        BorderMarkIndex borderMarks = markedIndex();
        clearedStorage.save(borderMarks);

        clearedStorage.clearEveryMark(borderMarks);
        storageIn(emptyFolder).save(new BorderMarkIndex());

        assertEquals(readFile(emptyFolder), readFile(clearedFolder));
    }

    @Test
    void keepsTheMarksForgottenWhenTheFileCannotBeWritten(@TempDir Path pluginFolder) throws IOException {
        Files.createDirectory(pluginFolder.resolve(FILE_NAME));
        BorderMarkIndex borderMarks = markedIndex();
        BorderMarkStorage storage = storageIn(pluginFolder);

        assertThrows(IOException.class, () -> storage.clearEveryMark(borderMarks));

        assertEquals(0, borderMarks.getMarkedBlockCount());
        assertTrue(Files.isDirectory(pluginFolder.resolve(FILE_NAME)));
        assertEquals(List.of(), loggedMessages, "the caller reports the failure");
    }

    @Test
    void throwsWhenTheFolderOfTheFileCannotBeCreated(@TempDir Path pluginFolder) throws IOException {
        Path blockingFile = Files.writeString(pluginFolder.resolve("plugin"), "not a folder");
        BorderMarkIndex borderMarks = markedIndex();
        BorderMarkStorage storage = storageIn(blockingFile);

        IOException failure = assertThrows(IOException.class, () -> storage.clearEveryMark(borderMarks));

        assertEquals("Could not create the directory of borders.yml", failure.getMessage());
        assertEquals(0, borderMarks.getMarkedBlockCount());
        assertEquals("not a folder", Files.readString(blockingFile, StandardCharsets.UTF_8));
    }

    @Test
    void savesWithoutThrowingAndLogsAFailedWrite(@TempDir Path pluginFolder) throws IOException {
        Files.createDirectory(pluginFolder.resolve(FILE_NAME));
        BorderMarkIndex borderMarks = markedIndex();

        storageIn(pluginFolder).save(borderMarks);

        assertEquals(3, borderMarks.getMarkedBlockCount());
        assertEquals(List.of("SEVERE Could not write borders.yml, border marks are not persisted"), loggedMessages);
    }

    private static BorderMarkIndex markedIndex() {
        BorderMarkIndex borderMarks = new BorderMarkIndex();
        borderMarks.mark(WORLD, 1, 64, 1);
        borderMarks.mark(WORLD, 2, 64, 1);
        borderMarks.mark(OTHER_WORLD, -5, 70, 12);
        return borderMarks;
    }

    private BorderMarkStorage storageIn(Path pluginFolder) {
        return new BorderMarkStorage(pluginFolder.resolve(FILE_NAME).toFile(), recordingLogger);
    }

    private static String readFile(Path pluginFolder) throws IOException {
        return Files.readString(pluginFolder.resolve(FILE_NAME), StandardCharsets.UTF_8);
    }

    private static Logger recordingLogger(List<String> loggedMessages) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                loggedMessages.add(logRecord.getLevel() + " " + logRecord.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        logger.setLevel(Level.ALL);
        return logger;
    }
}
