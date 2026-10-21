package de.simplepvparena.config;

import de.simplepvparena.support.UseLanguage;
import de.simplepvparena.visual.CustomVisualSettings;
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

class ItemsAdderSettingsTest {

    private static final String ENABLED_FALLBACK_PROBLEM = "items-adder.enabled must be true or false, "
            + "falling back to true";

    private final List<String> problems = new ArrayList<>();

    @Test
    void shipsItemsAdderSwitchedOnWithoutAnyIdentifier() {
        CustomVisualSettings defaults = PluginSettings.defaults().customVisualSettings();

        assertEquals(new CustomVisualSettings(true, "", "", "", "", ""), defaults);
        assertEquals(CustomVisualSettings.defaults(), defaults);
        assertEquals(defaults, PluginSettings.read(new MemoryConfiguration(), problems::add).customVisualSettings());
        assertEquals(List.of(), problems);
    }

    @Test
    void namesEveryPathTheStartAndTheReloadRead() {
        assertEquals("items-adder.enabled", CustomVisualSettings.ENABLED_CONFIGURATION_PATH);
        assertEquals("items-adder.countdown-images.three", CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH);
        assertEquals("items-adder.countdown-images.two", CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH);
        assertEquals("items-adder.countdown-images.one", CustomVisualSettings.ONE_IMAGE_CONFIGURATION_PATH);
        assertEquals("items-adder.countdown-images.fight", CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH);
        assertEquals("items-adder.totem-item", CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH);
        assertTrue(PluginSettings.CONFIGURATION_PATHS.containsAll(List.of(
                CustomVisualSettings.ENABLED_CONFIGURATION_PATH, CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH,
                CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH, CustomVisualSettings.ONE_IMAGE_CONFIGURATION_PATH,
                CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH,
                CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH)));
    }

    @Test
    void shipsTheSectionWithEnglishCommentsTheSuggestedIdentifiersAndTheRestartNote() throws IOException {
        List<String> lines = shippedConfigurationLines();

        int sectionIndex = lines.indexOf("items-adder:");
        assertTrue(sectionIndex > 0, "config.yml has to hold the section items-adder: " + lines);
        assertTrue(lines.get(sectionIndex - 1).startsWith("# Optional images and items from the ItemsAdder plugin."));
        assertTrue(lines.get(sectionIndex - 1).contains("the plugin shows its own titles and effects as before"));
        assertTrue(lines.get(sectionIndex + 1).contains("needs a server restart"), lines.get(sectionIndex + 1));
        assertEquals("  enabled: true", lines.get(sectionIndex + 2));
        assertTrue(lines.get(sectionIndex + 3).contains("Suggested IDs: simplepvparena:countdown_3, "
                + "simplepvparena:countdown_2, simplepvparena:countdown_1, simplepvparena:countdown_fight."));
        assertEquals(List.of("  countdown-images:", "    three: ''", "    two: ''", "    one: ''", "    fight: ''"),
                lines.subList(sectionIndex + 4, sectionIndex + 9));
        assertTrue(lines.get(sectionIndex + 9).contains("Suggested ID: simplepvparena:totem."));
        assertEquals("  totem-item: ''", lines.get(sectionIndex + 10));
    }

    @Test
    void readsTheSwitchAndEveryIdentifierWithoutAProblem() {
        MemoryConfiguration configuration = configurationWith(false, "simplepvparena:countdown_3",
                "simplepvparena:countdown_2", "simplepvparena:countdown_1", "simplepvparena:countdown_fight",
                "simplepvparena:totem");

        CustomVisualSettings settings = PluginSettings.read(configuration, problems::add).customVisualSettings();

        assertEquals(new CustomVisualSettings(false, "simplepvparena:countdown_3", "simplepvparena:countdown_2",
                "simplepvparena:countdown_1", "simplepvparena:countdown_fight", "simplepvparena:totem"), settings);
        assertEquals(List.of(), problems);
    }

    @Test
    void stripsTheSpacesAroundAnIdentifierAndKeepsAnEmptyOneEmpty() {
        MemoryConfiguration configuration = configurationWith(true, "  simplepvparena:countdown_3 ", "", "   ",
                "", "\tsimplepvparena:totem");

        CustomVisualSettings settings = PluginSettings.read(configuration, problems::add).customVisualSettings();

        assertEquals(new CustomVisualSettings(true, "simplepvparena:countdown_3", "", "", "",
                "simplepvparena:totem"), settings);
        assertEquals(List.of(), problems);
    }

    @Test
    void fallsBackToSwitchedOnForASwitchThatIsNoTruthValueAndReportsIt() {
        for (Object writtenValue : new Object[] {"off", 0, 1.5D, "false"}) {
            problems.clear();
            MemoryConfiguration configuration = new MemoryConfiguration();
            configuration.set(CustomVisualSettings.ENABLED_CONFIGURATION_PATH, writtenValue);

            CustomVisualSettings settings = PluginSettings.read(configuration, problems::add).customVisualSettings();

            assertTrue(settings.enabled(), "a value of " + writtenValue);
            assertEquals(List.of(ENABLED_FALLBACK_PROBLEM), problems, "a value of " + writtenValue);
        }
    }

