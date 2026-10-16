package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

public final class BukkitItemStackCodec implements ItemStackCodec {

    @Override
    public byte[] encode(ItemStack[] contents) {
        return ItemStack.serializeItemsAsBytes(contents);
    }

    @Override
    public ItemStack[] decode(byte[] encodedContents) {
        return ItemStack.deserializeItemsFromBytes(encodedContents);
    }
}
