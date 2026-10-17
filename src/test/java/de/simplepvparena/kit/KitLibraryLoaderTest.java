package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
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
    private static final String NO_READABLE_ITEM_NOTICE = "The kit section of config.yml holds no item that could "
            + "be read, so no kit was taken over into kits.yml. There is no kit and every fighter starts a round "
            + "with an empty kit until a kit is created in the kit menu of /tournament";
    private static final String SET_ASIDE_NOTICE = "There is no kits.yml, the kits lie in kits.yml.broken because "
            + "it could not be read at an earlier start. Repair kits.yml.broken and rename it to kits.yml, or delete "
            + "it if its kits are to be discarded, then the next start takes the kit section of config.yml over";
    private static final String SHIPPED_TYPED_KIT = """
            match:
              round-timeout-seconds: 300
            kit:
              armor:
                head:
                  material: DIAMOND_HELMET
                chest:
                  material: DIAMOND_CHESTPLATE
                legs:
                  material: DIAMOND_LEGGINGS
                feet:
                  material: DIAMOND_BOOTS
              items:
                - material: DIAMOND_SWORD
                - material: BOW
                - material: ARROW
                  amount: 64
                - material: GOLDEN_APPLE
                  amount: 6
            """;

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
    void takesTheShippedKitInTheTypedFormatOverAsTheDefaultKitWithoutChangingConfigYml(@TempDir Path pluginFolder)
            throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf(SHIPPED_TYPED_KIT);
        String configurationBefore = configuration.saveToString();
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(KitLibraryLoader.MIGRATED_KIT_NAME), kitLibrary.getKitNames());
        assertEquals(Optional.of("default"), kitLibrary.getDefaultKitName());
        KitDefinition handedOutKit = kitService.getKitDefinition();
        assertEquals(Map.of(KitEquipmentSlot.HEAD, "DIAMOND_HELMET x1",
                KitEquipmentSlot.CHEST, "DIAMOND_CHESTPLATE x1",
                KitEquipmentSlot.LEGS, "DIAMOND_LEGGINGS x1",
                KitEquipmentSlot.FEET, "DIAMOND_BOOTS x1"), equipmentLabelsOf(handedOutKit));
        assertEquals(List.of("DIAMOND_SWORD x1", "BOW x1", "ARROW x64", "GOLDEN_APPLE x6"),
                List.of(inventoryLabelOf(handedOutKit, 0), inventoryLabelOf(handedOutKit, 1),
                        inventoryLabelOf(handedOutKit, 2), inventoryLabelOf(handedOutKit, 3)));
        assertEquals(4, handedOutKit.inventoryItems().size());
        assertEquals(List.of(MIGRATION_NOTICE), adminNotices);
        assertEquals(List.of(), problems);
        assertEquals(configurationBefore, configuration.saveToString());
        assertEquals(Optional.of(new SavedKitLibrary(List.of(new SavedKit("default",
                        FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "DIAMOND_HELMET x1",
                                KitEquipmentSlot.CHEST, "DIAMOND_CHESTPLATE x1",
                                KitEquipmentSlot.LEGS, "DIAMOND_LEGGINGS x1",
                                KitEquipmentSlot.FEET, "DIAMOND_BOOTS x1"),
                                "DIAMOND_SWORD x1", "BOW x1", "ARROW x64", "GOLDEN_APPLE x6"))),
                Optional.of("default"))), storageIn(pluginFolder).find().savedKitLibrary());
    }

    @Test
    void reportsAnUnknownMaterialOfTheTypedKitAndTakesTheReadableItemsOver(@TempDir Path pluginFolder)
            throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf("""
                kit:
                  armor:
                    head:
                      material: DIAMANT_HELMET
                  items:
                    - material: DIAMOND_SWORD
                    - material: NOT_A_MATERIAL
                      amount: 3
                """);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of("default"), kitLibrary.getKitNames());
        assertEquals(Map.of(), equipmentLabelsOf(kitService.getKitDefinition()));
        assertEquals("DIAMOND_SWORD x1", inventoryLabelOf(kitService.getKitDefinition(), 0));
        assertEquals(1, kitService.getKitDefinition().inventoryItems().size());
        assertEquals(List.of("Unknown material in kit configuration: DIAMANT_HELMET",
                "Unknown material in kit configuration: NOT_A_MATERIAL"), problems);
        assertEquals(List.of(MIGRATION_NOTICE), adminNotices);
    }

    @Test
    void takesNoKitOverAndWarnsWhenNoItemOfTheTypedKitCanBeRead(@TempDir Path pluginFolder)
            throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf("""
                kit:
                  armor:
                    head:
                      material: DIAMANT_HELMET
                  items:
                    - material: NOT_A_SWORD
                    - amount: 6
                """);
        String configurationBefore = configuration.saveToString();
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(List.of(NO_READABLE_ITEM_NOTICE), adminNotices);
        assertEquals(List.of("Unknown material in kit configuration: DIAMANT_HELMET",
                "Unknown material in kit configuration: NOT_A_SWORD",
                "Kit item without material found, skipping it"), problems);
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
        assertEquals(configurationBefore, configuration.saveToString());
    }

    @Test
    void reportsUnusableItemsOfTheTypedKitAndTakesTheUsableItemsOver(@TempDir Path pluginFolder)
            throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf("""
                kit:
                  armor:
                    head:
                      material: DIAMOND_HELMET
                  items:
                    - material: WATER
                    - material: DIAMOND_SWORD
                    - material: ARROW
                      amount: 0
                    - material: BOW
                """);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(KitLibraryLoader.MIGRATED_KIT_NAME), kitLibrary.getKitNames());
        assertEquals(Optional.of("default"), kitLibrary.getDefaultKitName());
        assertEquals(Map.of(KitEquipmentSlot.HEAD, "DIAMOND_HELMET x1"),
                equipmentLabelsOf(kitService.getKitDefinition()));
        assertEquals(List.of("DIAMOND_SWORD x1", "BOW x1"),
                List.of(inventoryLabelOf(kitService.getKitDefinition(), 0),
                        inventoryLabelOf(kitService.getKitDefinition(), 1)));
        assertEquals(2, kitService.getKitDefinition().inventoryItems().size());
        assertEquals(List.of("Unusable item WATER in kit configuration: WATER isn't an item",
                "Unusable item ARROW in kit configuration: the amount 0 is not between 1 and 99"), problems);
        assertEquals(List.of(MIGRATION_NOTICE), adminNotices);
    }

    @Test
    void takesNoKitOverAndWarnsWhenEveryItemOfTheTypedKitIsUnusable(@TempDir Path pluginFolder)
            throws InvalidConfigurationException {
        YamlConfiguration configuration = configurationOf("""
                kit:
                  armor:
                    head:
                      material: WATER
                  items:
                    - material: DIAMOND_SWORD
                      amount: 0
                """);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configuration.getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(List.of(NO_READABLE_ITEM_NOTICE), adminNotices);
        assertEquals(List.of("Unusable item WATER in kit configuration: WATER isn't an item",
                "Unusable item DIAMOND_SWORD in kit configuration: the amount 0 is not between 1 and 99"), problems);
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
    }

    @Test
    void takesNoTypedKitOverWhileKitsYmlExists(@TempDir Path pluginFolder)
            throws IOException, InvalidConfigurationException {
        storageIn(pluginFolder).save(new SavedKitLibrary(List.of(
                new SavedKit("Archer", FakeSerializedKits.carrying("bow"))), Optional.of("Archer")));
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationOf(SHIPPED_TYPED_KIT).getConfigurationSection("kit"));

        assertEquals(List.of("Archer"), kitLibrary.getKitNames());
        assertEquals("bow", inventoryLabelOf(kitService.getKitDefinition(), 0));
        assertEquals(List.of(), adminNotices);
    }

    @Test
    void takesNoKitOverAndWarnsWhileKitsYmlBrokenIsThere(@TempDir Path pluginFolder)
            throws IOException, InvalidConfigurationException {
        Path brokenFile = pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME + ".broken");
        Files.writeString(brokenFile, "format-version: 1\nkits: [\n", StandardCharsets.UTF_8);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationOf(SHIPPED_TYPED_KIT).getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(List.of(SET_ASIDE_NOTICE), adminNotices);
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
        assertEquals("format-version: 1\nkits: [\n", Files.readString(brokenFile, StandardCharsets.UTF_8));
    }

    @Test
    void takesNoSavedKitOverAndWarnsWhileKitsYmlBrokenIsThere(@TempDir Path pluginFolder) throws IOException {
        Files.writeString(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME + ".broken"), "broken",
                StandardCharsets.UTF_8);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationWithASavedKit().getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(List.of(SET_ASIDE_NOTICE), adminNotices);
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
    }

    @Test
    void warnsAboutKitsYmlBrokenAlsoWithoutAKitSection(@TempDir Path pluginFolder) throws IOException {
        Files.writeString(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME + ".broken"), "broken",
                StandardCharsets.UTF_8);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(null);

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(List.of(SET_ASIDE_NOTICE), adminNotices);
    }

    @Test
    void setsABrokenKitsYmlAsideWithoutTakingTheTypedKitOverInTheSameStart(@TempDir Path pluginFolder)
            throws IOException, InvalidConfigurationException {
        Files.writeString(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), "format-version: 1\nkits: [\n",
                StandardCharsets.UTF_8);
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationOf(SHIPPED_TYPED_KIT).getConfigurationSection("kit"));

        assertEquals(List.of(), kitLibrary.getKitNames());
        assertTrue(kitService.getKitDefinition().isEmpty());
        assertEquals(1, adminNotices.size());
        assertTrue(adminNotices.get(0).startsWith("The kits in kits.yml could not be read: "), adminNotices.get(0));
        assertTrue(Files.isRegularFile(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME + ".broken")));
        assertFalse(Files.exists(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME)));
    }

    @Test
    void restoresTheKitsOnceKitsYmlBrokenIsRenamedBack(@TempDir Path pluginFolder)
            throws IOException, InvalidConfigurationException {
        storageIn(pluginFolder).save(new SavedKitLibrary(List.of(
                new SavedKit("Bogenschütze", FakeSerializedKits.carrying("bow")),
                new SavedKit("Knight", FakeSerializedKits.carrying("sword"))), Optional.of("Bogenschütze")));
        Path libraryFile = pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME);
        Path brokenFile = pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME + ".broken");
        Files.move(libraryFile, brokenFile);
        loaderIn(pluginFolder, libraryIn(pluginFolder))
                .load(configurationOf(SHIPPED_TYPED_KIT).getConfigurationSection("kit"));
        Files.move(brokenFile, libraryFile);
        adminNotices.clear();
        KitLibrary kitLibrary = libraryIn(pluginFolder);

        loaderIn(pluginFolder, kitLibrary).load(configurationOf(SHIPPED_TYPED_KIT).getConfigurationSection("kit"));

        assertEquals(List.of("Bogenschütze", "Knight"), kitLibrary.getKitNames());
        assertEquals(Optional.of("Bogenschütze"), kitLibrary.getDefaultKitName());
        assertEquals("bow", inventoryLabelOf(kitService.getKitDefinition(), 0));
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

        new KitLibraryLoader(storage, kitLibrary, kitConfigurationLoader(), adminNotices::add, problems::add)
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

    private static YamlConfiguration configurationOf(String contents) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(contents);
        return configuration;
    }

    private static String inventoryLabelOf(KitDefinition kitDefinition, int slotIndex) {
        return ((EncodableItemStack) kitDefinition.inventoryItems().get(slotIndex)).getLabel();
    }

    private static Map<KitEquipmentSlot, String> equipmentLabelsOf(KitDefinition kitDefinition) {
        Map<KitEquipmentSlot, String> equipmentLabels = new EnumMap<>(KitEquipmentSlot.class);
        kitDefinition.equipmentItems().forEach((equipmentSlot, itemStack) ->
                equipmentLabels.put(equipmentSlot, ((EncodableItemStack) itemStack).getLabel()));
        return equipmentLabels;
    }

    private KitConfigurationLoader kitConfigurationLoader() {
        return new KitConfigurationLoader(problems::add, new FakeKitItemSerializer(),
                KitLibraryLoaderTest::createItemStackLikeTheServer);
    }

    private static ItemStack createItemStackLikeTheServer(KitItem kitItem) {
        if (kitItem.material() == Material.WATER) {
            throw new IllegalArgumentException("WATER isn't an item");
        }
        return new EncodableItemStack(kitItem.material().name() + " x" + kitItem.amount());
    }

    private FileKitLibraryStorage storageIn(Path pluginFolder) {
        return new FileKitLibraryStorage(pluginFolder.resolve(FileKitLibraryStorage.FILE_NAME), problems::add);
    }

    private KitLibrary libraryIn(Path pluginFolder) {
        return new KitLibrary(storageIn(pluginFolder), kitService, new FakeKitItemSerializer(), problems::add,
                adminNotices::add);
    }

    private KitLibraryLoader loaderIn(Path pluginFolder, KitLibrary kitLibrary) {
        return new KitLibraryLoader(storageIn(pluginFolder), kitLibrary, kitConfigurationLoader(), adminNotices::add,
                problems::add);
    }
}
