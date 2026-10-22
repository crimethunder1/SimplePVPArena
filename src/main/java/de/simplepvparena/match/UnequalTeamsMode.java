package de.simplepvparena.match;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum UnequalTeamsMode {

    VOTE("vote"),
    ALL("all");

    private final String configurationName;

    UnequalTeamsMode(String configurationName) {
        this.configurationName = configurationName;
    }

    public String getConfigurationName() {
        return configurationName;
    }

    public static Optional<UnequalTeamsMode> fromConfigurationName(String writtenName) {
        if (writtenName == null) {
            return Optional.empty();
        }
        String normalizedName = writtenName.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.configurationName.equals(normalizedName))
                .findFirst();
    }

    public static List<String> configurationNames() {
        return Arrays.stream(values()).map(UnequalTeamsMode::getConfigurationName).toList();
    }
}
