package de.simplepvparena.config;

import de.simplepvparena.tournament.AutoMatchModeWriter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AutoMatchConfigurationWriter implements AutoMatchModeWriter {

    private final Supplier<? extends FileConfiguration> configurationSource;
    private final File configurationFile;
    private final Logger logger;

    public AutoMatchConfigurationWriter(Supplier<? extends FileConfiguration> configurationSource,
                                        File configurationFile, Logger logger) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void write(boolean enabled) throws IOException {
        FileConfiguration configuration = configurationSource.get();
        configuration.set(PluginSettings.AUTO_MATCH_ENABLED_PATH, enabled);
        try {
            configuration.save(configurationFile);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + configurationFile.getName() + ", the value " + enabled
                    + " of " + PluginSettings.AUTO_MATCH_ENABLED_PATH + " holds only until the next restart or reload "
                    + "of the configuration", failure);
            throw failure;
        }
    }
}
