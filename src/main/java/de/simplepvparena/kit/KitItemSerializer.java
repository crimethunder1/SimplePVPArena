package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

public interface KitItemSerializer {

    byte[] encode(ItemStack itemStack);

    ItemStack decode(byte[] itemBytes);
}
