package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.FakeKitItemSerializer;
import de.simplepvparena.support.SharedBukkitServer;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitSaveServiceTest {

    private static final List<String> FILLED_KIT_DESCRIPTION =
            List.of("Worn items: 1 of 5", "Inventory items: 1 of 36 slots");
    private static final String SAVED_KIT_NOTICE = "It is written into config.yml and is handed out from the next "
            + "round on, a running round keeps the kit its fighters were given";
    private static final String UNDECODABLE_LABEL = "broken";
    private static final String KIT_PATH = "kit";

    private final KitService kitService = new KitService();
    private final List<SerializedKit> savedKits = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private final Map<Object, ItemStack> inventoryItems = new HashMap<>();
    private final FakeKitItemSerializer kitItemSerializer = new FakeKitItemSerializer(UNDECODABLE_LABEL);
    private final KitSaveService kitSaveService = new KitSaveService(kitService, savedKits::add, problems::add,
            kitItemSerializer);

    KitSaveServiceTest() {
        SharedBukkitServer.install();
        kitService.setKitDefinition(new KitDefinition(Map.of(KitEquipmentSlot.HEAD, new FakeItemStack("helmet")),
                Map.of(0, new FakeItemStack("sword"))));
    }

    @Test
    void keepsTheKitAndWritesNothingWhenTheInventoryIsEmpty() {
        KitSaveResult result = kitSaveService.saveFromInventory(createPlayer());

        assertFalse(result.isSaved());
        assertEquals(List.of("Your inventory is empty, the kit was not changed"), result.describe());
        assertTrue(savedKits.isEmpty());
        assertEquals(FILLED_KIT_DESCRIPTION, kitSaveService.describeKit());
    }

    @Test
    void savesTheInventoryWhenOnlyTheOffhandHoldsAnItem() {
        inventoryItems.put(EquipmentSlot.OFF_HAND, new EncodableItemStack("shield"));

        KitSaveResult result = kitSaveService.saveFromInventory(createPlayer());

        assertTrue(result.isSaved());
        assertEquals(1, savedKits.size());
        assertEquals(Set.of(KitEquipmentSlot.OFF_HAND), savedKits.get(0).equipmentItems().keySet());
        assertTrue(savedKits.get(0).inventoryItems().isEmpty());
    }

    @Test
    void savesAFilledInventoryAsBefore() {
        inventoryItems.put(EquipmentSlot.HEAD, new EncodableItemStack("helmet"));
        inventoryItems.put(35, new EncodableItemStack("sword"));

        KitSaveResult result = kitSaveService.saveFromInventory(createPlayer());

        assertTrue(result.isSaved());
        assertEquals(List.of(new SerializedKit(Map.of(KitEquipmentSlot.HEAD, encode("helmet")),
                Map.of(35, encode("sword")))), savedKits);
        assertEquals(List.of("Kit saved: 2 items, 1 worn and 1 in the inventory", SAVED_KIT_NOTICE),
                result.describe());
        assertTrue(problems.isEmpty(), problems.toString());
        assertEquals(FILLED_KIT_DESCRIPTION, kitSaveService.describeKit());
        KitDefinition appliedKit = kitService.getKitDefinition();
        assertEquals("helmet", labelOf(appliedKit.equipmentItems().get(KitEquipmentSlot.HEAD)));
        assertEquals(Set.of(35), appliedKit.inventoryItems().keySet());
        assertEquals("sword", labelOf(appliedKit.inventoryItems().get(35)));
    }

    @Test
    void readsTheSavedKitBackFromTheConfigurationWithEveryItemInItsSlot() {
        inventoryItems.put(EquipmentSlot.HEAD, new EncodableItemStack("helmet"));
        inventoryItems.put(EquipmentSlot.OFF_HAND, new EncodableItemStack("shield"));
        inventoryItems.put(0, new EncodableItemStack("sword"));
        inventoryItems.put(35, new EncodableItemStack("arrows"));
        kitSaveService.saveFromInventory(createPlayer());
        MemoryConfiguration configuration = new MemoryConfiguration();

        savedKits.get(0).writeInto(configuration, KIT_PATH);
        KitDefinition loadedKit = new KitConfigurationLoader(createQuietLogger(), kitItemSerializer)
                .load(configuration.getConfigurationSection(KIT_PATH));

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

        KitSaveResult result = kitSaveService.saveFromInventory(createPlayer());

        assertTrue(result.isSaved());
        assertEquals(List.of(new SerializedKit(Map.of(KitEquipmentSlot.HEAD, encode("helmet")),
                Map.of(0, encode("sword"), 1, encode(UNDECODABLE_LABEL)))), savedKits);
        assertEquals(List.of("Kit saved: 2 items, 1 worn and 1 in the inventory", SAVED_KIT_NOTICE,
                "1 item could not be read back and are no part of the kit, the log names them"),
                result.describe());
        assertEquals(List.of("The stored kit item of the inventory slot 1 could not be read and is left out "
                + "of the kit"), problems);
        KitDefinition appliedKit = kitService.getKitDefinition();
        assertEquals(Set.of(KitEquipmentSlot.HEAD), appliedKit.equipmentItems().keySet());
        assertEquals(Set.of(0), appliedKit.inventoryItems().keySet());
        assertEquals("sword", labelOf(appliedKit.inventoryItems().get(0)));
    }

    private static String encode(String label) {
        return KitItemCodec.encode(label.getBytes(StandardCharsets.UTF_8));
    }

    private static String labelOf(ItemStack itemStack) {
        return ((EncodableItemStack) itemStack).getLabel();
    }

    private static Logger createQuietLogger() {
        Logger logger = Logger.getLogger("KitSaveServiceTest");
        logger.setUseParentHandlers(false);
        return logger;
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
