package de.simplepvparena.visual;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class VanillaVisuals implements CustomVisuals {

    @Override
    public Optional<String> findFontImage(String identifier) {
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> findItem(String identifier) {
        return Optional.empty();
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
