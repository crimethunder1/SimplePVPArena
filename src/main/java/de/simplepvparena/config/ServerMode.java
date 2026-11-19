package de.simplepvparena.config;

import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@NullMarked
public enum ServerMode {

    EVENT("event"),
    AUTO("auto");

    public static final ServerMode DEFAULT_MODE = EVENT;

    private final String configurationName;

    ServerMode(String configurationName) {
        this.configurationName = configurationName;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public ServerMode switched() {
        return this == EVENT ? AUTO : EVENT;
    }

    public boolean allowsAutoMatches() {
        return this == AUTO;
    }

    public boolean keepsAreaInventories() {
        return this == AUTO;
    }

    public boolean sendsJoiningPlayersToTheTribune() {
        return this == EVENT;
    }

    public static Optional<ServerMode> fromConfigurationName(String writtenName) {
        String normalizedName = writtenName.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.configurationName.equals(normalizedName))
                .findFirst();
    }

    public static List<String> configurationNames() {
        return Arrays.stream(values()).map(ServerMode::getConfigurationName).toList();
    }
}
