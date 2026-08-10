package de.simplepvparena.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

final class MenuItems {

    static final Material INFO_MATERIAL = Material.BOOK;
    static final Material CONFIRM_MATERIAL = Material.LIME_WOOL;
    static final Material REFUSE_MATERIAL = Material.RED_WOOL;
    static final Material DESTRUCTIVE_MATERIAL = Material.BARRIER;
    static final Material BACK_MATERIAL = Material.OAK_DOOR;
    static final Material PAGE_MATERIAL = Material.ARROW;
    static final Material UNAVAILABLE_MATERIAL = Material.GRAY_DYE;

    private MenuItems() {
    }

    static ItemStack create(Material material, String displayName, List<String> loreLines) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String loreLine : loreLines) {
            lore.add(Component.text(loreLine).decoration(TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) {
            itemMeta.lore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    static ItemStack create(Material material, String displayName, String... loreLines) {
        return create(material, displayName, List.of(loreLines));
    }

    static ItemStack info(String displayName, List<String> loreLines) {
        return create(INFO_MATERIAL, displayName, loreLines);
    }

    static ItemStack unavailable(String displayName, String reason) {
        return create(UNAVAILABLE_MATERIAL, displayName, reason);
    }

    static ItemStack back(String target) {
        return create(BACK_MATERIAL, "Back", "Returns to " + target);
    }
}
