package de.simplepvparena.config;

import de.simplepvparena.support.UseLanguage;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementSettingsTest {

    private static final String SIDEBAR_FALLBACK_PROBLEM = "placement.sidebar.enabled must be true or false, "
            + "falling back to true";
    private static final String BOARD_FALLBACK_PROBLEM = "placement.board.enabled must be true or false, "
            + "falling back to true";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheSidebarAndTheBoardSwitchedOn() {
        PluginSettings defaults = PluginSettings.defaults();

        assertTrue(defaults.placementSidebarEnabled());
        assertTrue(defaults.placementBoardEnabled());
        assertEquals("placement.sidebar.enabled", PluginSettings.PLACEMENT_SIDEBAR_ENABLED_PATH);
        assertEquals("placement.board.enabled", PluginSettings.PLACEMENT_BOARD_ENABLED_PATH);
        assertTrue(PluginSettings.CONFIGURATION_PATHS.containsAll(List.of(
                PluginSettings.PLACEMENT_SIDEBAR_ENABLED_PATH, PluginSettings.PLACEMENT_BOARD_ENABLED_PATH)));
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsBothEntriesWithAnEnglishComment() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("placement:");
        assertTrue(sectionIndex > 0, "config.yml has to hold the section placement: " + lines);
        assertEquals("# Live placement of the tournament and of the auto matches.", lines.get(sectionIndex - 1));
        assertEquals(List.of(
                "  # Sidebar for the players inside the area of effect.",
                "  sidebar:",
                "    # When true, every player inside the area of effect sees the round, the running match and the "
                        + "teams still in, or the wins of the auto matches, at the side of the screen. false gives "
                        + "every player the former scoreboard back.",
                "    enabled: true",
                "  # Text board above the place an admin chose with the standings board tile of the arena menu.",
                "  board:",
                "    # When true, the board shows the same and who is out in which round. false removes it, the "
                        + "place stays saved.",
                "    enabled: true"), lines.subList(sectionIndex + 1, sectionIndex + 9));
    }

    @Test
    void readsBothTruthValuesOfBothSwitchesWithoutAProblem() {
        PluginSettings switchedOff = PluginSettings.read(configurationWith(false, false), problems::add);
        PluginSettings onlyTheBoard = PluginSettings.read(configurationWith(false, true), problems::add);

        assertFalse(switchedOff.placementSidebarEnabled());
        assertFalse(switchedOff.placementBoardEnabled());
        assertFalse(onlyTheBoard.placementSidebarEnabled());
        assertTrue(onlyTheBoard.placementBoardEnabled());
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToShownForAValueThatIsNoTruthValueAndReportsIt() {
        for (Object writtenValue : new Object[] {"off", 0, 1.5D, "false"}) {
            problems.clear();

            PluginSettings settings = PluginSettings.read(configurationWith(writtenValue, writtenValue),
                    problems::add);

            assertTrue(settings.placementSidebarEnabled(), "a value of " + writtenValue);
            assertTrue(settings.placementBoardEnabled(), "a value of " + writtenValue);
            assertEquals(List.of(SIDEBAR_FALLBACK_PROBLEM, BOARD_FALLBACK_PROBLEM), problems,
                    "a value of " + writtenValue + " is reported");
        }
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntriesInGerman() {
        PluginSettings.read(configurationWith("aus", true), problems::add);

        assertEquals(List.of("placement.sidebar.enabled muss true oder false sein, stattdessen gilt true"), problems);
    }

    @Test
    void theStartAndEveryReloadHandBothSwitchesOn() {
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWith(false, true));
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                liveConfiguration, problems::add);

        reloadService.applyStoredConfiguration();
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.PLACEMENT_SIDEBAR_ENABLED));
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.PLACEMENT_BOARD_ENABLED));

        fileOnDisk.set(configurationWith(true, false));
        reloadService.reloadFromFile();
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.PLACEMENT_SIDEBAR_ENABLED));
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.PLACEMENT_BOARD_ENABLED));

        fileOnDisk.set(configurationWith(true, "broken"));
        reloadService.reloadFromFile();
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.PLACEMENT_BOARD_ENABLED),
                "a broken value falls back to shown");
        assertEquals(List.of(BOARD_FALLBACK_PROBLEM), problems);
    }

    @Test
    void theReloadAnswerNamesBothSwitchesInTheirOwnCategory() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  whether the placement is shown as a sidebar in the area of effect and as a "
                + "board at the tribune"), lines.toString());
    }

    @Test
    @UseLanguage("de")
    void theReloadAnswerNamesBothSwitchesInGerman() {
        assertTrue(ConfigurationReloadReport.describeReloadedCategories().contains("ob die Platzierung als "
                + "Seitenleiste im Wirkungsbereich und als Tafel an der Tribüne gezeigt wird"));
    }

    private static MemoryConfiguration configurationWith(Object sidebarEnabled, Object boardEnabled) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.PLACEMENT_SIDEBAR_ENABLED_PATH, sidebarEnabled);
        configuration.set(PluginSettings.PLACEMENT_BOARD_ENABLED_PATH, boardEnabled);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = PlacementSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
