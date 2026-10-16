package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record KitDefinition(Map<KitEquipmentSlot, ItemStack> equipmentItems,
                            Map<Integer, ItemStack> inventoryItems) {

    public KitDefinition {
        Objects.requireNonNull(equipmentItems, "equipmentItems");
        Objects.requireNonNull(inventoryItems, "inventoryItems");
        Map<KitEquipmentSlot, ItemStack> copiedEquipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        equipmentItems.forEach((equipmentSlot, itemStack) ->
                copiedEquipmentItems.put(equipmentSlot, itemStack.clone()));
        Map<Integer, ItemStack> copiedInventoryItems = new TreeMap<>();
        inventoryItems.forEach((slotIndex, itemStack) -> copiedInventoryItems.put(slotIndex, itemStack.clone()));
        equipmentItems = Collections.unmodifiableMap(copiedEquipmentItems);
        inventoryItems = Collections.unmodifiableMap(copiedInventoryItems);
    }

    public static KitDefinition empty() {
        return new KitDefinition(Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return equipmentItems.isEmpty() && inventoryItems.isEmpty();
    }

    public int itemCount() {
        return equipmentItems.size() + inventoryItems.size();
    }

    public List<String> describe() {
        if (isEmpty()) {
            return List.of("The kit is empty, no fighter is given anything");
        }
        List<String> lines = new ArrayList<>();
        lines.add("Worn items: " + equipmentItems.size() + " of " + KitEquipmentSlot.values().length);
        lines.add("Inventory items: " + inventoryItems.size() + " of " + SerializedKit.INVENTORY_SLOT_COUNT
                + " slots");
        return lines;
    }
}
