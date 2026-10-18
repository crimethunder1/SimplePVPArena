package de.simplepvparena.message;

import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YamlMessageCatalogTest {

    @TempDir
    Path folder;

    @Test
    void nestedKeysBecomePathsWithDots() throws Exception {
        YamlMessageCatalog catalog = read("""
                menu:
                  back:
                    name: 'Back'
                    main-menu: 'Returns to the main menu'
                """);

        assertEquals(Set.of("menu.back.name", "menu.back.main-menu"), catalog.paths());
        assertEquals(Optional.of(List.of("Returns to the main menu")), catalog.find("menu.back.main-menu"));
    }

    @Test
    void aSectionIsNoText() throws Exception {
        YamlMessageCatalog catalog = read("""
                menu:
                  back:
                    name: 'Back'
                """);

        assertEquals(Optional.empty(), catalog.find("menu.back"));
        assertEquals(Optional.empty(), catalog.find("menu"));
    }

    @Test
    void aListBecomesTheLinesOfTheText() throws Exception {
        YamlMessageCatalog catalog = read("""
                player:
                  unknown:
                    - 'Unknown player {player}'
                    - 'The player has to be online or known to the server'
                """);

        assertEquals(Optional.of(List.of("Unknown player {player}",
                "The player has to be online or known to the server")), catalog.find("player.unknown"));
    }

    @Test
    void aQuotedValueKeepsLeadingSpacesColonsAndHashes() throws Exception {
        YamlMessageCatalog catalog = read("""
                report:
                  line: '  Spawn: 10, 64, 10 # set'
                """);

        assertEquals(Optional.of(List.of("  Spawn: 10, 64, 10 # set")), catalog.find("report.line"));
    }

    @Test
    void aValueWithoutQuotesIsReadAsText() throws Exception {
        YamlMessageCatalog catalog = read("""
                menu:
                  limit: 64
                  name: Back
                """);

        assertEquals(Optional.of(List.of("64")), catalog.find("menu.limit"));
        assertEquals(Optional.of(List.of("Back")), catalog.find("menu.name"));
    }

    @Test
    void aFileIsReadAsUtf8() throws Exception {
        Path file = folder.resolve("de.yml");
        Files.writeString(file, "menu:\n  back: 'Zurück zum Hauptmenü'\n", StandardCharsets.UTF_8);

        assertEquals(Optional.of(List.of("Zurück zum Hauptmenü")),
                YamlMessageCatalog.readFile(file).find("menu.back"));
    }

    @Test
    void brokenYamlIsRefused() {
        assertThrows(InvalidConfigurationException.class, () -> read("""
                menu:
                  back: 'Returns to
                    name: [
                """));
    }

    @Test
    void aMissingResourceIsAnError() {
        assertThrows(IllegalStateException.class, () -> YamlMessageCatalog.readResource("lang/xx.yml"));
    }

    private static YamlMessageCatalog read(String content) throws Exception {
        return YamlMessageCatalog.read(new StringReader(content));
    }
}
