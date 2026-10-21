package de.simplepvparena.config;

import de.simplepvparena.arena.ArenaCleaningParticleSettings;
import de.simplepvparena.border.ConfirmedBorderParticleSettings;
import de.simplepvparena.match.TeamColourSettings;
import de.simplepvparena.message.ConfigMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTextsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void reportsEveryBrokenEntryWithTheWordingOfBefore() {
        PluginSettings.read(configurationWithEveryValueBroken(), problems::add);

        assertEquals(List.of(
                "match.round-timeout-seconds must be at least one second, falling back to 300 seconds",
                "match.start-gate-duration-seconds must be at least 1 second, falling back to 10 seconds",
                "match.team-colours.first-team is no colour a name can be shown in, falling back to red. Usable "
                        + "colours: " + Messages.join(TeamColourSettings.knownColourNames()),
                "match.team-size must be a whole number between 1 and 8 players, falling back to 2 players",
                "arena.baseline-depth-below-floor must lie between 0 and 64 blocks, falling back to 11 blocks",
                "arena.clean-particle.total-duration-seconds cannot be negative, falling back to 6.0 seconds. Use "
                        + "zero to clean without the two coloured phases",
                "arena.clean-particle.columns-per-tick cannot be negative, falling back to 24 columns per tick. Use "
                        + "zero to draw none",
                "arena.shape-maximum-cells must be at least one cell, falling back to 50000",
                "arena.safety-zone-radius cannot be negative, falling back to 64 blocks. Use zero to switch the "
                        + "safety zone off",
                "arena.effect-margin must be a whole number between 0 and 512 blocks, falling back to 96 blocks",
                "border.confirmed-particle.height cannot be negative, falling back to 0.25 blocks. Use zero for the "
                        + "shortest possible marker",
                "language fr is no shipped language, falling back to en. Shipped languages: en and de"), problems);
    }

    @Test
    void listsTheUsableColoursWithAndBeforeTheLastOne() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "pink");

        PluginSettings.read(configuration, problems::add);

        List<String> colourNames = TeamColourSettings.knownColourNames();
        String usableColours = problems.get(0).substring(problems.get(0).indexOf("Usable colours: ")
                + "Usable colours: ".length());
        assertTrue(usableColours.endsWith(" and " + colourNames.get(colourNames.size() - 1)), usableColours);
        assertEquals(colourNames.size() - 2, usableColours.split(", ", -1).length - 1, usableColours);
    }

    @Test
    void listsTheUsableColoursInAlphabeticalOrder() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.SECOND_TEAM_COLOUR_PATH, "pink");

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("match.team-colours.second-team is no colour a name can be shown in, falling back to "
                + "aqua. Usable colours: aqua, black, blue, dark_aqua, dark_blue, dark_gray, dark_green, dark_purple, "
                + "dark_red, gold, gray, green, light_purple, red, white and yellow"), problems);
    }

    @Test
    void reportsTwoTeamsInTheSameColourWithTheWordingOfBefore() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "gold");
        configuration.set(PluginSettings.SECOND_TEAM_COLOUR_PATH, "gold");

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("match.team-colours gives both teams the colour gold, which would be no colouring at "
                + "all, falling back to red and aqua"), problems);
    }

    @Test
    void writesAFallbackOfOneInTheSingular() {
        Placeholder path = Placeholder.of("path", "some.entry");
        Placeholder minimum = Placeholder.of("minimum", 0);
        Placeholder maximum = Placeholder.of("maximum", 9);

        assertEquals("some.entry must lie between 0 and 9 blocks, falling back to 1 block",
                Messages.plural(ConfigMessage.BASELINE_DEPTH_OUT_OF_RANGE, 1, path, maximum));
        assertEquals("some.entry cannot be negative, falling back to 1 block. Use zero to switch the safety zone off",
                Messages.plural(ConfigMessage.SAFETY_ZONE_RADIUS_NEGATIVE, 1, path));
        assertEquals("some.entry must be a whole number between 0 and 9 blocks, falling back to 1 block",
                Messages.plural(ConfigMessage.EFFECT_MARGIN_OUT_OF_RANGE, 1, path, minimum, maximum));
        assertEquals("some.entry must be a whole number between 0 and 9 players, falling back to 1 player",
                Messages.plural(ConfigMessage.TEAM_SIZE_OUT_OF_RANGE, 1, path, minimum, maximum));
        assertEquals("some.entry cannot be negative, falling back to 1 column per tick. Use zero to draw none",
                Messages.plural(ConfigMessage.CLEANING_COLUMNS_NEGATIVE, 1, path));
        assertEquals("some.entry must be at least 1 second, falling back to 1 second",
                Messages.plural(ConfigMessage.START_GATE_DURATION_TOO_SHORT, 1, path, Placeholder.of("minimum", 1)));
        assertEquals("some.entry must be at least one second, falling back to 1 second",
                Messages.plural(ConfigMessage.ROUND_TIMEOUT_TOO_SHORT, 1, path));
    }

    @Test
    void namesTheLargerTeamsAtTheStartInTheSingularAndThePlural() {
        Placeholder teamSize = Placeholder.of("teamSize", 2);
        Placeholder path = Placeholder.of("path", PluginSettings.TEAM_SIZE_PATH);

        assertEquals("1 team has more than 2 players: Bravo (3). The team size 2 of match.team-size in config.yml "
                        + "applies anyway since the start, this team keeps its players and every online player of it "
                        + "fights",
                Messages.plural(ConfigMessage.LARGER_TEAMS_AT_START, 1,
                        Placeholder.of("teams", "1 team has more than 2 players: Bravo (3)"), teamSize, path));
        assertEquals("2 teams have more than 2 players: Bravo (3) and Charlie (4). The team size 2 of "
                        + "match.team-size in config.yml applies anyway since the start, these teams keep their "
                        + "players and every online player of them fights",
                Messages.plural(ConfigMessage.LARGER_TEAMS_AT_START, 2,
                        Placeholder.of("teams", "2 teams have more than 2 players: Bravo (3) and Charlie (4)"),
                        teamSize, path));
    }

    @Test
    void writesTheReloadAnswerWithItsBrokenEntriesLineByLine() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);
        ConfigurationReloadService reloadService = new ConfigurationReloadService(() -> configuration, () -> { },
                new RecordingLiveConfiguration(), problem -> { });

        assertEquals(List.of("config.yml was reloaded and applied",
                "  the language and the language files in the lang folder, for every text shown from now on",
                "  the round reset delay, the round timeout, the sudden death damage and whether the final is fought "
                        + "without a time limit",
                "  the seconds the start gate takes at the beginning of a round",
                "  the seconds of the countdown right before the fight, from the next round on",
                "  the seconds a fighter who lost the connection has to come back, for every connection lost from "
                        + "now on",
                "  the team size, unless a tournament is in progress or a team has more players than a smaller new "
                        + "size",
                "  whether the auto match mode is on, the seconds before an auto match starts and the pause between "
                        + "two auto matches",
                "  the cleaning and baseline batch sizes and the particles of a running cleaning",
                "  the depth below the floor the next baseline is taken to",
                "  the cell limit of the arena calculation",
                "  the radius of the safety zone",
                "  the margin of the area of effect around the fight area",
                "  whether joining players are sent to the tribune",
                "  whether players hand in their inventory in the area of effect, false gives every inventory back "
                        + "at once",
                "  the border particles, their limits and the confirmed border marker",
                "The calculated fight area, the spawns, the border blocks, the teams and a running match stay "
                        + "untouched",
                "Entries that could not be used and fell back to their default:",
                "  arena.safety-zone-radius cannot be negative, falling back to 64 blocks. Use zero to switch the "
                        + "safety zone off"), reloadService.reloadFromFile().describe());
    }

    static MemoryConfiguration configurationWithEveryValueBroken() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ROUND_TIMEOUT_SECONDS_PATH, 0);
        configuration.set(PluginSettings.START_GATE_DURATION_SECONDS_PATH, 0);
        configuration.set(PluginSettings.FIRST_TEAM_COLOUR_PATH, "pink");
        configuration.set(PluginSettings.TEAM_SIZE_PATH, 12);
        configuration.set(PluginSettings.ARENA_BASELINE_DEPTH_BELOW_FLOOR_PATH, 99);
        configuration.set(ArenaCleaningParticleSettings.TOTAL_DURATION_CONFIGURATION_PATH, -1.0D);
        configuration.set(ArenaCleaningParticleSettings.COLUMNS_PER_TICK_CONFIGURATION_PATH, -1);
        configuration.set(PluginSettings.ARENA_MAXIMUM_CELLS_PATH, 0);
        configuration.set(PluginSettings.ARENA_SAFETY_ZONE_RADIUS_PATH, -1);
        configuration.set(PluginSettings.ARENA_EFFECT_MARGIN_PATH, 9999);
        configuration.set(ConfirmedBorderParticleSettings.HEIGHT_CONFIGURATION_PATH, -1.0D);
        configuration.set(PluginSettings.LANGUAGE_PATH, "fr");
        return configuration;
    }
}
