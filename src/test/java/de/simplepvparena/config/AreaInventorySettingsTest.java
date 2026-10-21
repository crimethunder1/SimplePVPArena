package de.simplepvparena.config;

import de.simplepvparena.arena.AreaInventoryService;
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

class AreaInventorySettingsTest {

    private static final String FALLBACK_PROBLEM = "arena.area-inventory.enabled must be true or false, "
            + "falling back to true";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsTheInventoryHandedInWithinTheAreaOfEffect() {
        PluginSettings defaults = PluginSettings.defaults();

        assertTrue(defaults.areaInventoryEnabled());
        assertTrue(AreaInventoryService.DEFAULT_ENABLED);
        assertEquals("arena.area-inventory.enabled", PluginSettings.ARENA_AREA_INVENTORY_ENABLED_PATH);
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains(PluginSettings.ARENA_AREA_INVENTORY_ENABLED_PATH));
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add));
        assertEquals(List.of(), problems);
    }

    @Test
    void shipsTheEntryWithAnEnglishCommentInTheArenaSection() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("  area-inventory:");
        assertTrue(sectionIndex > lines.indexOf("arena:"), "config.yml has to hold arena.area-inventory: " + lines);
        assertEquals("  # Inventories of the players inside the area of effect.", lines.get(sectionIndex - 1));
        assertEquals("    # When true, every player who enters the area of effect, admins included, hands in the "
                + "whole inventory and gets it back on leaving. Items taken inside are removed on leaving. false "
                + "keeps the inventory with the player.", lines.get(sectionIndex + 1));
        assertEquals("    enabled: true", lines.get(sectionIndex + 2));
    }

    @Test
    void readsBothTruthValuesWithoutAProblem() {
        assertFalse(PluginSettings.read(configurationWithSwitch(false), problems::add).areaInventoryEnabled());
        assertTrue(PluginSettings.read(configurationWithSwitch(true), problems::add).areaInventoryEnabled());
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToHandedInForAValueThatIsNoTruthValueAndReportsIt() {
        for (Object writtenValue : new Object[] {"off", 0, 1.5D, "false"}) {
            problems.clear();

            PluginSettings settings = PluginSettings.read(configurationWithSwitch(writtenValue), problems::add);

            assertTrue(settings.areaInventoryEnabled(), "a value of " + writtenValue);
            assertEquals(List.of(FALLBACK_PROBLEM), problems, "a value of " + writtenValue + " is reported");
        }
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntryInGerman() {
        PluginSettings.read(configurationWithSwitch("aus"), problems::add);

        assertEquals(List.of("arena.area-inventory.enabled muss true oder false sein, stattdessen gilt true"),
                problems);
    }

    @Test
    void theStartAndEveryReloadHandTheSwitchToTheService() {
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWithSwitch(false));
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                liveConfiguration, problems::add);

        reloadService.applyStoredConfiguration();
        assertFalse(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.AREA_INVENTORY_ENABLED));

        fileOnDisk.set(configurationWithSwitch(true));
        reloadService.reloadFromFile();
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.AREA_INVENTORY_ENABLED));

        fileOnDisk.set(configurationWithSwitch("broken"));
        reloadService.reloadFromFile();
        assertTrue(liveConfiguration.booleanValueOf(RecordingLiveConfiguration.AREA_INVENTORY_ENABLED),
                "a broken value falls back to handed in");
        assertEquals(List.of(FALLBACK_PROBLEM), problems);
    }

    @Test
    void theReloadAnswerNamesTheSwitchInItsOwnCategory() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  whether players hand in their inventory in the area of effect, false gives "
                + "every inventory back at once"), lines.toString());
    }

    private static MemoryConfiguration configurationWithSwitch(Object enabled) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(PluginSettings.ARENA_AREA_INVENTORY_ENABLED_PATH, enabled);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = AreaInventorySettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
