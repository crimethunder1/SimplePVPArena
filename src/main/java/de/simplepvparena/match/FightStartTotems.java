package de.simplepvparena.match;

import org.bukkit.inventory.ItemStack;

public interface FightStartTotems {

    ItemStack createTotem(ItemStack customItem);

    boolean isTotem(ItemStack itemStack);
}
