package de.simplepvparena.config;

import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.border.ConfirmedBorderParticleWriter;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfirmedBorderParticleConfigurationWriter implements ConfirmedBorderParticleWriter {

    private final Supplier<? extends FileConfiguration> configurationSource;
    private final File configurationFile;
    private final Logger logger;

    public ConfirmedBorderParticleConfigurationWriter(Supplier<? extends FileConfiguration> configurationSource,
                                                      File configurationFile, Logger logger) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void write(boolean enabled) throws IOException {
        FileConfiguration configuration = configurationSource.get();
        configuration.set(ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH, enabled);
        try {
            configuration.save(configurationFile);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + configurationFile.getName() + ", the value " + enabled
                    + " of " + ConfirmedBorderParticleSettings.ENABLED_CONFIGURATION_PATH + " holds only until the "
                    + "next restart or reload of the configuration", failure);
            throw failure;
        }
    }
}
