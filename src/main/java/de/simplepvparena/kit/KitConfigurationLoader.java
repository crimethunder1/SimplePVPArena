package de.simplepvparena.kit;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;

public final class KitConfigurationLoader {

    private static final String ARMOR_SECTION = "armor";
    private static final String ITEMS_SECTION = "items";
    private static final String ENCHANTMENTS_SECTION = "enchantments";
    private static final int MINIMUM_AMOUNT = 1;
    private static final int MAXIMUM_AMOUNT = 99;

    private final Consumer<String> problemReporter;
    private final KitSerialization kitSerialization;
    private final Function<KitItem, ItemStack> itemStackFactory;

    public KitConfigurationLoader(Consumer<String> problemReporter, KitItemSerializer kitItemSerializer,
                                  Function<KitItem, ItemStack> itemStackFactory) {
        this.problemReporter = problemReporter;
        this.kitSerialization = new KitSerialization(kitItemSerializer);
        this.itemStackFactory = itemStackFactory;
    }

    public KitDefinition load(ConfigurationSection kitSection) {
        if (kitSection == null) {
            return KitDefinition.empty();
        }
        if (SerializedKit.isWrittenIn(kitSection)) {
            return kitSerialization.toDefinition(SerializedKit.read(kitSection, problemReporter), problemReporter);
        }
        return loadTypedKit(kitSection);
    }

    public SerializedKit loadSerializedKit(ConfigurationSection kitSection) {
        return kitSerialization.toSerializedKit(load(kitSection), problemReporter);
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
                problemReporter.accept("Unknown armor slot in kit configuration: " + slotKey);
                continue;
            }
            ConfigurationSection itemSection = armorSection.getConfigurationSection(slotKey);
            if (itemSection == null) {
                continue;
            }
            readItemStack(itemSection.getValues(false), itemSection)
                    .ifPresent(itemStack -> armorItems.put(equipmentSlot.get(), itemStack));
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
                problemReporter.accept("The kit configuration holds more than " + SerializedKit.INVENTORY_SLOT_COUNT
                        + " items, the ones beyond that do not fit into an inventory and are skipped");
                break;
            }
            readItemStack(itemValues, null).ifPresent(itemStack -> inventoryItems.put(slotIndex, itemStack));
        }
        return inventoryItems;
    }

    private Optional<ItemStack> readItemStack(Map<?, ?> itemValues, ConfigurationSection itemSection) {
        Object rawMaterial = itemValues.get("material");
        if (rawMaterial == null) {
            problemReporter.accept("Kit item without material found, skipping it");
            return Optional.empty();
        }
        Material material = Material.matchMaterial(rawMaterial.toString());
        if (material == null) {
            problemReporter.accept("Unknown material in kit configuration: " + rawMaterial);
            return Optional.empty();
        }
        int amount = itemValues.get("amount") instanceof Number number ? number.intValue() : 1;
        if (amount < MINIMUM_AMOUNT || amount > MAXIMUM_AMOUNT) {
            problemReporter.accept("Unusable item " + material.name() + " in kit configuration: the amount "
                    + amount + " is not between " + MINIMUM_AMOUNT + " and " + MAXIMUM_AMOUNT);
            return Optional.empty();
        }
        try {
            Map<Enchantment, Integer> enchantments = itemSection != null
                    ? readEnchantments(itemSection.getConfigurationSection(ENCHANTMENTS_SECTION))
                    : readEnchantments(itemValues.get(ENCHANTMENTS_SECTION));
            return Optional.of(itemStackFactory.apply(new KitItem(material, amount, enchantments)));
        } catch (RuntimeException failure) {
            problemReporter.accept("Unusable item " + material.name() + " in kit configuration: "
                    + describe(failure));
            return Optional.empty();
        }
    }

    private static String describe(RuntimeException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
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
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        enchantmentValues.forEach((rawKey, rawLevel) -> {
            Enchantment enchantment = resolveEnchantment(rawKey.toString());
            if (enchantment == null) {
                problemReporter.accept("Unknown enchantment in kit configuration: " + rawKey);
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
