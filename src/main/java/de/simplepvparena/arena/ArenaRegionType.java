package de.simplepvparena.arena;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum ArenaRegionType {

    FIGHT("fight"),
    SPECTATOR_BOX("spectatorbox"),
    TRIBUNE("tribune");

    private final String commandArgument;

    ArenaRegionType(String commandArgument) {
        this.commandArgument = commandArgument;
    }

    public String getCommandArgument() {
        return commandArgument;
    }

    public static Optional<ArenaRegionType> fromCommandArgument(String argument) {
        if (argument == null) {
            return Optional.empty();
        }
        String normalized = argument.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(regionType -> regionType.commandArgument.equals(normalized))
                .findFirst();
    }
}
