package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ItemStackCodec {

    byte[] encode(ItemStack[] contents);

    ItemStack[] decode(byte[] encodedContents);
}
