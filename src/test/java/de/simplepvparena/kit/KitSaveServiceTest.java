package de.simplepvparena.kit;

import de.simplepvparena.support.EncodableItemStack;
import de.simplepvparena.support.FakeItemStack;
import de.simplepvparena.support.SharedBukkitServer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitSaveServiceTest {

    private static final List<String> FILLED_KIT_DESCRIPTION =
            List.of("Worn items: 1 of 5", "Inventory items: 1 of 36 slots");

    private final KitService kitService = new KitService();
    private final List<SerializedKit> savedKits = new ArrayList<>();
    private final Map<Object, ItemStack> inventoryItems = new HashMap<>();
    private final KitSaveService kitSaveService = new KitSaveService(kitService, savedKits::add, problem -> { });

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
        assertEquals(List.of("Kit saved: 0 items, 0 worn and 0 in the inventory",
                "It is written into config.yml and is handed out from the next round on, "
                        + "a running round keeps the kit its fighters were given",
                "2 items could not be read back and are no part of the kit, the log names them"),
                result.describe());
        assertEquals(List.of("The kit is empty, no fighter is given anything"), kitSaveService.describeKit());
    }

    private static String encode(String label) {
        return KitItemCodec.encode(label.getBytes(StandardCharsets.UTF_8));
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
