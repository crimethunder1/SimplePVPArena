package de.simplepvparena.arena;

import org.bukkit.inventory.ItemStack;

public interface SpectatorItems {

    ItemStack createSpectatorToggleItem();

    boolean isSpectatorToggleItem(ItemStack itemStack);
}