    @Test
    void fallsBackToAnEmptyIdentifierForAValueThatIsNoTextAndReportsItWithTheSuggestion() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH, 3);
        configuration.set(CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH, List.of("simplepvparena:fight"));
        configuration.set(CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH, true);

        CustomVisualSettings settings = PluginSettings.read(configuration, problems::add).customVisualSettings();

        assertEquals(CustomVisualSettings.defaults(), settings);
        assertEquals(List.of(
                "items-adder.countdown-images.three must be an ID in quotes such as simplepvparena:countdown_3 or "
                        + "empty, falling back to empty, the own title or effect of the plugin is used",
                "items-adder.countdown-images.fight must be an ID in quotes such as simplepvparena:countdown_fight "
                        + "or empty, falling back to empty, the own title or effect of the plugin is used",
                "items-adder.totem-item must be an ID in quotes such as simplepvparena:totem or empty, falling "
                        + "back to empty, the own title or effect of the plugin is used"), problems);
    }

    @Test
    @UseLanguage("de")
    void reportsTheBrokenEntriesInGerman() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(CustomVisualSettings.ENABLED_CONFIGURATION_PATH, "aus");
        configuration.set(CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH, 2);

        PluginSettings.read(configuration, problems::add);

        assertEquals(List.of("items-adder.enabled muss true oder false sein, stattdessen gilt true",
                "items-adder.countdown-images.two muss eine ID in Anführungszeichen wie simplepvparena:countdown_2 "
                        + "oder leer sein, stattdessen bleibt der Wert leer und es gilt der eigene Titel oder Effekt "
                        + "des Plugins"), problems);
    }

    @Test
    void readsTheSwitchAloneForTheChoiceAtTheStart() {
        MemoryConfiguration configuration = new MemoryConfiguration();
        assertTrue(PluginSettings.readItemsAdderEnabled(configuration, problems::add));
        assertTrue(PluginSettings.readItemsAdderEnabled(null, problems::add));

        configuration.set(CustomVisualSettings.ENABLED_CONFIGURATION_PATH, false);
        assertFalse(PluginSettings.readItemsAdderEnabled(configuration, problems::add));
        assertEquals(List.of(), problems);

        configuration.set(CustomVisualSettings.ENABLED_CONFIGURATION_PATH, "nein");
        assertTrue(PluginSettings.readItemsAdderEnabled(configuration, problems::add));
        assertEquals(List.of(ENABLED_FALLBACK_PROBLEM), problems);
    }

    @Test
    void theStartAndEveryReloadHandTheSettingsOn() {
        AtomicReference<MemoryConfiguration> fileOnDisk = new AtomicReference<>(configurationWith(true,
                "simplepvparena:countdown_3", "", "", "", ""));
        RecordingLiveConfiguration liveConfiguration = new RecordingLiveConfiguration();
        ConfigurationReloadService reloadService = new ConfigurationReloadService(fileOnDisk::get, () -> { },
                liveConfiguration, problems::add);

        reloadService.applyStoredConfiguration();
        assertEquals(new CustomVisualSettings(true, "simplepvparena:countdown_3", "", "", "", ""),
                liveConfiguration.customVisualSettings());

        fileOnDisk.set(configurationWith(false, "", "", "", "simplepvparena:countdown_fight",
                "simplepvparena:totem"));
        reloadService.reloadFromFile();
        assertEquals(new CustomVisualSettings(false, "", "", "", "simplepvparena:countdown_fight",
                "simplepvparena:totem"), liveConfiguration.customVisualSettings());

        MemoryConfiguration broken = new MemoryConfiguration();
        broken.set(CustomVisualSettings.ENABLED_CONFIGURATION_PATH, "broken");
        fileOnDisk.set(broken);
        reloadService.reloadFromFile();
        assertEquals(CustomVisualSettings.defaults(), liveConfiguration.customVisualSettings());
        assertEquals(List.of(ENABLED_FALLBACK_PROBLEM), problems);
    }

    @Test
    void theReloadAnswerNamesTheSectionInItsOwnCategoryWithTheRestartNote() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  the IDs of the ItemsAdder images and items and whether they are used, a switch "
                + "between ItemsAdder and the own visuals of the plugin needs a restart"), lines.toString());
    }

    @Test
    @UseLanguage("de")
    void theReloadAnswerNamesTheSectionInGerman() {
        List<String> lines = new ConfigurationReloadService(MemoryConfiguration::new, () -> { },
                new RecordingLiveConfiguration(), problems::add).reloadFromFile().describe();

        assertTrue(lines.contains("  die IDs der Bilder und Items aus ItemsAdder und ob sie genutzt werden, ein "
                + "Wechsel zwischen ItemsAdder und den eigenen Anzeigen des Plugins braucht einen Neustart"),
                lines.toString());
    }

    private static MemoryConfiguration configurationWith(boolean enabled, String three, String two, String one,
                                                         String fight, String totem) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        configuration.set(CustomVisualSettings.ENABLED_CONFIGURATION_PATH, enabled);
        configuration.set(CustomVisualSettings.THREE_IMAGE_CONFIGURATION_PATH, three);
        configuration.set(CustomVisualSettings.TWO_IMAGE_CONFIGURATION_PATH, two);
        configuration.set(CustomVisualSettings.ONE_IMAGE_CONFIGURATION_PATH, one);
        configuration.set(CustomVisualSettings.FIGHT_IMAGE_CONFIGURATION_PATH, fight);
        configuration.set(CustomVisualSettings.TOTEM_ITEM_CONFIGURATION_PATH, totem);
        return configuration;
    }

    private static List<String> shippedConfigurationLines() throws IOException {
        try (InputStream shippedFile = ItemsAdderSettingsTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            return new String(shippedFile.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }
}
