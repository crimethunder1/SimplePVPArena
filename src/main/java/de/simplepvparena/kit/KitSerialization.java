package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class KitSerialization {

    private final KitItemSerializer kitItemSerializer;

    public KitSerialization(KitItemSerializer kitItemSerializer) {
        this.kitItemSerializer = Objects.requireNonNull(kitItemSerializer, "kitItemSerializer");
    }

    public Optional<String> encode(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(KitItemCodec.encode(kitItemSerializer.encode(itemStack)));
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
    }

    public Optional<ItemStack> decode(String storedItem) {
        return KitItemCodec.decode(storedItem).flatMap(this::deserialize);
    }

    private Optional<ItemStack> deserialize(byte[] itemBytes) {
        try {
            ItemStack itemStack = kitItemSerializer.decode(itemBytes);
            return itemStack == null || itemStack.isEmpty() ? Optional.empty() : Optional.of(itemStack);
        } catch (RuntimeException broken) {
            return Optional.empty();
        }
    }

    public KitDefinition toDefinition(SerializedKit serializedKit, Consumer<String> problemReporter) {
        Map<KitEquipmentSlot, ItemStack> equipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        serializedKit.equipmentItems().forEach((equipmentSlot, storedItem) -> {
            Optional<ItemStack> itemStack = decode(storedItem);
            if (itemStack.isEmpty()) {
                problemReporter.accept("The stored kit item of the slot " + equipmentSlot.getConfigurationKey()
                        + " could not be read and is left out of the kit");
                return;
            }
            equipmentItems.put(equipmentSlot, itemStack.get());
        });
        Map<Integer, ItemStack> inventoryItems = new TreeMap<>();
        serializedKit.inventoryItems().forEach((slotIndex, storedItem) -> {
            Optional<ItemStack> itemStack = decode(storedItem);
            if (itemStack.isEmpty()) {
                problemReporter.accept("The stored kit item of the inventory slot " + slotIndex
                        + " could not be read and is left out of the kit");
                return;
            }
            inventoryItems.put(slotIndex, itemStack.get());
        });
        return new KitDefinition(equipmentItems, inventoryItems);
    }

    public SerializedKit toSerializedKit(KitDefinition kitDefinition, Consumer<String> problemReporter) {
        Map<KitEquipmentSlot, String> equipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        kitDefinition.equipmentItems().forEach((equipmentSlot, itemStack) -> {
            Optional<String> storedItem = encode(itemStack);
            if (storedItem.isEmpty()) {
                problemReporter.accept("The kit item of the slot " + equipmentSlot.getConfigurationKey()
                        + " could not be stored and is left out of the kit");
                return;
            }
            equipmentItems.put(equipmentSlot, storedItem.get());
        });
        Map<Integer, String> inventoryItems = new TreeMap<>();
        kitDefinition.inventoryItems().forEach((slotIndex, itemStack) -> {
            Optional<String> storedItem = encode(itemStack);
            if (storedItem.isEmpty()) {
                problemReporter.accept("The kit item of the inventory slot " + slotIndex
                        + " could not be stored and is left out of the kit");
                return;
            }
            inventoryItems.put(slotIndex, storedItem.get());
        });
        return new SerializedKit(equipmentItems, inventoryItems);
    }
}
