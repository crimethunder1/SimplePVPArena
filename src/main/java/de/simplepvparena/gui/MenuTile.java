package de.simplepvparena.gui;

import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

record MenuTile(Material material, String displayName, List<String> loreLines) {

    MenuTile {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(displayName, "displayName");
        loreLines = List.copyOf(loreLines);
    }
}
