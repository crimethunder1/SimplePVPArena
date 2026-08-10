package de.simplepvparena.kit;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Objects;

public record KitItem(Material material, int amount, Map<Enchantment, Integer> enchantments) {

    public KitItem {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(enchantments, "enchantments");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
        enchantments = Map.copyOf(enchantments);
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(material, amount);
        if (enchantments.isEmpty()) {
            return itemStack;
        }
        ItemMeta itemMeta = itemStack.getItemMeta();
        enchantments.forEach((enchantment, level) -> itemMeta.addEnchant(enchantment, level, true));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
