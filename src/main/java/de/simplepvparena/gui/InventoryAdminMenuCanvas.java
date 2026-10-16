package de.simplepvparena.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class InventoryAdminMenuCanvas implements AdminMenuCanvas {

    private final Inventory inventory;

    InventoryAdminMenuCanvas(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    @Override
    public void set(int slot, MenuTile tile) {
        inventory.setItem(slot, toItemStack(tile));
    }

    @Override
    public void clear() {
        inventory.clear();
    }

    static ItemStack toItemStack(MenuTile tile) {
        ItemStack itemStack = new ItemStack(tile.material());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(tile.displayName()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String loreLine : tile.loreLines()) {
            lore.add(Component.text(loreLine).decoration(TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) {
            itemMeta.lore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
