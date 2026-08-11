package de.simplepvparena.kit;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;

public record SerializedKit(Map<KitEquipmentSlot, String> equipmentItems, Map<Integer, String> inventoryItems) {

    public static final String EQUIPMENT_SECTION = "equipment";
    public static final String SLOTS_SECTION = "slots";
    public static final String FORMAT_KEY = "format";
    public static final String FORMAT_NAME = "base64-item";

    public static final int INVENTORY_SLOT_COUNT = 36;

    public SerializedKit {
        Objects.requireNonNull(equipmentItems, "equipmentItems");
        Objects.requireNonNull(inventoryItems, "inventoryItems");
        equipmentItems = Collections.unmodifiableMap(equipmentItems.isEmpty()
                ? new EnumMap<>(KitEquipmentSlot.class)
                : new EnumMap<>(equipmentItems));
        inventoryItems = Collections.unmodifiableMap(new TreeMap<>(inventoryItems));
    }

    public static SerializedKit empty() {
        return new SerializedKit(Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return equipmentItems.isEmpty() && inventoryItems.isEmpty();
    }

    public int itemCount() {
        return equipmentItems.size() + inventoryItems.size();
    }

    public static boolean isWrittenIn(ConfigurationSection kitSection) {
        return kitSection != null
                && (kitSection.isConfigurationSection(EQUIPMENT_SECTION)
                        || kitSection.isConfigurationSection(SLOTS_SECTION));
    }

    public static SerializedKit read(ConfigurationSection kitSection, Consumer<String> problemReporter) {
        if (kitSection == null) {
            return empty();
        }
        return new SerializedKit(readEquipmentItems(kitSection.getConfigurationSection(EQUIPMENT_SECTION),
                problemReporter), readInventoryItems(kitSection.getConfigurationSection(SLOTS_SECTION),
                problemReporter));
    }

    private static Map<KitEquipmentSlot, String> readEquipmentItems(ConfigurationSection equipmentSection,
                                                                    Consumer<String> problemReporter) {
        Map<KitEquipmentSlot, String> equipmentItems = new EnumMap<>(KitEquipmentSlot.class);
        if (equipmentSection == null) {
            return equipmentItems;
        }
        for (String slotKey : equipmentSection.getKeys(false)) {
            Optional<KitEquipmentSlot> equipmentSlot = KitEquipmentSlot.findByKey(slotKey);
            if (equipmentSlot.isEmpty()) {
                problemReporter.accept("Unknown kit slot " + slotKey + ", the known ones are "
                        + describeKnownSlots());
                continue;
            }
            String storedItem = equipmentSection.getString(slotKey);
            if (storedItem == null || storedItem.isBlank()) {
                problemReporter.accept("The kit slot " + slotKey + " holds no item and is skipped");
                continue;
            }
            equipmentItems.put(equipmentSlot.get(), storedItem);
        }
        return equipmentItems;
    }

    private static Map<Integer, String> readInventoryItems(ConfigurationSection slotsSection,
                                                           Consumer<String> problemReporter) {
        Map<Integer, String> inventoryItems = new TreeMap<>();
        if (slotsSection == null) {
            return inventoryItems;
        }
        for (String slotKey : slotsSection.getKeys(false)) {
            Optional<Integer> slotIndex = readSlotIndex(slotKey, problemReporter);
            if (slotIndex.isEmpty()) {
                continue;
            }
            String storedItem = slotsSection.getString(slotKey);
            if (storedItem == null || storedItem.isBlank()) {
                problemReporter.accept("The kit inventory slot " + slotKey + " holds no item and is skipped");
                continue;
            }
            inventoryItems.put(slotIndex.get(), storedItem);
        }
        return inventoryItems;
    }

    private static Optional<Integer> readSlotIndex(String slotKey, Consumer<String> problemReporter) {
        int slotIndex;
        try {
            slotIndex = Integer.parseInt(slotKey.trim());
        } catch (NumberFormatException notANumber) {
            problemReporter.accept("The kit inventory slot " + slotKey + " is not a slot number and is skipped");
            return Optional.empty();
        }
        if (slotIndex < 0 || slotIndex >= INVENTORY_SLOT_COUNT) {
            problemReporter.accept("The kit inventory slot " + slotKey + " lies outside of the "
                    + INVENTORY_SLOT_COUNT + " inventory slots and is skipped");
            return Optional.empty();
        }
        return Optional.of(slotIndex);
    }

    public void writeInto(ConfigurationSection parentSection, String kitPath) {
        parentSection.set(kitPath, null);
        ConfigurationSection kitSection = parentSection.createSection(kitPath);
        kitSection.set(FORMAT_KEY, FORMAT_NAME);
        ConfigurationSection equipmentSection = kitSection.createSection(EQUIPMENT_SECTION);
        equipmentItems.forEach((equipmentSlot, storedItem) ->
                equipmentSection.set(equipmentSlot.getConfigurationKey(), storedItem));
        ConfigurationSection slotsSection = kitSection.createSection(SLOTS_SECTION);
        new TreeMap<>(inventoryItems).forEach((slotIndex, storedItem) ->
                slotsSection.set(String.valueOf(slotIndex), storedItem));
    }

    private static String describeKnownSlots() {
        StringBuilder knownSlots = new StringBuilder();
        for (KitEquipmentSlot equipmentSlot : KitEquipmentSlot.values()) {
            if (!knownSlots.isEmpty()) {
                knownSlots.append(", ");
            }
            knownSlots.append(equipmentSlot.getConfigurationKey());
        }
        return knownSlots.toString();
    }
}
