package de.simplepvparena.kit;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;

public final class KitConfigurationLoader {

    private static final String ARMOR_SECTION = "armor";
    private static final String ITEMS_SECTION = "items";
    private static final String ENCHANTMENTS_SECTION = "enchantments";

    private final Logger logger;
    private final KitSerialization kitSerialization;

    public KitConfigurationLoader(Logger logger, KitItemSerializer kitItemSerializer) {
        this.logger = logger;
        this.kitSerialization = new KitSerialization(kitItemSerializer);
    }

    public KitDefinition load(ConfigurationSection kitSection) {
        if (kitSection == null) {
            return KitDefinition.empty();
        }
        if (SerializedKit.isWrittenIn(kitSection)) {
            return kitSerialization.toDefinition(SerializedKit.read(kitSection, logger::warning), logger::warning);
        }
        return loadTypedKit(kitSection);
    }

    private KitDefinition loadTypedKit(ConfigurationSection kitSection) {
        return new KitDefinition(readArmorItems(kitSection.getConfigurationSection(ARMOR_SECTION)),
                readInventoryItems(kitSection.getList(ITEMS_SECTION)));
    }

    private Map<KitEquipmentSlot, ItemStack> readArmorItems(ConfigurationSection armorSection) {
        Map<KitEquipmentSlot, ItemStack> armorItems = new EnumMap<>(KitEquipmentSlot.class);
        if (armorSection == null) {
            return armorItems;
        }
        for (String slotKey : armorSection.getKeys(false)) {
            Optional<KitEquipmentSlot> equipmentSlot = KitEquipmentSlot.findByKey(slotKey);
            if (equipmentSlot.isEmpty()) {
                logger.warning("Unknown armor slot in kit configuration: " + slotKey);
                continue;
            }
            ConfigurationSection itemSection = armorSection.getConfigurationSection(slotKey);
            if (itemSection == null) {
                continue;
            }
            readKitItem(itemSection.getValues(false), itemSection)
                    .ifPresent(item -> armorItems.put(equipmentSlot.get(), item.toItemStack()));
        }
        return armorItems;
    }

    private Map<Integer, ItemStack> readInventoryItems(List<?> rawItems) {
        Map<Integer, ItemStack> inventoryItems = new TreeMap<>();
        if (rawItems == null) {
            return inventoryItems;
        }
        for (Object rawItem : rawItems) {
            if (!(rawItem instanceof Map<?, ?> itemValues)) {
                continue;
            }
            int slotIndex = inventoryItems.size();
            if (slotIndex >= SerializedKit.INVENTORY_SLOT_COUNT) {
                logger.warning("The kit configuration holds more than " + SerializedKit.INVENTORY_SLOT_COUNT
                        + " items, the ones beyond that do not fit into an inventory and are skipped");
                break;
            }
            readKitItem(itemValues, null).ifPresent(item -> inventoryItems.put(slotIndex, item.toItemStack()));
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
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(namespacedKey);
    }
}
