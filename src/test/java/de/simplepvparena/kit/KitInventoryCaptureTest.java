package de.simplepvparena.kit;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitInventoryCaptureTest {

    @Test
    void takesEveryWornSlotAndEveryInventorySlotExactlyOnce() {
        List<KitEquipmentSlot> askedEquipmentSlots = new ArrayList<>();
        List<Integer> askedInventorySlots = new ArrayList<>();

        KitInventoryCapture.capture(equipmentSlot -> {
            askedEquipmentSlots.add(equipmentSlot);
            return Optional.of("worn-" + equipmentSlot.getConfigurationKey());
        }, slotIndex -> {
            askedInventorySlots.add(slotIndex);
            return Optional.of("slot-" + slotIndex);
        });

        assertEquals(List.of(KitEquipmentSlot.values()), askedEquipmentSlots);
        assertEquals(SerializedKit.INVENTORY_SLOT_COUNT, askedInventorySlots.size());
        assertEquals(0, askedInventorySlots.get(0).intValue());
        assertEquals(SerializedKit.INVENTORY_SLOT_COUNT - 1,
                askedInventorySlots.get(askedInventorySlots.size() - 1).intValue());
    }

    @Test
    void storesTheItemOfEverySlotUnderThatSlot() {
        SerializedKit capturedKit = KitInventoryCapture.capture(
                equipmentSlot -> Optional.of("worn-" + equipmentSlot.getConfigurationKey()),
                slotIndex -> Optional.of("slot-" + slotIndex));

        assertEquals("worn-offhand", capturedKit.equipmentItems().get(KitEquipmentSlot.OFF_HAND));
        assertEquals("worn-head", capturedKit.equipmentItems().get(KitEquipmentSlot.HEAD));
        assertEquals("slot-0", capturedKit.inventoryItems().get(0));
        assertEquals("slot-35", capturedKit.inventoryItems().get(35));
        assertEquals(KitEquipmentSlot.values().length + SerializedKit.INVENTORY_SLOT_COUNT,
                capturedKit.itemCount());
    }

    @Test
    void skipsEmptySlotsWithoutMovingTheItemsAroundThem() {
        SerializedKit capturedKit = KitInventoryCapture.capture(
                equipmentSlot -> equipmentSlot == KitEquipmentSlot.CHEST
                        ? Optional.of("chestplate")
                        : Optional.empty(),
                slotIndex -> slotIndex == 3 ? Optional.of("sword") : Optional.empty());

        assertEquals(Map.of(KitEquipmentSlot.CHEST, "chestplate"), capturedKit.equipmentItems());
        assertEquals(Map.of(3, "sword"), capturedKit.inventoryItems());
    }

    @Test
    void capturesAnEmptyKitOutOfAnEmptyInventory() {
        SerializedKit capturedKit = KitInventoryCapture.capture(equipmentSlot -> Optional.empty(),
                slotIndex -> Optional.empty());

        assertTrue(capturedKit.isEmpty());
    }
}
