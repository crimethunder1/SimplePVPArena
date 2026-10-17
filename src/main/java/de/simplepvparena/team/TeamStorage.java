package de.simplepvparena.team;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TeamStorage {

    public static final String BROKEN_FILE_EXTENSION = ".broken";

    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";

    private final File teamsFile;
    private final Logger logger;

    public TeamStorage(File teamsFile, Logger logger) {
        this.teamsFile = Objects.requireNonNull(teamsFile, "teamsFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void load(TeamRegistry teamRegistry) {
        if (!teamsFile.isFile()) {
            teamRegistry.clear();
            logger.info("No " + teamsFile.getName() + " found, starting with an empty team registry");
            return;
        }
        byte[] storedContent;
        try {
            storedContent = Files.readAllBytes(teamsFile.toPath());
        } catch (IOException failure) {
            teamRegistry.clear();
            logger.log(Level.SEVERE, "Could not read " + teamsFile.getName()
                    + ", starting with an empty team registry", failure);
            return;
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(new String(storedContent, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException unreadableFile) {
            teamRegistry.clear();
            logger.warning(teamsFile.getName() + " could not be read (" + describe(unreadableFile)
                    + "), not a single team was loaded, " + keepBrokenFile(storedContent));
            return;
        }
        List<String> skippedEntries = teamRegistry.loadFromConfiguration(configuration);
        if (!skippedEntries.isEmpty()) {
            warnAboutSkippedEntries(skippedEntries, keepBrokenFile(storedContent));
        }
        logger.info("Loaded " + teamRegistry.getTeamCount() + " teams from " + teamsFile.getName());
    }

    public void save(TeamRegistry teamRegistry) {
        YamlConfiguration configuration = new YamlConfiguration();
        teamRegistry.saveToConfiguration(configuration);
        String serializedTeams = configuration.saveToString();
        Path targetFile = teamsFile.toPath().toAbsolutePath();
        Path directory = targetFile.getParent();
        try {
            Files.createDirectories(directory);
        } catch (IOException | RuntimeException failure) {
            logger.severe("Could not create the directory of " + teamsFile.getName() + ", teams are not persisted");
            return;
        }
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(directory, teamsFile.getName(), TEMPORARY_FILE_EXTENSION);
            Files.writeString(temporaryFile, serializedTeams, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, targetFile);
        } catch (IOException | RuntimeException failure) {
            removeTemporaryFile(temporaryFile, failure);
            logger.log(Level.SEVERE, "Could not write " + teamsFile.getName(), failure);
        }
    }

    private void warnAboutSkippedEntries(List<String> skippedEntries, String brokenFileNotice) {
        int skippedEntryCount = skippedEntries.size();
        List<String> lines = new ArrayList<>();
        lines.add(skippedEntryCount + (skippedEntryCount == 1 ? " entry" : " entries") + " of "
                + teamsFile.getName() + " could not be read and " + (skippedEntryCount == 1 ? "was" : "were")
                + " skipped, " + brokenFileNotice + ":");
        for (String skippedEntry : skippedEntries) {
            lines.add("  " + skippedEntry);
        }
        logger.warning(String.join(System.lineSeparator(), lines));
    }

    private String keepBrokenFile(byte[] storedContent) {
        Path brokenFile = teamsFile.toPath().resolveSibling(teamsFile.getName() + BROKEN_FILE_EXTENSION);
        try {
            Files.write(brokenFile, storedContent);
            return "the file as it was read is kept as " + brokenFile.getFileName();
        } catch (IOException | RuntimeException failure) {
            return "the file could not be copied to " + brokenFile.getFileName() + " (" + describe(failure)
                    + "), the next change of the teams overwrites it";
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
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private static String describe(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
