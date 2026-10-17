package de.simplepvparena.kit;

import de.simplepvparena.support.FakeSerializedKits;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileKitLibraryStorageNameRoundTripTest {

    private static final List<String> DIFFICULT_NAMES = List.of("yes", "no", "on", "off", "null", "true", "false",
            "123", "007", "1e3", "a-b", "a_b", "Bogenschütze", "Ärger", "ß-Kit", "日本語");

    private final List<String> problems = new ArrayList<>();

    @Test
    void writesEveryDifficultNameAsTheSameTextIntoTheFile(@TempDir Path pluginFolder)
            throws IOException, InvalidConfigurationException {
        List<String> changedNames = new ArrayList<>();
        for (String kitName : DIFFICULT_NAMES) {
            Path kitFolder = Files.createDirectories(pluginFolder.resolve("kit-" + DIFFICULT_NAMES.indexOf(kitName)));
            storageIn(kitFolder).save(libraryWithTheSingleKit(kitName));

            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(Files.readString(kitFolder.resolve(FileKitLibraryStorage.FILE_NAME),
                    StandardCharsets.UTF_8));

            if (!List.of(kitName).equals(readKitNames(configuration))
                    || !kitName.equals(configuration.get("default-kit"))) {
                changedNames.add(kitName + " read back as " + readKitNames(configuration) + " with the default kit "
                        + configuration.get("default-kit"));
            }
        }

        assertEquals(List.of(), changedNames);
    }

    @Test
    void readsBackEveryDifficultNameAsTheNameOfTheKitAndOfTheDefaultKit(@TempDir Path pluginFolder)
            throws IOException {
        List<String> changedNames = new ArrayList<>();
        for (String kitName : DIFFICULT_NAMES) {
            Path kitFolder = Files.createDirectories(pluginFolder.resolve("kit-" + DIFFICULT_NAMES.indexOf(kitName)));
            FileKitLibraryStorage storage = storageIn(kitFolder);
            SavedKitLibrary savedKitLibrary = libraryWithTheSingleKit(kitName);
            storage.save(savedKitLibrary);

            KitLibraryLookup lookup = storage.find();

            if (!Optional.of(savedKitLibrary).equals(lookup.savedKitLibrary())) {
                changedNames.add(kitName + " read back as " + lookup.outcome() + " "
                        + lookup.savedKitLibrary().map(SavedKitLibrary::toString)
                                .orElse(lookup.failureReason().orElse("")));
            }
        }

        assertEquals(List.of(), changedNames);
        assertEquals(List.of(), problems);
    }

    @Test
    void readsBackALibraryHoldingEveryDifficultNameInTheSameOrder(@TempDir Path pluginFolder) throws IOException {
        List<SavedKit> savedKits = new ArrayList<>();
        for (String kitName : DIFFICULT_NAMES) {
            savedKits.add(new SavedKit(kitName, FakeSerializedKits.carrying(kitName)));
        }
        SavedKitLibrary savedKitLibrary = new SavedKitLibrary(savedKits, Optional.of("no"));
        FileKitLibraryStorage storage = storageIn(pluginFolder);

        storage.save(savedKitLibrary);
        KitLibraryLookup lookup = storage.find();

        assertEquals(KitLibraryLookup.Outcome.FOUND, lookup.outcome(), lookup.failureReason().toString());
        assertEquals(Optional.of(savedKitLibrary), lookup.savedKitLibrary());
        assertEquals(List.of(), problems);
    }

    private static SavedKitLibrary libraryWithTheSingleKit(String kitName) {
        return new SavedKitLibrary(List.of(new SavedKit(kitName, FakeSerializedKits.carrying("bow"))),
                Optional.of(kitName));
    }

    private static List<String> readKitNames(YamlConfiguration configuration) {
        ConfigurationSection kitsSection = configuration.getConfigurationSection("kits");
        return kitsSection == null ? List.of() : List.copyOf(kitsSection.getKeys(false));
    }

    private FileKitLibraryStorage storageIn(Path pluginFolder) {
        return new FileKitLibraryStorage(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), problems::add);
    }
}
