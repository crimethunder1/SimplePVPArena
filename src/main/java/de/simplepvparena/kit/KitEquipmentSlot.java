package de.simplepvparena.kit;

import org.bukkit.inventory.EquipmentSlot;

import java.util.Locale;
import java.util.Optional;

public enum KitEquipmentSlot {

    HEAD("head", EquipmentSlot.HEAD),
    CHEST("chest", EquipmentSlot.CHEST),
    LEGS("legs", EquipmentSlot.LEGS),
    FEET("feet", EquipmentSlot.FEET),
    OFF_HAND("offhand", EquipmentSlot.OFF_HAND);

    private final String configurationKey;
    private final EquipmentSlot equipmentSlot;

    KitEquipmentSlot(String configurationKey, EquipmentSlot equipmentSlot) {
        this.configurationKey = configurationKey;
        this.equipmentSlot = equipmentSlot;
    }

    public String getConfigurationKey() {
        return configurationKey;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public static Optional<KitEquipmentSlot> findByKey(String configurationKey) {
        if (configurationKey == null) {
            return Optional.empty();
        }
        String normalizedKey = configurationKey.trim().toLowerCase(Locale.ROOT);
        for (KitEquipmentSlot slot : values()) {
            if (slot.configurationKey.equals(normalizedKey)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }
}
