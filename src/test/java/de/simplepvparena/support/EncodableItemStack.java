package de.simplepvparena.support;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class EncodableItemStack extends ItemStack {

    private final String label;

    public EncodableItemStack(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack clone() {
        return new EncodableItemStack(label);
    }

    @Override
    public String toString() {
        return "EncodableItemStack{" + label + "}";
    }
}
