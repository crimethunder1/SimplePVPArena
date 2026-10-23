package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaConfigurationWriter;
import de.simplepvparena.arena.ArenaRegistry;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ArenaConfigurationFileWriter implements ArenaConfigurationWriter {

    private final ArenaRegistry arenaRegistry;
    private final Supplier<? extends FileConfiguration> configurationSource;
    private final File configurationFile;
    private final Logger logger;

    public ArenaConfigurationFileWriter(ArenaRegistry arenaRegistry,
                                        Supplier<? extends FileConfiguration> configurationSource,
                                        File configurationFile, Logger logger) {
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void write() throws IOException {
        FileConfiguration configuration = configurationSource.get();
        arenaRegistry.saveToConfiguration(configuration);
        try {
            configuration.save(configurationFile);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + configurationFile.getName() + ", the arena configuration "
                    + "holds only until the next restart or reload of the configuration", failure);
            throw failure;
        }
    }
}
