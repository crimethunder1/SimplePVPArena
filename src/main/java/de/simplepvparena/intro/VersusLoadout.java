package de.simplepvparena.intro;

import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitEquipmentSlot;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@NullMarked
public record VersusLoadout(Map<EquipmentSlot, ItemStack> armour, Optional<ItemStack> weapon,
                            Optional<ItemStack> bow, Optional<ItemStack> crossbow, Optional<ItemStack> shield) {

    public VersusLoadout {
        armour = Map.copyOf(armour);
    }

    public static VersusLoadout of(KitDefinition kit) {
        Map<EquipmentSlot, ItemStack> armour = new EnumMap<>(EquipmentSlot.class);
        ItemStack shield = null;
        for (Map.Entry<KitEquipmentSlot, ItemStack> entry : kit.equipmentItems().entrySet()) {
            ItemStack item = entry.getValue();
            if (entry.getKey() == KitEquipmentSlot.OFF_HAND) {
                if (item.getType() == Material.SHIELD) {
                    shield = item;
                }
                continue;
            }
            armour.put(entry.getKey().getEquipmentSlot(), item);
        }
        ItemStack weapon = null;
        ItemStack bow = null;
        ItemStack crossbow = null;
        ItemStack firstItem = null;
        for (ItemStack item : new TreeMap<>(kit.inventoryItems()).values()) {
            Material material = item.getType();
            if (firstItem == null && !material.isAir()) {
                firstItem = item;
            }
            if (weapon == null && isMeleeWeapon(material)) {
                weapon = item;
            } else if (bow == null && material == Material.BOW) {
                bow = item;
            } else if (crossbow == null && material == Material.CROSSBOW) {
                crossbow = item;
            } else if (shield == null && material == Material.SHIELD) {
                shield = item;
            }
        }
        if (weapon == null) {
            weapon = firstItem;
        }
        return new VersusLoadout(armour, Optional.ofNullable(weapon), Optional.ofNullable(bow),
                Optional.ofNullable(crossbow), Optional.ofNullable(shield));
    }

    public Optional<ItemStack> rangedWeapon() {
        return bow.or(() -> crossbow);
    }

    static boolean isMeleeWeapon(Material material) {
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_SPEAR")
                || material == Material.MACE || material == Material.TRIDENT;
    }
}
