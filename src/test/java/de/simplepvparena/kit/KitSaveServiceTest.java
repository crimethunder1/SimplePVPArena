package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.FakeSerializedKits;
import de.simplepvparena.support.InMemoryKitLibraryStorage;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitSaveServiceTest {

    private static final List<String> FILLED_KIT_DESCRIPTION =
            List.of("Worn items: 1 of 5", "Inventory items: 1 of 36 slots");
    private static final String SAVED_KIT_NOTICE = "It is written into kits.yml and is handed out from the next "
            + "round on, a running round keeps the kit its fighters were given";
    private static final String SAVED_FURTHER_KIT_NOTICE = "It is written into kits.yml and is handed out once it "
            + "is used as the default kit";
    private static final String UNDECODABLE_LABEL = "broken";

    private final KitService kitService = new KitService();
    private final InMemoryKitLibraryStorage storage = new InMemoryKitLibraryStorage();
    private final List<String> problems = new ArrayList<>();
    private final Map<Object, ItemStack> inventoryItems = new HashMap<>();
    private final FakeKitItemSerializer kitItemSerializer = new FakeKitItemSerializer(UNDECODABLE_LABEL);
    private final KitLibrary kitLibrary = new KitLibrary(storage, kitService, kitItemSerializer, problems::add,
            problem -> { });
    private final KitSaveService kitSaveService = new KitSaveService(kitLibrary, kitItemSerializer);

    KitSaveServiceTest() {
        SharedBukkitServer.install();
    }

    @Test
    void keepsTheKitAndWritesNothingWhenTheInventoryIsEmpty() {
        createFilledKit("Archer");

        KitSaveResult result = kitSaveService.replaceFromInventory(createPlayer(), "Archer");

        assertFalse(result.isSaved());
        assertEquals(List.of("Your inventory is empty, the kit was not changed"), result.describe());
        assertEquals(1, storage.savedLibraries().size());
        assertEquals(FILLED_KIT_DESCRIPTION, kitService.getKitDefinition().describe());
    }

    @Test
    void createsNoKitAndWritesNothingWhenTheInventoryIsEmpty() {
        KitSaveResult result = kitSaveService.createFromInventory(createPlayer(), "Archer");

        assertFalse(result.isSaved());
        assertEquals(List.of("Your inventory is empty, no kit was created"), result.describe());
        assertEquals(List.of(), kitLibrary.getKitNames());
        assertEquals(List.of(), storage.savedLibraries());
        assertTrue(kitSaveService.holdsNoItem(createPlayer()));
    }

    @Test
    void savesTheInventoryWhenOnlyTheOffhandHoldsAnItem() {
        inventoryItems.put(EquipmentSlot.OFF_HAND, new EncodableItemStack("shield"));

        KitSaveResult result = kitSaveService.createFromInventory(createPlayer(), "Guard");

        assertTrue(result.isSaved());
        assertFalse(kitSaveService.holdsNoItem(createPlayer()));
        SerializedKit savedKit = storage.lastSavedLibrary().kits().get(0).serializedKit();
        assertEquals(Set.of(KitEquipmentSlot.OFF_HAND), savedKit.equipmentItems().keySet());
        assertTrue(savedKit.inventoryItems().isEmpty());
    }

    @Test
    void savesAFilledInventoryAsANewKit() {
        inventoryItems.put(EquipmentSlot.HEAD, new EncodableItemStack("helmet"));
        inventoryItems.put(35, new EncodableItemStack("sword"));

        KitSaveResult result = kitSaveService.createFromInventory(createPlayer(), "Knight");

        assertTrue(result.isSaved());
        assertEquals(List.of(new SavedKit("Knight", new SerializedKit(
                        Map.of(KitEquipmentSlot.HEAD, FakeSerializedKits.encode("helmet")),
                        Map.of(35, FakeSerializedKits.encode("sword"))))),
                storage.lastSavedLibrary().kits());
        assertEquals(List.of("Kit Knight saved: 2 items, 1 worn and 1 in the inventory", SAVED_KIT_NOTICE),
                result.describe());
        assertTrue(problems.isEmpty(), problems.toString());
        assertEquals(FILLED_KIT_DESCRIPTION, kitService.getKitDefinition().describe());
        KitDefinition appliedKit = kitService.getKitDefinition();
        assertEquals("helmet", labelOf(appliedKit.equipmentItems().get(KitEquipmentSlot.HEAD)));
        assertEquals(Set.of(35), appliedKit.inventoryItems().keySet());
        assertEquals("sword", labelOf(appliedKit.inventoryItems().get(35)));
    }

    @Test
    void replacesAKitThatIsNotTheDefaultKitWithoutHandingItOut() {
        createFilledKit("Archer");
        createFilledKit("Knight");
        inventoryItems.put(0, new EncodableItemStack("axe"));

        KitSaveResult result = kitSaveService.replaceFromInventory(createPlayer(), "knight");

        assertEquals(List.of("Kit Knight saved: 1 item, 0 worn and 1 in the inventory", SAVED_FURTHER_KIT_NOTICE),
                result.describe());
        assertEquals(1, kitLibrary.findKit("Knight").orElseThrow().itemCount());
        assertEquals(FILLED_KIT_DESCRIPTION, kitService.getKitDefinition().describe());
    }

    @Test
    void refusesANameThatIsNotAllowedOrExistsAlreadyBeforeTheInventoryIsLookedAt() {
        createFilledKit("Archer");

        assertEquals(List.of("A kit named Archer exists already"),
                kitSaveService.createFromInventory(createPlayer(), "archer").describe());
        assertEquals(List.of("The kit name my kit is not allowed, a kit name has 1 to 32 letters, digits, "
                + "hyphens or underscores"), kitSaveService.createFromInventory(createPlayer(), "my kit").describe());
        assertEquals(List.of("Archer"), kitLibrary.getKitNames());
    }

    @Test
    void refusesToReplaceAnUnknownKit() {
        inventoryItems.put(0, new EncodableItemStack("axe"));

        KitSaveResult result = kitSaveService.replaceFromInventory(createPlayer(), "Ghost");

        assertEquals(List.of("Unknown kit Ghost"), result.describe());
        assertEquals(List.of(), storage.savedLibraries());
    }

    @Test
    void readsTheSavedKitBackFromTheLibraryFileWithEveryItemInItsSlot() {
        inventoryItems.put(EquipmentSlot.HEAD, new EncodableItemStack("helmet"));
        inventoryItems.put(EquipmentSlot.OFF_HAND, new EncodableItemStack("shield"));
        inventoryItems.put(0, new EncodableItemStack("sword"));
        inventoryItems.put(35, new EncodableItemStack("arrows"));
        kitSaveService.createFromInventory(createPlayer(), "Knight");
        KitService restartedKitService = new KitService();

        new KitLibrary(KitLibraryStorage.NONE, restartedKitService, kitItemSerializer, problems::add, problem -> { })
                .restore(storage.lastSavedLibrary());
        KitDefinition loadedKit = restartedKitService.getKitDefinition();

        assertEquals(4, loadedKit.itemCount());
        assertEquals(Set.of(KitEquipmentSlot.HEAD, KitEquipmentSlot.OFF_HAND), loadedKit.equipmentItems().keySet());
        assertEquals("helmet", labelOf(loadedKit.equipmentItems().get(KitEquipmentSlot.HEAD)));
        assertEquals("shield", labelOf(loadedKit.equipmentItems().get(KitEquipmentSlot.OFF_HAND)));
        assertEquals(Set.of(0, 35), loadedKit.inventoryItems().keySet());
        assertEquals("sword", labelOf(loadedKit.inventoryItems().get(0)));
        assertEquals("arrows", labelOf(loadedKit.inventoryItems().get(35)));
        assertTrue(problems.isEmpty(), problems.toString());
    }

    @Test
    void countsAnItemThatCannotBeReadBackAndSavesTheOtherItems() {
        inventoryItems.put(EquipmentSlot.HEAD, new EncodableItemStack("helmet"));
        inventoryItems.put(0, new EncodableItemStack("sword"));
        inventoryItems.put(1, new EncodableItemStack(UNDECODABLE_LABEL));

        KitSaveResult result = kitSaveService.createFromInventory(createPlayer(), "Knight");

        assertTrue(result.isSaved());
        assertEquals(new SerializedKit(Map.of(KitEquipmentSlot.HEAD, FakeSerializedKits.encode("helmet")),
                        Map.of(0, FakeSerializedKits.encode("sword"), 1, FakeSerializedKits.encode(UNDECODABLE_LABEL))),
                storage.lastSavedLibrary().kits().get(0).serializedKit());
        assertEquals(List.of("Kit Knight saved: 2 items, 1 worn and 1 in the inventory", SAVED_KIT_NOTICE,
                "1 item could not be read back and are no part of the kit, the log names them"),
                result.describe());
        assertEquals(List.of("Kit Knight: The stored kit item of the inventory slot 1 could not be read and is left "
                + "out of the kit"), problems);
        KitDefinition appliedKit = kitService.getKitDefinition();
        assertEquals(Set.of(KitEquipmentSlot.HEAD), appliedKit.equipmentItems().keySet());
        assertEquals(Set.of(0), appliedKit.inventoryItems().keySet());
        assertEquals("sword", labelOf(appliedKit.inventoryItems().get(0)));
    }

    private void createFilledKit(String kitName) {
        kitLibrary.createKit(kitName, FakeSerializedKits.wearingAndCarrying(Map.of(KitEquipmentSlot.HEAD, "helmet"),
                "sword"));
    }

    private static String labelOf(ItemStack itemStack) {
        return ((EncodableItemStack) itemStack).getLabel();
    }

    private Player createPlayer() {
        PlayerInventory inventory = SharedBukkitServer.proxy(PlayerInventory.class, (method, arguments) ->
                switch (method.getName()) {
                    case "getItem" -> inventoryItems.get(arguments[0]);
                    default -> throw new UnsupportedOperationException("PlayerInventory." + method.getName());
                });
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getInventory" -> inventory;
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}
