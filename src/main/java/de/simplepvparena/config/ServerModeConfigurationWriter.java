package de.simplepvparena.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@NullMarked
public final class ServerModeConfigurationWriter {

    private final Supplier<? extends @NonNull FileConfiguration> configurationSource;
    private final File configurationFile;
    private final Logger logger;

    public ServerModeConfigurationWriter(Supplier<? extends @NonNull FileConfiguration> configurationSource,
                                         File configurationFile, Logger logger) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void write(ServerMode serverMode) throws IOException {
        FileConfiguration configuration = configurationSource.get();
        configuration.set(PluginSettings.SERVER_MODE_PATH, serverMode.getConfigurationName());
        try {
            configuration.save(configurationFile);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + configurationFile.getName() + ", the value "
                    + serverMode.getConfigurationName() + " of " + PluginSettings.SERVER_MODE_PATH
                    + " holds only until the next restart or reload of the configuration", failure);
            throw failure;
        }
    }
}
