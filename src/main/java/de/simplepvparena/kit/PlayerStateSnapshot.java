package de.simplepvparena.kit;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public final class PlayerStateSnapshot {

    private final ItemStack[] contents;
    private final int level;
    private final float exp;
    private final int foodLevel;
    private final float saturation;
    private final double health;

    public PlayerStateSnapshot(ItemStack[] contents, int level, float exp, int foodLevel, float saturation,
                               double health) {
        this.contents = copyContents(Objects.requireNonNull(contents, "contents"));
        this.level = level;
        this.exp = exp;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.health = health;
    }

    public ItemStack[] getContents() {
        return copyContents(contents);
    }

    public int getLevel() {
        return level;
    }

    public float getExp() {
        return exp;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public double getHealth() {
        return health;
    }

    private static ItemStack[] copyContents(ItemStack[] sourceContents) {
        ItemStack[] copiedContents = new ItemStack[sourceContents.length];
        for (int slotIndex = 0; slotIndex < sourceContents.length; slotIndex++) {
            ItemStack itemStack = sourceContents[slotIndex];
            copiedContents[slotIndex] = itemStack == null ? null : itemStack.clone();
        }
        return copiedContents;
    }
}
