package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

public interface ItemStackCodec {

    byte[] encode(ItemStack[] contents);

    ItemStack[] decode(byte[] encodedContents);
}
