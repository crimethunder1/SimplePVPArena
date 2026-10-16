package de.simplepvparena.support;

import de.simplepvparena.kit.ItemStackCodec;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class FakeItemStackCodec implements ItemStackCodec {

    @Override
    public byte[] encode(ItemStack[] contents) {
        ByteArrayOutputStream encodedContents = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(encodedContents)) {
            output.writeInt(contents.length);
            for (ItemStack itemStack : contents) {
                output.writeBoolean(itemStack != null);
                if (itemStack != null) {
                    output.writeUTF(((FakeItemStack) itemStack).getLabel());
                }
            }
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        return encodedContents.toByteArray();
    }

    @Override
    public ItemStack[] decode(byte[] encodedContents) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(encodedContents))) {
            ItemStack[] contents = new ItemStack[input.readInt()];
            for (int slotIndex = 0; slotIndex < contents.length; slotIndex++) {
                contents[slotIndex] = input.readBoolean() ? new FakeItemStack(input.readUTF()) : null;
            }
            return contents;
        } catch (IOException failure) {
            throw new IllegalArgumentException("unreadable contents", failure);
        }
    }
}
