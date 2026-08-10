package de.simplepvparena.kit;

import org.bukkit.inventory.EquipmentSlot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record KitDefinition(Map<EquipmentSlot, KitItem> armorItems, List<KitItem> inventoryItems) {

    public KitDefinition {
        Objects.requireNonNull(armorItems, "armorItems");
        Objects.requireNonNull(inventoryItems, "inventoryItems");
        armorItems = Map.copyOf(armorItems);
        inventoryItems = List.copyOf(inventoryItems);
    }

    public static KitDefinition empty() {
        return new KitDefinition(Map.of(), List.of());
    }

    public boolean isEmpty() {
        return armorItems.isEmpty() && inventoryItems.isEmpty();
    }
}
