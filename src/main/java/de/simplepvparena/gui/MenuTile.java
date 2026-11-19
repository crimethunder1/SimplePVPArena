package de.simplepvparena.gui;

import org.bukkit.Material;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
record MenuTile(Material material, String displayName, List<String> loreLines) {

    MenuTile {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(displayName, "displayName");
        loreLines = List.copyOf(loreLines);
    }
}
