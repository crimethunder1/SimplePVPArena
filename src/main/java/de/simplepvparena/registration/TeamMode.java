package de.simplepvparena.registration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum TeamMode {

    FREE_CHOICE("free-choice"),
    RANDOM("random");

    public static final TeamMode DEFAULT_TEAM_MODE = FREE_CHOICE;

    private final String configurationName;

    TeamMode(String configurationName) {
        this.configurationName = configurationName;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public TeamMode switched() {
        return this == FREE_CHOICE ? RANDOM : FREE_CHOICE;
    }

    public static Optional<TeamMode> fromConfigurationName(String writtenName) {
        if (writtenName == null) {
            return Optional.empty();
        }
        String normalizedName = writtenName.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.configurationName.equals(normalizedName))
                .findFirst();
    }

    public static List<String> configurationNames() {
        return Arrays.stream(values()).map(TeamMode::getConfigurationName).toList();
    }
}
