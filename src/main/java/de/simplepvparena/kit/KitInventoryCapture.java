package de.simplepvparena.kit;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;

public final class KitInventoryCapture {

    private KitInventoryCapture() {
    }

    public static SerializedKit capture(Function<KitEquipmentSlot, Optional<String>> equipmentSource,
                                        IntFunction<Optional<String>> inventorySlotSource) {
        Map<KitEquipmentSlot, String> equipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        for (KitEquipmentSlot equipmentSlot : KitEquipmentSlot.values()) {
            equipmentSource.apply(equipmentSlot).ifPresent(storedItem -> equipmentItems.put(equipmentSlot, storedItem));
        }
        Map<Integer, String> inventoryItems = new TreeMap<>();
        for (int slotIndex = 0; slotIndex < SerializedKit.INVENTORY_SLOT_COUNT; slotIndex++) {
            int capturedSlotIndex = slotIndex;
            inventorySlotSource.apply(slotIndex)
                    .ifPresent(storedItem -> inventoryItems.put(capturedSlotIndex, storedItem));
        }
        return new SerializedKit(equipmentItems, inventoryItems);
    }
}
