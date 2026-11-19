package de.simplepvparena.config;

import de.simplepvparena.arena.EffectArea;
import de.simplepvparena.arena.EffectMarginWriter;
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
public final class EffectMarginConfigurationWriter implements EffectMarginWriter {

    private final Supplier<? extends @NonNull FileConfiguration> configurationSource;
    private final File configurationFile;
    private final Logger logger;

    public EffectMarginConfigurationWriter(Supplier<? extends @NonNull FileConfiguration> configurationSource,
                                           File configurationFile, Logger logger) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configurationFile = Objects.requireNonNull(configurationFile, "configurationFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void write(int margin) throws IOException {
        FileConfiguration configuration = configurationSource.get();
        configuration.set(EffectArea.MARGIN_CONFIGURATION_PATH, margin);
        try {
            configuration.save(configurationFile);
        } catch (IOException failure) {
            logger.log(Level.SEVERE, "Could not write " + configurationFile.getName() + ", the value " + margin
                    + " of " + EffectArea.MARGIN_CONFIGURATION_PATH + " holds only until the next restart or reload "
                    + "of the configuration", failure);
            throw failure;
        }
    }
}
