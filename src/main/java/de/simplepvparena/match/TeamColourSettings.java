package de.simplepvparena.match;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public record TeamColourSettings(NamedTextColor firstTeamColour, NamedTextColor secondTeamColour) {

    public static final String SECTION_CONFIGURATION_PATH = "match.team-colours";
    public static final String FIRST_TEAM_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".first-team";
    public static final String SECOND_TEAM_CONFIGURATION_PATH = SECTION_CONFIGURATION_PATH + ".second-team";

    public static final String DEFAULT_FIRST_TEAM_COLOUR_NAME = "red";

    public static final String DEFAULT_SECOND_TEAM_COLOUR_NAME = "aqua";

    private static final char NAME_SEPARATOR = '_';
    private static final char NAMESPACE_SEPARATOR = ':';

    public TeamColourSettings {
        Objects.requireNonNull(firstTeamColour, "firstTeamColour");
        Objects.requireNonNull(secondTeamColour, "secondTeamColour");
    }

    public static TeamColourSettings defaults() {
        return new TeamColourSettings(colourOfName(DEFAULT_FIRST_TEAM_COLOUR_NAME).orElseThrow(),
                colourOfName(DEFAULT_SECOND_TEAM_COLOUR_NAME).orElseThrow());
    }

    public static Optional<NamedTextColor> colourOfName(String colourName) {
        if (colourName == null) {
            return Optional.empty();
        }
        String withoutNamespace = colourName.substring(colourName.lastIndexOf(NAMESPACE_SEPARATOR) + 1);
        String normalized = withoutNamespace.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', NAME_SEPARATOR)
                .replace(' ', NAME_SEPARATOR);
        return Optional.ofNullable(NamedTextColor.NAMES.value(normalized));
    }

    public static String nameOf(NamedTextColor colour) {
        return NamedTextColor.NAMES.key(colour);
    }

    public static List<String> knownColourNames() {
        return List.copyOf(NamedTextColor.NAMES.keys());
    }
}
