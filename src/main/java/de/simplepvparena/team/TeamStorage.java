package de.simplepvparena.team;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TeamStorage {

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
        List<String> skippedTeamNames = teamRegistry.loadFromConfiguration(
                YamlConfiguration.loadConfiguration(teamsFile));
        for (String skippedTeamName : skippedTeamNames) {
            logger.warning("Skipped unreadable team " + skippedTeamName + " in " + teamsFile.getName());
        }
        logger.info("Loaded " + teamRegistry.getTeamCount() + " teams from " + teamsFile.getName());
    }

    public void save(TeamRegistry teamRegistry) {
        YamlConfiguration configuration = new YamlConfiguration();
        teamRegistry.saveToConfiguration(configuration);
        File parentDirectory = teamsFile.getParentFile();
        if (parentDirectory != null && !parentDirectory.isDirectory() && !parentDirectory.mkdirs()) {
            logger.severe("Could not create the directory of " + teamsFile.getName() + ", teams are not persisted");
            return;
        }
        try {
            configuration.save(teamsFile);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + teamsFile.getName(), failure);
        }
    }
}
