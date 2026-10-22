package de.simplepvparena.config;

import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.visual.ConfiguredVisuals;
import de.simplepvparena.visual.CustomVisualSettings;
import de.simplepvparena.visual.RecordingCustomVisuals;
import de.simplepvparena.visual.VanillaVisuals;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StationItemSettingsTest {

    private final List<String> problems = new ArrayList<>();

    @Test
    void namesThePathAndShipsItEmpty() {
        assertEquals("items-adder.station-item", CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH);
        assertEquals("simplepvparena:station", CustomVisualSettings.SUGGESTED_STATION_ITEM_IDENTIFIER);
        assertTrue(PluginSettings.CONFIGURATION_PATHS.contains(CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH));
        assertEquals("", CustomVisualSettings.defaults().stationItemIdentifier());
        assertEquals("", PluginSettings.defaults().customVisualSettings().stationItemIdentifier());
    }

    @Test
    void theShippedFileHoldsTheValueWithAnEnglishCommentAndTheSuggestionBelowTheTotem() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int valueIndex = lines.indexOf("  station-item: ''");
        assertTrue(valueIndex > 0, "config.yml has to hold station-item: " + lines);
        assertEquals("  totem-item: ''", lines.get(valueIndex - 2));
        String comment = lines.get(valueIndex - 1);
        assertTrue(comment.startsWith("  # Item of ItemsAdder floating above every registration station."), comment);
        assertTrue(comment.endsWith("Suggested ID: simplepvparena:station."), comment);
    }

    @Test
    void readsTheIdentifierAndStripsItsSpaces() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH, "  simplepvparena:station ");

        CustomVisualSettings settings = PluginSettings.read(configuration, problems::add).customVisualSettings();

        assertEquals("simplepvparena:station", settings.stationItemIdentifier());
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToEmptyForAValueThatIsNoTextAndNamesTheSuggestion() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH, 7);

        CustomVisualSettings settings = PluginSettings.read(configuration, problems::add).customVisualSettings();

        assertEquals("", settings.stationItemIdentifier());
        assertEquals(List.of("items-adder.station-item must be an ID in quotes such as simplepvparena:station or "
                + "empty, falling back to empty, the own title or effect of the plugin is used"), problems);
    }

    @Test
    void theStartAndEveryReloadHandTheIdentifierOn() {
        MemoryConfiguration first = new MemoryConfiguration();
        first.set(CustomVisualSettings.STATION_ITEM_CONFIGURATION_PATH, "simplepvparena:station");
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(first);
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                liveConfiguration, problems::add);

        reloadService.applyStoredConfiguration();
        assertEquals("simplepvparena:station", liveConfiguration.customVisualSettings().stationItemIdentifier());

        fileOnDisk.set(new MemoryConfiguration());
        reloadService.reloadFromFile();
        assertEquals("", liveConfiguration.customVisualSettings().stationItemIdentifier());
    }

    @Test
    void theConfiguredVisualsHandOutTheStationItemOrNothing() {
        FakeItemStack stationItem = new FakeItemStack("station item");
        List<String> warnings = new ArrayList<>();
        ConfiguredVisuals configuredVisuals = new ConfiguredVisuals(new RecordingCustomVisuals()
                .withItem("simplepvparena:station", stationItem), warnings::add);
        assertEquals(Optional.empty(), configuredVisuals.findStationItem());

        configuredVisuals.applySettings(stationSettings("simplepvparena:station"));
        assertEquals(Optional.of(stationItem), configuredVisuals.findStationItem());

        configuredVisuals.applySettings(stationSettings("simplepvparena:lost_station"));
        assertEquals(Optional.empty(), configuredVisuals.findStationItem());
        assertEquals(List.of("ItemsAdder has no item simplepvparena:lost_station, set in items-adder.station-item "
                + "of config.yml, the own effect of the plugin is used instead"), warnings);
    }

    @Test
    void withoutItemsAdderThereIsNeverAStationItem() {
        List<String> warnings = new ArrayList<>();
        ConfiguredVisuals configuredVisuals = new ConfiguredVisuals(new VanillaVisuals(), warnings::add);
        configuredVisuals.applySettings(stationSettings("simplepvparena:station"));

        assertEquals(Optional.empty(), configuredVisuals.findStationItem());
        assertEquals(List.of(), warnings);
    }

    private static CustomVisualSettings stationSettings(String stationIdentifier) {
        return new CustomVisualSettings(true, "", "", "", "", "", stationIdentifier);
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = StationItemSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
