package de.simplepvparena.arena;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum RegionCorner {

    CORNER_ONE("corner1"),
    CORNER_TWO("corner2");

    private final String commandArgument;

    RegionCorner(String commandArgument) {
        this.commandArgument = commandArgument;
    }

    public String getCommandArgument() {
        return commandArgument;
    }

    public static Optional<RegionCorner> fromCommandArgument(String argument) {
        if (argument == null) {
            return Optional.empty();
        }
        String normalized = argument.toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(corner -> corner.commandArgument.equals(normalized))
                .findFirst();
    }
}
