package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

public final class BukkitKitItemSerializer implements KitItemSerializer {

    @Override
    public byte[] encode(ItemStack itemStack) {
        return itemStack.serializeAsBytes();
    }

    @Override
    public ItemStack decode(byte[] itemBytes) {
        return ItemStack.deserializeBytes(itemBytes);
    }
}
