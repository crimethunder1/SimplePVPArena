package de.simplepvparena.support;

import de.simplepvparena.kit.KitEquipmentSlot;
import de.simplepvparena.kit.KitItemCodec;
import de.simplepvparena.kit.SerializedKit;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

public final class FakeSerializedKits {

    private FakeSerializedKits() {
    }

    public static String encode(String label) {
        return KitItemCodec.encode(label.getBytes(StandardCharsets.UTF_8));
    }

    public static SerializedKit carrying(String... inventoryLabels) {
        Map<Integer, String> inventoryItems = new TreeMap<>();
        for (int slotIndex = 0; slotIndex < inventoryLabels.length; slotIndex++) {
            inventoryItems.put(slotIndex, encode(inventoryLabels[slotIndex]));
        }
        return new SerializedKit(Map.of(), inventoryItems);
    }

    public static SerializedKit wearingAndCarrying(Map<KitEquipmentSlot, String> equipmentLabels,
                                                   String... inventoryLabels) {
        Map<KitEquipmentSlot, String> equipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        equipmentLabels.forEach((equipmentSlot, label) -> equipmentItems.put(equipmentSlot, encode(label)));
        return new SerializedKit(equipmentItems, carrying(inventoryLabels).inventoryItems());
    }
}
