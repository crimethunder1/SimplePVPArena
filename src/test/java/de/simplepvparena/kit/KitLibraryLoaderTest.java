package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.SharedBukkitServer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitLibraryLoaderTest {

    private static final String MIGRATION_NOTICE = "The kit of config.yml was taken over into kits.yml as the kit "
            + "default and is the default kit. The kit section of config.yml is not read any more, the kits are "
            + "managed in the kit menu of /tournament";

    private final KitService kitService = new KitService();
    private final List<String> adminNotices = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();

    KitLibraryLoaderTest() {
        SharedBukkitServer.install();
    }

    @Test
    void restoresTheKitsOfKitsYmlAndHandsOutTheDefaultKit(@TempDir Path pluginFolder) throws IOException {
        storageIn(pluginFolder).save(new SavedKitLibrary(List.of(
                new SavedKit("Archer", FakeSerializedKits.carrying("bow")),
                new SavedKit("Knight", FakeSerializedKits.carrying("sword"))), Optional.of("Knight")));
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationWithASavedKit().getConfigurationSection("kit"));

        assertEquals(List.of("Archer", "Knight"), kitLibrary.getKitNames());
        assertEquals(Optional.of("Knight"), kitLibrary.getDefaultKitName());
        assertEquals("sword", inventoryLabelOf(kitService.getKitDefinition(), 0));
        assertEquals(List.of(), adminNotices, "a kit section of config.yml is not read while kits.yml exists");
    }

    @Test
    void takesTheSavedKitOfConfigYmlOverAsTheDefaultKitWithoutChangingConfigYml(@TempDir Path pluginFolder)
            throws IOException {
        YamlConfiguration configuration = configurationWithASavedKit();
        String configurationBefore = configuration.saveToString();
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(KitLibraryLoader.MIGRATED_KIT_NAME), kitLibrary.getKitNames());
        assertEquals(Optional.of("default"), kitLibrary.getDefaultKitName());
        assertEquals("helmet", ((EncodableItemStack) kitService.getKitDefinition().equipmentItems()
                .get(KitEquipmentSlot.HEAD)).getLabel());
        assertEquals("sword", inventoryLabelOf(kitService.getKitDefinition(), 0));
        assertEquals(List.of(MIGRATION_NOTICE), adminNotices);
        assertEquals(configurationBefore, configuration.saveToString());
        assertEquals(Optional.of(new SavedKitLibrary(List.of(new SavedKit("default",
                        FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "helmet"), "sword"))),
                Optional.of("default"))), storageIn(pluginFolder).find().savedKitLibrary());
    }

    @Test
    void startsWithoutAKitWhenNeitherKitsYmlNorAKitSectionExists(@TempDir Path pluginFolder) {
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(null);

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(List.of(), adminNotices);
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
    }

    @Test
    void takesNoEmptyKitSectionOver(@TempDir Path pluginFolder) {
        YamlConfiguration configuration = new YamlConfiguration();
        SerializedKit.empty().writeInto(configuration, "kit");
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(List.of(), adminNotices);
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
    }

    @Test
    void takesAKitSectionInTheTypedFormatNotOver(@TempDir Path pluginFolder) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("kit.armor.head.material", "DIAMOND_HELMET");
        configuration.set("kit.items", List.of(Map.of("material", "DIAMOND_SWORD")));
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(List.of(), adminNotices);
    }

    @Test
    void warnsTheAdminsAboutAnUnreadableKitsYmlAndStartsWithoutAKit(@TempDir Path pluginFolder)
            throws IOException {
        Files.writeString(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), "format-version: 1\nkits: [\n",
                StandardCharsets.UTF_8);
        kitService.setKitDefinition(new KitDefinition(Map.of(), Map.of(0, new EncodableItemStack("old"))));
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationWithASavedKit().getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(1, adminNotices.size());
        String notice = adminNotices.get(0);
        assertTrue(notice.startsWith("The kits in kits.yml could not be read: "), notice);
        assertTrue(notice.endsWith(". The file was kept as kits.yml.broken, there is no kit and every fighter "
                + "starts a round with an empty kit until a kit is created in the kit menu of /tournament"), notice);
        assertTrue(Files.isRegularFile(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME + ".broken")));
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)),
                "the kit section of config.yml is not taken over while the broken file is set aside");
        assertTrue(kitService.getKitDefinition().isEmpty());
    }

    @Test
    void keepsTheTakenOverKitInMemoryWhenKitsYmlCannotBeWritten(@TempDir Path pluginFolder) throws IOException {
        Path folderThatIsAFile = pluginFolder.resolve("plugin-folder");
        Files.writeString(folderThatIsAFile, "a file in place of the folder", StandardCharsets.UTF_8);
        FileKitLibraryStorage storage = new FileKitLibraryStorage(
                folderThatIsAFile.resolve(FileKitLibraryStorage.FILE_NAME), problems::add);
        KitLibrary kitLibrary = new KitLibrary(storage, kitService, new FakeKitItemSerializer(), problems::add,
                adminNotices::add);

        new KitLibraryLoader(storage, kitLibrary, adminNotices::add, problems::add)
                .load(configurationWithASavedKit().getConfigurationSection("kit"));

        assertEquals(List.of("default"), kitLibrary.getKitNames());
        assertEquals("sword", inventoryLabelOf(kitService.getKitDefinition(), 0));
        assertEquals(2, adminNotices.size());
        assertTrue(adminNotices.get(0).startsWith("The kits could not be written into kits.yml"),
                adminNotices.toString());
        assertEquals(MIGRATION_NOTICE, adminNotices.get(1));
    }

    private static YamlConfiguration configurationWithASavedKit() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("match.round-timeout-seconds", 300);
        FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "helmet"), "sword")
                .writeInto(configuration, "kit");
        return configuration;
    }

    private static String inventoryLabelOf(KitDefinition kitDefinition, int slotIndex) {
        return ((EncodableItemStack) kitDefinition.inventoryItems().get(slotIndex)).getLabel();
    }

    private FileKitLibraryStorage storageIn(Path pluginFolder) {
        return new FileKitLibraryStorage(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), problems::add);
    }

    private KitLibrary libraryIn(Path pluginFolder) {
        return new KitLibrary(storageIn(pluginFolder), kitService, new FakeKitItemSerializer(), problems::add,
                adminNotices::add);
    }

    private KitLibraryLoader loaderIn(Path pluginFolder, KitLibrary kitLibrary) {
        return new KitLibraryLoader(storageIn(pluginFolder), kitLibrary, adminNotices::add, problems::add);
    }
}
