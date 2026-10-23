package de.simplepvparena.support;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class FakeItemStack extends ItemStack {

    private final String label;

    public FakeItemStack(String label) {
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
        return new FakeItemStack(label);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FakeItemStack otherItemStack && label.equals(otherItemStack.label);
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public String toString() {
        return "FakeItemStack{" + label + "}";
    }
}
