package de.simplepvparena.border;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BorderMarkStorage {

    private static final String WORLDS_KEY = "worlds";
    private static final String WORLD_NAME_KEY = "world";
    private static final String MARKS_KEY = "marks";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

    private final File bordersFile;
    private final Logger logger;

    public BorderMarkStorage(File bordersFile, Logger logger) {
        this.bordersFile = Objects.requireNonNull(bordersFile, "bordersFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void load(BorderMarkIndex borderMarks) {
        borderMarks.clear();
        if (!bordersFile.isFile()) {
            logger.info("No " + bordersFile.getName() + " found, starting without border marks");
            return;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(bordersFile);
        for (Map<?, ?> worldEntry : configuration.getMapList(WORLDS_KEY)) {
            readWorldEntry(borderMarks, worldEntry);
        }
        logger.info("Loaded " + borderMarks.getMarkedBlockCount() + " border marks from " + bordersFile.getName());
    }

    public void save(BorderMarkIndex borderMarks) {
        try {
            write(borderMarks);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + bordersFile.getName() + ", border marks are not persisted",
                    failure);
        }
    }

    public void clearEveryMark(BorderMarkIndex borderMarks) throws IOException {
        borderMarks.clear();
        write(borderMarks);
    }

    public void write(BorderMarkIndex borderMarks) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        List<Map<String, Object>> worldEntries = new ArrayList<>();
        for (String worldName : borderMarks.getWorldNames()) {
            long[] markedPositions = borderMarks.getMarkedPositions(worldName);
            if (markedPositions.length == 0) {
                continue;
            }
            Map<String, Object> worldEntry = new LinkedHashMap<>();
            worldEntry.put(WORLD_NAME_KEY, worldName);
            worldEntry.put(MARKS_KEY, toList(markedPositions));
            worldEntries.add(worldEntry);
        }
        configuration.set(WORLDS_KEY, worldEntries);
        File parentDirectory = bordersFile.getParentFile();
        if (parentDirectory != null && !parentDirectory.isDirectory() && !parentDirectory.mkdirs()) {
            throw new IOException("Could not create the directory of " + bordersFile.getName());
        }
        Path targetFile = bordersFile.toPath().toAbsolutePath();
        Path temporaryFile = Files.createTempFile(targetFile.getParent(), bordersFile.getName(),
                TEMPORARY_FILE_EXTENSION);
        try {
            Files.writeString(temporaryFile, configuration.saveToString(), StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, targetFile);
        } catch (IOException | RuntimeException writeFailure) {
            removeTemporaryFile(temporaryFile, writeFailure);
            throw writeFailure;
        }
    }

    private static void moveIntoPlace(Path temporaryFile, Path targetFile) throws IOException {
        try {
            Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeTemporaryFile(Path temporaryFile, Exception writeFailure) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private void readWorldEntry(BorderMarkIndex borderMarks, Map<?, ?> worldEntry) {
        Object worldName = worldEntry.get(WORLD_NAME_KEY);
        Object marks = worldEntry.get(MARKS_KEY);
        if (!(worldName instanceof String readableWorldName) || !(marks instanceof List<?> readableMarks)) {
            logger.warning("Skipped an unreadable world entry in " + bordersFile.getName());
            return;
        }
        long[] markedPositions = new long[readableMarks.size()];
        int markCount = 0;
        for (Object mark : readableMarks) {
            if (mark instanceof Number encodedPosition) {
                markedPositions[markCount++] = encodedPosition.longValue();
            }
        }
        if (markCount < readableMarks.size()) {
            logger.warning("Skipped " + (readableMarks.size() - markCount) + " unreadable border marks of world "
                    + readableWorldName + " in " + bordersFile.getName());
            markedPositions = Arrays.copyOf(markedPositions, markCount);
        }
        borderMarks.replaceWorld(readableWorldName, markedPositions);
    }

    private static List<Long> toList(long[] markedPositions) {
        List<Long> marks = new ArrayList<>(markedPositions.length);
        for (long markedPosition : markedPositions) {
            marks.add(markedPosition);
        }
        return marks;
    }
}
