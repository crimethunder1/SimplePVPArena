package de.simplepvparena.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ArenaSpawnType {

    FIRST_TEAM("team1", "first team spawn"),
    SECOND_TEAM("team2", "second team spawn"),
    TRIBUNE("tribune", "tribune spawn");

    private final String argumentName;
    private final String displayName;

    ArenaSpawnType(String argumentName, String displayName) {
        this.argumentName = argumentName;
        this.displayName = displayName;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<ArenaSpawnType> fromArgument(String argument) {
        if (argument == null) {
            return Optional.empty();
        }
        String normalizedArgument = argument.toLowerCase(Locale.ROOT);
        for (ArenaSpawnType spawnType : values()) {
            if (spawnType.argumentName.equals(normalizedArgument)) {
                return Optional.of(spawnType);
            }
        }
        return Optional.empty();
    }

    public static List<String> getArgumentNames() {
        List<String> argumentNames = new ArrayList<>();
        for (ArenaSpawnType spawnType : values()) {
            argumentNames.add(spawnType.argumentName);
        }
        return List.copyOf(argumentNames);
    }
}
