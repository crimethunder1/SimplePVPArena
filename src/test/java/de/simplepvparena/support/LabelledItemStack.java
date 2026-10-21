package de.simplepvparena.support;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class LabelledItemStack extends ItemStack {

    private final String label;
    private final boolean fightStartTotem;

    private LabelledItemStack(String label, boolean fightStartTotem) {
        this.label = Objects.requireNonNull(label, "label");
        this.fightStartTotem = fightStartTotem;
    }

    public static LabelledItemStack of(String label) {
        return new LabelledItemStack(label, false);
    }

    public LabelledItemStack asFightStartTotem() {
        return new LabelledItemStack(label, true);
    }

    public String getLabel() {
        return label;
    }

    public boolean isFightStartTotem() {
        return fightStartTotem;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public ItemStack clone() {
        return new LabelledItemStack(label, fightStartTotem);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LabelledItemStack otherItemStack && label.equals(otherItemStack.label)
                && fightStartTotem == otherItemStack.fightStartTotem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, fightStartTotem);
    }

    @Override
    public String toString() {
        return fightStartTotem ? "totem " + label : label;
    }
}
