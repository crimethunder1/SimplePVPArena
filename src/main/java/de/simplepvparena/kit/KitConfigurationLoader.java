package de.simplepvparena.kit;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public final class KitConfigurationLoader {

    private static final String ARMOR_SECTION = "armor";
    private static final String ITEMS_SECTION = "items";
    private static final String ENCHANTMENTS_SECTION = "enchantments";

    private static final Map<String, EquipmentSlot> ARMOR_SLOTS_BY_KEY = Map.of(
            "head", EquipmentSlot.HEAD,
            "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS,
            "feet", EquipmentSlot.FEET);

    private final Logger logger;

    public KitConfigurationLoader(Logger logger) {
        this.logger = logger;
    }

    public KitDefinition load(ConfigurationSection kitSection) {
        if (kitSection == null) {
            return KitDefinition.empty();
        }
        return new KitDefinition(readArmorItems(kitSection.getConfigurationSection(ARMOR_SECTION)),
                readInventoryItems(kitSection.getList(ITEMS_SECTION)));
    }

    private Map<EquipmentSlot, KitItem> readArmorItems(ConfigurationSection armorSection) {
        Map<EquipmentSlot, KitItem> armorItems = new EnumMap<>(EquipmentSlot.class);
        if (armorSection == null) {
            return armorItems;
        }
        for (String slotKey : armorSection.getKeys(false)) {
            EquipmentSlot equipmentSlot = ARMOR_SLOTS_BY_KEY.get(slotKey.toLowerCase(Locale.ROOT));
            if (equipmentSlot == null) {
                logger.warning("Unknown armor slot in kit configuration: " + slotKey);
                continue;
            }
            ConfigurationSection itemSection = armorSection.getConfigurationSection(slotKey);
            if (itemSection == null) {
                continue;
            }
            readKitItem(itemSection.getValues(false), itemSection).ifPresent(item -> armorItems.put(equipmentSlot, item));
        }
        return armorItems;
    }

    private List<KitItem> readInventoryItems(List<?> rawItems) {
        List<KitItem> inventoryItems = new ArrayList<>();
        if (rawItems == null) {
            return inventoryItems;
        }
        for (Object rawItem : rawItems) {
            if (rawItem instanceof Map<?, ?> itemValues) {
                readKitItem(itemValues, null).ifPresent(inventoryItems::add);
            }
        }
        return inventoryItems;
    }

    private Optional<KitItem> readKitItem(Map<?, ?> itemValues, ConfigurationSection itemSection) {
        Object rawMaterial = itemValues.get("material");
        if (rawMaterial == null) {
            logger.warning("Kit item without material found, skipping it");
            return Optional.empty();
        }
        Material material = Material.matchMaterial(rawMaterial.toString());
        if (material == null) {
            logger.warning("Unknown material in kit configuration: " + rawMaterial);
            return Optional.empty();
        }
        int amount = itemValues.get("amount") instanceof Number number ? number.intValue() : 1;
        Map<Enchantment, Integer> enchantments = itemSection != null
                ? readEnchantments(itemSection.getConfigurationSection(ENCHANTMENTS_SECTION))
                : readEnchantments(itemValues.get(ENCHANTMENTS_SECTION));
        return Optional.of(new KitItem(material, amount, enchantments));
    }

    private Map<Enchantment, Integer> readEnchantments(ConfigurationSection enchantmentSection) {
        if (enchantmentSection == null) {
            return Map.of();
        }
        return readEnchantments(enchantmentSection.getValues(false));
    }

    private Map<Enchantment, Integer> readEnchantments(Object rawEnchantments) {
        if (!(rawEnchantments instanceof Map<?, ?> enchantmentValues)) {
            return Map.of();
        }
        Map<Enchantment, Integer> enchantments = new java.util.LinkedHashMap<>();
        enchantmentValues.forEach((rawKey, rawLevel) -> {
            Enchantment enchantment = resolveEnchantment(rawKey.toString());
            if (enchantment == null) {
                logger.warning("Unknown enchantment in kit configuration: " + rawKey);
                return;
            }
            int level = rawLevel instanceof Number number ? number.intValue() : 1;
            enchantments.put(enchantment, level);
        });
        return enchantments;
    }

    private Enchantment resolveEnchantment(String enchantmentName) {
        NamespacedKey namespacedKey = NamespacedKey.fromString(enchantmentName.toLowerCase(Locale.ROOT));
        if (namespacedKey == null) {
            return null;
        }
        return Registry.ENCHANTMENT.get(namespacedKey);
    }
}
