package de.simplepvparena.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfigurationReloadService {

    private final Supplier<ConfigurationSection> configurationSource;
    private final Runnable configurationFileReloader;
    private final LiveConfiguration liveConfiguration;
    private final Consumer<String> problemLogger;

    public ConfigurationReloadService(Supplier<ConfigurationSection> configurationSource,
                                      Runnable configurationFileReloader,
                                      LiveConfiguration liveConfiguration,
                                      Consumer<String> problemLogger) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configurationFileReloader = Objects.requireNonNull(configurationFileReloader,
                "configurationFileReloader");
        this.liveConfiguration = Objects.requireNonNull(liveConfiguration, "liveConfiguration");
        this.problemLogger = Objects.requireNonNull(problemLogger, "problemLogger");
    }

    public ConfigurationReloadReport applyStoredConfiguration() {
        return apply(configurationSource.get(), settings -> settings.applyAtStartTo(liveConfiguration));
    }

    public ConfigurationReloadReport reloadFromFile() {
        configurationFileReloader.run();
        ConfigurationSection configuration = configurationSource.get();
        liveConfiguration.setLanguage(PluginSettings.readLanguage(configuration, problem -> { }));
        return apply(configuration, settings -> settings.applyTo(liveConfiguration));
    }

    private ConfigurationReloadReport apply(ConfigurationSection configuration,
                                            Consumer<PluginSettings> application) {
        List<String> problems = new ArrayList<>();
        application.accept(PluginSettings.read(configuration, problem -> collect(problems, problem)));
        return ConfigurationReloadReport.of(problems);
    }

    private void collect(List<String> problems, String problem) {
        problems.add(problem);
        problemLogger.accept(problem);
    }
}
