package de.simplepvparena.support;

import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class EncodableItemStack extends ItemStack {

    private final String label;

    public EncodableItemStack(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public byte[] serializeAsBytes() {
        return label.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "EncodableItemStack{" + label + "}";
    }
}
