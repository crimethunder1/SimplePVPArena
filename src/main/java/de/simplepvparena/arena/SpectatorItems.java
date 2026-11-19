package de.simplepvparena.arena;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SpectatorItems {

    ItemStack createSpectatorToggleItem();

    boolean isSpectatorToggleItem(ItemStack itemStack);
}
