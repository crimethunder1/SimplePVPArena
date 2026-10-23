package de.simplepvparena.gate;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class StartGateMarkStorage {

    public static final String FILE_NAME = "startgates.yml";

    private static final String GATE_KEY = "gate";
    private static final String WORLD_NAME_KEY = "world";
    private static final String MARKS_KEY = "marks";

    private final File gatesFile;
    private final Logger logger;

    public StartGateMarkStorage(File gatesFile, Logger logger) {
        this.gatesFile = Objects.requireNonNull(gatesFile, "gatesFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void load(StartGateMarks gateMarks) {
        gateMarks.clear();
        if (!gatesFile.isFile()) {
            logger.info("No " + gatesFile.getName() + " found, starting without a start gate");
            return;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(gatesFile);
        Map<String, Map<Long, Material>> readMarksByWorld = new LinkedHashMap<>();
        for (Map<?, ?> worldEntry : configuration.getMapList(GATE_KEY)) {
            readWorldEntry(readMarksByWorld, worldEntry);
        }
        for (Map.Entry<String, Map<Long, Material>> world : readMarksByWorld.entrySet()) {
            gateMarks.replaceWorld(world.getKey(), world.getValue());
        }
        logger.info("Loaded " + gateMarks.getMarkedBlockCount() + " start gate marks from "
                + gatesFile.getName());
    }

    public void save(StartGateMarks gateMarks) {
        try {
            write(gateMarks);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + gatesFile.getName() + ", the start gate is not persisted",
                    failure);
        }
    }

    public void clearEveryMark(StartGateMarks gateMarks) throws IOException {
        gateMarks.clear();
        write(gateMarks);
    }

    public void write(StartGateMarks gateMarks) throws IOException {
        YamlConfiguration configuration = new YamlConfiguration();
        List<Map<String, Object>> worldEntries = new ArrayList<>();
        for (String worldName : gateMarks.getWorldNames()) {
            Map<Long, Material> marksOfWorld = gateMarks.getMarks(worldName);
            if (marksOfWorld.isEmpty()) {
                continue;
            }
            Map<String, Object> worldEntry = new LinkedHashMap<>();
            worldEntry.put(WORLD_NAME_KEY, worldName);
            worldEntry.put(MARKS_KEY, toLines(marksOfWorld));
            worldEntries.add(worldEntry);
        }
        configuration.set(GATE_KEY, worldEntries);
        File parentDirectory = gatesFile.getParentFile();
        if (parentDirectory != null && !parentDirectory.isDirectory() && !parentDirectory.mkdirs()) {
            throw new IOException("Could not create the directory of " + gatesFile.getName());
        }
        configuration.save(gatesFile);
    }

    private void readWorldEntry(Map<String, Map<Long, Material>> readMarksByWorld, Map<?, ?> worldEntry) {
        Object worldName = worldEntry.get(WORLD_NAME_KEY);
        Object marks = worldEntry.get(MARKS_KEY);
        if (!(worldName instanceof String readableWorldName) || !(marks instanceof List<?> readableMarks)) {
            logger.warning("Skipped an unreadable gate entry in " + gatesFile.getName());
            return;
        }
        Map<Long, Material> materialsByPosition = readMarksByWorld
                .computeIfAbsent(readableWorldName, world -> new LinkedHashMap<>());
        int unreadableMarkCount = 0;
        for (Object mark : readableMarks) {
            if (!(mark instanceof String markLine)) {
                unreadableMarkCount++;
                continue;
            }
            StartGateMarkLine parsedMark = StartGateMarkLine.parse(markLine).orElse(null);
            if (parsedMark == null) {
                unreadableMarkCount++;
                continue;
            }
            materialsByPosition.put(parsedMark.encodedPosition(), parsedMark.material());
        }
        if (unreadableMarkCount > 0) {
            logger.warning("Skipped " + unreadableMarkCount + " unreadable gate marks of the world "
                    + readableWorldName + " in " + gatesFile.getName());
        }
    }

    private static List<String> toLines(Map<Long, Material> marksOfWorld) {
        List<String> lines = new ArrayList<>(marksOfWorld.size());
        for (Map.Entry<Long, Material> mark : marksOfWorld.entrySet()) {
            lines.add(StartGateMarkLine.format(mark.getKey(), mark.getValue()));
        }
        return lines;
    }
}
