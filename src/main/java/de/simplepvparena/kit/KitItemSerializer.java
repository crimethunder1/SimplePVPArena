package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface KitItemSerializer {

    byte[] encode(ItemStack itemStack);

    ItemStack decode(byte[] itemBytes);
}
