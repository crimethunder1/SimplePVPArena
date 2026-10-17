package de.simplepvparena.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ShippedConfigurationTest {

    @Test
    void holdsEveryPathTheStartAndTheReloadRead() {
        YamlConfiguration configuration = shippedConfiguration();

        List<String> missingPaths = new ArrayList<>();
        for (String path : PluginSettings.CONFIGURATION_PATHS) {
            if (!configuration.contains(path)) {
                missingPaths.add(path);
            }
        }

        assertEquals(List.of(), missingPaths, "config.yml has to document every setting the plugin reads");
    }

    @Test
    void isReadWithoutASingleProblem() {
        List<String> problems = new ArrayList<>();

        PluginSettings.read(shippedConfiguration(), problems::add);

        assertEquals(List.of(), problems, "the shipped file must never fall back to a default");
    }

    @Test
    void repeatsTheValuesTheCodeFallsBackTo() {
        PluginSettings shipped = PluginSettings.read(shippedConfiguration(), problem -> { });

        assertEquals(PluginSettings.defaults(), shipped);
    }

    @Test
    void shipsNoKitBecauseTheKitsLiveInKitsYml() {
        YamlConfiguration configuration = shippedConfiguration();

        assertFalse(configuration.contains(PluginSettings.KIT_SECTION_PATH));
    }

    private static YamlConfiguration shippedConfiguration() {
        try (InputStream shippedFile = ShippedConfigurationTest.class.getResourceAsStream("/config.yml")) {
            assertNotNull(shippedFile, "config.yml is missing from the plugin resources");
            try (Reader reader = new InputStreamReader(shippedFile, StandardCharsets.UTF_8)) {
                YamlConfiguration configuration = new YamlConfiguration();
                configuration.load(reader);
                return configuration;
            }
        } catch (Exception failure) {
            throw new AssertionError("the shipped config.yml could not be read", failure);
        }
    }
}
