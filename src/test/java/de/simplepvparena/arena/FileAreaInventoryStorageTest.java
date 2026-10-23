package de.simplepvparena.arena;

import de.simplepvparena.kit.ItemStackCodec;
import de.simplepvparena.kit.KitItemCodec;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeItemStackCodec;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileAreaInventoryStorageTest {

    private static final int SLOT_COUNT = 41;
    private static final int OFF_HAND_SLOT = 40;
    private static final int HELMET_SLOT = 39;
    private static final AreaPlayerCondition WITHOUT_EFFECTS = new AreaPlayerCondition(List.of(), 20, 5.0F, 0.0F);

    @TempDir
    Path dataFolder;

    private final UUID playerUniqueId = UUID.randomUUID();

    @Test
    void writesTheWholeInventoryIntoItsOwnFolderAndReadsItBack() throws IOException {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());
        ItemStack[] contents = fullContents();

        storage.save(playerUniqueId, contents, List.of(), WITHOUT_EFFECTS);

        Path inventoryFile = inventoryDirectory().resolve(playerUniqueId + ".yml");
        assertTrue(Files.isRegularFile(inventoryFile));
        assertEquals(List.of(inventoryFile), filesIn(inventoryDirectory()), "no temporary file is left behind");
        String writtenFile = Files.readString(inventoryFile, StandardCharsets.UTF_8);
        assertTrue(writtenFile.contains("format-version: 3"), writtenFile);
        assertFalse(writtenFile.contains("loose-items"), writtenFile);
        assertTrue(writtenFile.contains("saved-at: "), writtenFile);
        AreaInventoryLookup lookup = storage.find(playerUniqueId);
        assertEquals(AreaInventoryLookup.Outcome.FOUND, lookup.outcome());
        assertEquals(playerUniqueId + ".yml", lookup.fileName());
        assertArrayEquals(contents, lookup.contents().orElseThrow(), "armour and off hand are part of the file");
        assertEquals(List.of(), lookup.looseItems());
    }

    @Test
    void writesTheItemsOfTheCursorAndTheCraftingGridAndReadsThemBack() throws IOException {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());
        List<ItemStack> looseItems = List.of(new FakeItemStack("cursor torch"), new FakeItemStack("grid planks"));

        storage.save(playerUniqueId, fullContents(), looseItems, WITHOUT_EFFECTS);

        String writtenFile = Files.readString(inventoryDirectory().resolve(playerUniqueId + ".yml"),
                StandardCharsets.UTF_8);
        assertTrue(writtenFile.contains("loose-items: "), writtenFile);
        AreaInventoryLookup lookup = storage.find(playerUniqueId);
        assertEquals(AreaInventoryLookup.Outcome.FOUND, lookup.outcome());
        assertArrayEquals(fullContents(), lookup.contents().orElseThrow());
        assertEquals(looseItems, lookup.looseItems());
    }

    @Test
    void readsAFileOfTheFirstFormatWithoutLooseItems() throws IOException {
        Files.createDirectories(inventoryDirectory());
        String encodedContents = KitItemCodec.encode(new FakeItemStackCodec().encode(fullContents()));
        Files.writeString(inventoryDirectory().resolve(playerUniqueId + ".yml"),
                "format-version: 1\nsaved-at: '2026-09-01T10:00:00Z'\ncontents: " + encodedContents + "\n",
                StandardCharsets.UTF_8);

        AreaInventoryLookup lookup = storageIn(inventoryDirectory()).find(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.FOUND, lookup.outcome());
        assertArrayEquals(fullContents(), lookup.contents().orElseThrow());
        assertEquals(List.of(), lookup.looseItems());
    }

    @Test
    void namesTheFolderAreaInventories() {
        assertEquals("area-inventories", FileAreaInventoryStorage.DIRECTORY_NAME);
    }

    @Test
    void replacesAnOlderFileOfTheSamePlayer() throws IOException {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());
        storage.save(playerUniqueId, fullContents(), List.of(), WITHOUT_EFFECTS);
        ItemStack[] newerContents = new ItemStack[SLOT_COUNT];
        newerContents[3] = new FakeItemStack("bread");

        storage.save(playerUniqueId, newerContents, List.of(), WITHOUT_EFFECTS);

        assertArrayEquals(newerContents, storage.find(playerUniqueId).contents().orElseThrow());
        assertEquals(1, filesIn(inventoryDirectory()).size());
    }

    @Test
    void reportsAMissingFileWithoutCreatingOne() {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());

        AreaInventoryLookup lookup = storage.find(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.MISSING, lookup.outcome());
        assertTrue(lookup.contents().isEmpty());
        assertFalse(Files.exists(inventoryDirectory()));
    }

    @Test
    void setsAnUnreadableFileAsideAsBroken() throws IOException {
        Files.createDirectories(inventoryDirectory());
        Path inventoryFile = inventoryDirectory().resolve(playerUniqueId + ".yml");
        Files.writeString(inventoryFile, "format-version: 1\ncontents: [not base64\n", StandardCharsets.UTF_8);

        AreaInventoryLookup lookup = storageIn(inventoryDirectory()).find(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals(playerUniqueId + ".yml.broken", lookup.fileName());
        assertFalse(Files.exists(inventoryFile));
        assertTrue(Files.isRegularFile(inventoryDirectory().resolve(playerUniqueId + ".yml.broken")));
    }

    @Test
    void treatsAFileOfAnUnknownFormatOrWithUndecodableItemsAsBroken() throws IOException {
        Files.createDirectories(inventoryDirectory());
        Path inventoryFile = inventoryDirectory().resolve(playerUniqueId + ".yml");
        Files.writeString(inventoryFile, "format-version: 7\ncontents: AAAA\n", StandardCharsets.UTF_8);
        assertEquals(AreaInventoryLookup.Outcome.BROKEN,
                storageIn(inventoryDirectory()).find(playerUniqueId).outcome());

        Files.writeString(inventoryFile, "format-version: 1\ncontents: AAAA\n", StandardCharsets.UTF_8);
        AreaInventoryLookup lookup = storageIn(inventoryDirectory()).find(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.BROKEN, lookup.outcome());
        assertTrue(lookup.fileName().startsWith(playerUniqueId + ".yml."), lookup.fileName());
        assertTrue(lookup.fileName().endsWith(".broken"), lookup.fileName());
    }

    @Test
    void keepsAnEarlierBrokenFileWhenASecondOneIsSetAside() throws IOException {
        Files.createDirectories(inventoryDirectory());
        Path earlierBrokenFile = inventoryDirectory().resolve(playerUniqueId + ".yml.broken");
        Files.writeString(earlierBrokenFile, "the first broken inventory", StandardCharsets.UTF_8);
        Files.writeString(inventoryDirectory().resolve(playerUniqueId + ".yml"), "{{{", StandardCharsets.UTF_8);

        AreaInventoryLookup lookup = storageIn(inventoryDirectory()).find(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.BROKEN, lookup.outcome());
        assertEquals("the first broken inventory", Files.readString(earlierBrokenFile, StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(inventoryDirectory().resolve(lookup.fileName())), lookup.fileName());
        assertEquals(2, filesIn(inventoryDirectory()).size());
    }

    @Test
    void throwsWhenTheFolderCannotBeWrittenAndLeavesNoFileBehind() throws IOException {
        Path blockedDirectory = dataFolder.resolve("blocked");
        Files.writeString(blockedDirectory, "a file where the folder should be", StandardCharsets.UTF_8);

        assertThrows(IOException.class,
                () -> storageIn(blockedDirectory).save(playerUniqueId, fullContents(), List.of(), WITHOUT_EFFECTS));

        assertTrue(Files.isRegularFile(blockedDirectory));
    }

    @Test
    void throwsWhenTheItemsCannotBeEncodedAndWritesNothing() {
        ItemStackCodec failingCodec = new ItemStackCodec() {
            @Override
            public byte[] encode(ItemStack[] contents) {
                throw new IllegalStateException("unencodable item");
            }

            @Override
            public ItemStack[] decode(byte[] encodedContents) {
                throw new UnsupportedOperationException();
            }
        };
        FileAreaInventoryStorage storage = new FileAreaInventoryStorage(inventoryDirectory(), failingCodec);

        IOException failure = assertThrows(IOException.class,
                () -> storage.save(playerUniqueId, fullContents(), List.of(), WITHOUT_EFFECTS));

        assertEquals("the inventory could not be encoded", failure.getMessage());
        assertFalse(Files.exists(inventoryDirectory().resolve(playerUniqueId + ".yml")));
    }

    @Test
    void deletesTheFileAndToleratesAMissingOne() throws IOException {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());
        storage.save(playerUniqueId, fullContents(), List.of(), WITHOUT_EFFECTS);

        storage.delete(playerUniqueId);
        storage.delete(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.MISSING, storage.find(playerUniqueId).outcome());
    }

    @Test
    void writesTheEffectsAndTheHungerAndReadsThemBack() throws IOException {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());
        AreaPlayerCondition condition = new AreaPlayerCondition(List.of(
                new SavedPotionEffect("minecraft:speed", 1, 1200, false, true, true),
                new SavedPotionEffect("minecraft:night_vision", 0, SavedPotionEffect.INFINITE_DURATION_TICKS, true,
                        false, false)), 9, 2.5F, 1.25F);

        storage.save(playerUniqueId, fullContents(), List.of(), condition);

        String writtenFile = Files.readString(inventoryDirectory().resolve(playerUniqueId + ".yml"),
                StandardCharsets.UTF_8);
        assertTrue(writtenFile.contains("potion-effects:"), writtenFile);
        assertTrue(writtenFile.contains("type: minecraft:speed"), writtenFile);
        assertTrue(writtenFile.contains("hunger:"), writtenFile);
        AreaInventoryLookup lookup = storage.find(playerUniqueId);
        assertEquals(AreaInventoryLookup.Outcome.FOUND, lookup.outcome());
        assertEquals(condition, lookup.condition().orElseThrow());
    }

    @Test
    void writesAnEmptyListOfEffects() throws IOException {
        FileAreaInventoryStorage storage = storageIn(inventoryDirectory());

        storage.save(playerUniqueId, fullContents(), List.of(), WITHOUT_EFFECTS);

        assertEquals(WITHOUT_EFFECTS, storage.find(playerUniqueId).condition().orElseThrow());
    }

    @Test
    void readsAFileOfTheSecondFormatWithoutEffectsAndHunger() throws IOException {
        Files.createDirectories(inventoryDirectory());
        String encodedContents = KitItemCodec.encode(new FakeItemStackCodec().encode(fullContents()));
        Files.writeString(inventoryDirectory().resolve(playerUniqueId + ".yml"),
                "format-version: 2\nsaved-at: '2026-10-01T10:00:00Z'\ncontents: " + encodedContents + "\n",
                StandardCharsets.UTF_8);

        AreaInventoryLookup lookup = storageIn(inventoryDirectory()).find(playerUniqueId);

        assertEquals(AreaInventoryLookup.Outcome.FOUND, lookup.outcome());
        assertArrayEquals(fullContents(), lookup.contents().orElseThrow());
        assertTrue(lookup.condition().isEmpty());
    }

    @Test
    void treatsAFileOfTheThirdFormatWithoutHungerOrWithAnUnreadableEffectAsBroken() throws IOException {
        Files.createDirectories(inventoryDirectory());
        String encodedContents = KitItemCodec.encode(new FakeItemStackCodec().encode(fullContents()));
        Path inventoryFile = inventoryDirectory().resolve(playerUniqueId + ".yml");
        Files.writeString(inventoryFile, "format-version: 3\ncontents: " + encodedContents
                + "\npotion-effects: []\n", StandardCharsets.UTF_8);
        assertEquals(AreaInventoryLookup.Outcome.BROKEN,
                storageIn(inventoryDirectory()).find(playerUniqueId).outcome());

        Files.writeString(inventoryFile, "format-version: 3\ncontents: " + encodedContents
                + "\npotion-effects:\n- type: minecraft:speed\n  amplifier: fast\n"
                + "hunger:\n  food-level: 20\n  saturation: 5.0\n  exhaustion: 0.0\n", StandardCharsets.UTF_8);

        assertEquals(AreaInventoryLookup.Outcome.BROKEN,
                storageIn(inventoryDirectory()).find(playerUniqueId).outcome());
    }

    private Path inventoryDirectory() {
        return dataFolder.resolve(FileAreaInventoryStorage.DIRECTORY_NAME);
    }

    private static FileAreaInventoryStorage storageIn(Path directory) {
        return new FileAreaInventoryStorage(directory, new FakeItemStackCodec());
    }

    private static ItemStack[] fullContents() {
        ItemStack[] contents = new ItemStack[SLOT_COUNT];
        contents[0] = new FakeItemStack("diamond sword");
        contents[35] = new FakeItemStack("golden apple");
        contents[HELMET_SLOT] = new FakeItemStack("netherite helmet");
        contents[OFF_HAND_SLOT] = new FakeItemStack("shield");
        return contents;
    }

    private static List<Path> filesIn(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.sorted().toList();
        }
    }
}
