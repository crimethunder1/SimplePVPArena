package de.simplepvparena.gui;

import org.bukkit.Material;

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

    static MenuTile create(Material material, String displayName, List<String> loreLines) {
        return new MenuTile(material, displayName, loreLines);
    }

    static MenuTile create(Material material, String displayName, String... loreLines) {
        return create(material, displayName, List.of(loreLines));
    }

    static MenuTile info(String displayName, List<String> loreLines) {
        return create(INFO_MATERIAL, displayName, loreLines);
    }

    static MenuTile unavailable(String displayName, String reason) {
        return create(UNAVAILABLE_MATERIAL, displayName, reason);
    }

    static MenuTile back(String target) {
        return create(BACK_MATERIAL, "Back", "Returns to " + target);
    }
}
