package de.simplepvparena.kit;

import de.simplepvparena.support.FakeSerializedKits;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileKitLibraryStorageTest {

    private static final String BROKEN_FILE_NAME = FileKitLibraryStorage.FILE_NAME + ".broken";

    private final List<String> problems = new ArrayList<>();

    @Test
    void readsBackWhatItSavedInTheSameOrder(@TempDir Path pluginFolder) throws IOException {
        SavedKitLibrary savedKitLibrary = threeKitLibrary();
        FileKitLibraryStorage storage = storageIn(pluginFolder);

        storage.save(savedKitLibrary);
        KitLibraryLookup lookup = storage.find();

        assertEquals(KitLibraryLookup.Outcome.FOUND, lookup.outcome());
        assertEquals(FileKitLibraryStorage.FILE_NAME, lookup.fileName());
        assertEquals(Optional.of(savedKitLibrary), lookup.savedKitLibrary());
        assertEquals(Optional.empty(), lookup.failureReason());
        assertEquals(List.of(), problems);
    }

    @Test
    void readsBackAnEmptyLibrary(@TempDir Path pluginFolder) throws IOException {
        FileKitLibraryStorage storage = storageIn(pluginFolder);

        storage.save(SavedKitLibrary.empty());

        assertEquals(Optional.of(SavedKitLibrary.empty()), storage.find().savedKitLibrary());
    }

    @Test
    void writesTheFormatVersionTheDefaultKitAndEveryKitInTheFormatOfTheKitSection(@TempDir Path pluginFolder)
            throws IOException {
        storageIn(pluginFolder).save(threeKitLibrary());

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME).toFile());

        assertEquals(1, configuration.getInt("format-version"));
        assertEquals("Knight", configuration.getString("default-kit"));
        assertEquals(List.of("Archer", "Knight", "123"),
                List.copyOf(configuration.getConfigurationSection("kits").getKeys(false)));
        assertEquals(SerializedKit.FORMAT_NAME, configuration.getString("kits.Knight.format"));
        assertEquals(FakeSerializedKits.encode("helmet"), configuration.getString("kits.Knight.equipment.head"));
        assertEquals(FakeSerializedKits.encode("sword"), configuration.getString("kits.Knight.slots.0"));
        assertTrue(SerializedKit.isWrittenIn(configuration.getConfigurationSection("kits.Archer")));
    }

    @Test
    void leavesNoTemporaryFileBehind(@TempDir Path pluginFolder) throws IOException {
        FileKitLibraryStorage storage = storageIn(pluginFolder);

        storage.save(threeKitLibrary());
        storage.save(SavedKitLibrary.empty());

        assertEquals(List.of(FileKitLibraryStorage.FILE_NAME), fileNamesIn(pluginFolder));
        assertEquals(Optional.of(SavedKitLibrary.empty()), storage.find().savedKitLibrary());
    }

    @Test
    void throwsAndLeavesNoTemporaryFileWhenTheFileCannotBeMovedIntoPlace(@TempDir Path pluginFolder)
            throws IOException {
        Path folderInPlaceOfTheFile = pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME);
        Files.createDirectories(folderInPlaceOfTheFile);
        Files.writeString(folderInPlaceOfTheFile.resolve("content.txt"), "blocks the move", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> storageIn(pluginFolder).save(threeKitLibrary()));

        assertEquals(List.of(FileKitLibraryStorage.FILE_NAME), fileNamesIn(pluginFolder));
        assertTrue(Files.isDirectory(folderInPlaceOfTheFile));
    }

    @Test
    void throwsWhenTheFolderCannotBeWritten(@TempDir Path pluginFolder) throws IOException {
        Path folderThatIsAFile = pluginFolder.resolve("plugin-folder");
        Files.writeString(folderThatIsAFile, "a file in place of the folder", StandardCharsets.UTF_8);
        FileKitLibraryStorage storage = new FileKitLibraryStorage(
                folderThatIsAFile.resolve(FileKitLibraryStorage.FILE_NAME), problems::add);

        assertThrows(IOException.class, () -> storage.save(threeKitLibrary()));
        assertEquals(List.of("plugin-folder"), fileNamesIn(pluginFolder));
    }

    @Test
    void reportsAMissingFile(@TempDir Path pluginFolder) {
        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.MISSING, lookup.outcome());
        assertEquals(FileKitLibraryStorage.FILE_NAME, lookup.fileName());
        assertTrue(lookup.savedKitLibrary().isEmpty());
        assertEquals(List.of(), fileNamesIn(pluginFolder));
    }

    @Test
    void reportsAFileSetAsideAtAnEarlierStartWhenTheFileIsMissing(@TempDir Path pluginFolder) throws IOException {
        Files.writeString(pluginFolder.resolve(BROKEN_FILE_NAME), "the broken file", StandardCharsets.UTF_8);

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.SET_ASIDE, lookup.outcome());
        assertEquals(BROKEN_FILE_NAME, lookup.fileName());
        assertTrue(lookup.savedKitLibrary().isEmpty());
        assertEquals(Optional.empty(), lookup.failureReason());
        assertEquals("the broken file", Files.readString(pluginFolder.resolve(BROKEN_FILE_NAME)));
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void readsTheFileAndLeavesAFileSetAsideAtAnEarlierStartAlone(@TempDir Path pluginFolder) throws IOException {
        FileKitLibraryStorage storage = storageIn(pluginFolder);
        storage.save(threeKitLibrary());
        Files.writeString(pluginFolder.resolve(BROKEN_FILE_NAME), "the broken file", StandardCharsets.UTF_8);

        KitLibraryLookup lookup = storage.find();

        assertEquals(KitLibraryLookup.Outcome.FOUND, lookup.outcome());
        assertEquals(Optional.of(threeKitLibrary()), lookup.savedKitLibrary());
        assertEquals(List.of(FileKitLibraryStorage.FILE_NAME, BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void readsAFileSetAsideOnceItIsRenamedBack(@TempDir Path pluginFolder) throws IOException {
        FileKitLibraryStorage storage = storageIn(pluginFolder);
        storage.save(threeKitLibrary());
        Path libraryFile = pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME);
        Files.writeString(libraryFile, "format-version: 1\nkits: [\n" + Files.readString(libraryFile),
                StandardCharsets.UTF_8);
        assertEquals(KitLibraryLookup.Outcome.BROKEN, storage.find().outcome());
        Path brokenFile = pluginFolder.resolve(BROKEN_FILE_NAME);
        Files.writeString(brokenFile, Files.readString(brokenFile).replace("format-version: 1\nkits: [\n", ""),
                StandardCharsets.UTF_8);
        assertEquals(KitLibraryLookup.Outcome.SET_ASIDE, storage.find().outcome());

        Files.move(brokenFile, libraryFile);
        KitLibraryLookup lookup = storage.find();

        assertEquals(KitLibraryLookup.Outcome.FOUND, lookup.outcome());
        assertEquals(Optional.of(threeKitLibrary()), lookup.savedKitLibrary());
    }

    @Test
    void movesAFileItCannotParseAside(@TempDir Path pluginFolder) throws IOException {
        writeLibraryFile(pluginFolder, "format-version: 1\nkits: [\n");

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(BROKEN_FILE_NAME, lookup.fileName());
        assertTrue(lookup.failureReason().isPresent());
        assertTrue(lookup.savedKitLibrary().isEmpty());
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void movesAFileWithAnUnknownFormatVersionAside(@TempDir Path pluginFolder) throws IOException {
        FileKitLibraryStorage storage = storageIn(pluginFolder);
        storage.save(threeKitLibrary());
        Path libraryFile = pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME);
        writeLibraryFile(pluginFolder, Files.readString(libraryFile).replace("format-version: 1",
                "format-version: 2"));

        KitLibraryLookup lookup = storage.find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("unknown format-version 2"), lookup.failureReason());
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void movesAFileWithoutAFormatVersionAside(@TempDir Path pluginFolder) throws IOException {
        writeLibraryFile(pluginFolder, "kits: {}\n");

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("missing or unreadable format-version"), lookup.failureReason());
    }

    @Test
    void movesAFileWhoseDefaultKitIsNotOneOfItsKitsAside(@TempDir Path pluginFolder) throws IOException {
        writeLibraryFile(pluginFolder, """
                format-version: 1
                default-kit: Ghost
                kits:
                  Archer:
                    format: base64-item
                    equipment: {}
                    slots: {}
                """);

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("The default kit Ghost is not one of the kits"), lookup.failureReason());
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void movesAFileWithTwoKitsOfTheSameNameInAnotherCaseAside(@TempDir Path pluginFolder) throws IOException {
        writeLibraryFile(pluginFolder, """
                format-version: 1
                default-kit: Archer
                kits:
                  Archer:
                    slots: {}
                  ARCHER:
                    slots: {}
                """);

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("The kit name ARCHER is used twice"), lookup.failureReason());
    }

    @Test
    void movesAFileWithAKitThatHoldsNoItemSectionsAside(@TempDir Path pluginFolder) throws IOException {
        writeLibraryFile(pluginFolder, """
                format-version: 1
                default-kit: Archer
                kits:
                  Archer:
                    armor:
                      head:
                        material: DIAMOND_HELMET
                """);

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(Optional.of("the kit Archer holds neither equipment nor slots"), lookup.failureReason());
    }

    @Test
    void overwritesABrokenFileThatIsAlreadyThere(@TempDir Path pluginFolder) throws IOException {
        Files.writeString(pluginFolder.resolve(BROKEN_FILE_NAME), "the older broken file", StandardCharsets.UTF_8);
        writeLibraryFile(pluginFolder, "the newer broken file");

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals("the newer broken file", Files.readString(pluginFolder.resolve(BROKEN_FILE_NAME)));
        assertEquals(List.of(BROKEN_FILE_NAME), fileNamesIn(pluginFolder));
    }

    @Test
    void reportsAnUnknownSlotOfAKitAndReadsTheOtherItems(@TempDir Path pluginFolder) throws IOException {
        writeLibraryFile(pluginFolder, """
                format-version: 1
                default-kit: Archer
                kits:
                  Archer:
                    equipment:
                      tail: %s
                    slots:
                      '0': %s
                """.formatted(FakeSerializedKits.encode("tail"), FakeSerializedKits.encode("bow")));

        KitLibraryLookup lookup = storageIn(pluginFolder).find();

        assertEquals(KitLibraryLookup.Outcome.FOUND, lookup.outcome(), lookup.failureReason().toString());
        assertEquals(new SavedKitLibrary(List.of(new SavedKit("Archer", FakeSerializedKits.carrying("bow"))),
                Optional.of("Archer")), lookup.savedKitLibrary().orElseThrow());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).startsWith("Kit Archer in kits.yml: Unknown kit slot tail"), problems.toString());
    }

    @Test
    void createsTheFolderWhenItIsNotThereYet(@TempDir Path pluginFolder) throws IOException {
        FileKitLibraryStorage storage = new FileKitLibraryStorage(
                pluginFolder.resolve("SimplePVPArena").resolve(FileKitLibraryStorage.FILE_NAME), problems::add);

        storage.save(threeKitLibrary());

        assertEquals(KitLibraryLookup.Outcome.FOUND, storage.find().outcome());
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
    }

    private static SavedKitLibrary threeKitLibrary() {
        return new SavedKitLibrary(List.of(
                new SavedKit("Archer", FakeSerializedKits.carrying("bow", "arrows")),
                new SavedKit("Knight", FakeSerializedKits.wearingAndCarrying(
                        Map.of(KitEquipmentSlot.HEAD, "helmet", KitEquipmentSlot.OFF_HAND, "shield"), "sword")),
                new SavedKit("123", SerializedKit.empty())), Optional.of("Knight"));
    }

    private FileKitLibraryStorage storageIn(Path pluginFolder) {
        return new FileKitLibraryStorage(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), problems::add);
    }

    private static void writeLibraryFile(Path pluginFolder, String contents) throws IOException {
        Files.writeString(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), contents, StandardCharsets.UTF_8);
    }

    private static List<String> fileNamesIn(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        } catch (IOException failure) {
            throw new AssertionError(failure);
        }
    }
}
