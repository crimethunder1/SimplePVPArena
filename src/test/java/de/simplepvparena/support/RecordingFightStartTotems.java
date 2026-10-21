package de.simplepvparena.support;

import de.simplepvparena.match.FightStartTotems;
import org.bukkit.inventory.ItemStack;

public final class RecordingFightStartTotems implements FightStartTotems {

    private int createdTotemCount;
    private RuntimeException creationFailure;

    public void failCreationWith(RuntimeException failure) {
        this.creationFailure = failure;
    }

    public int createdTotemCount() {
        return createdTotemCount;
    }

    @Override
    public ItemStack createTotem(ItemStack customItem) {
        if (creationFailure != null) {
            throw creationFailure;
        }
        createdTotemCount++;
        return ((LabelledItemStack) customItem).asFightStartTotem();
    }

    @Override
    public boolean isTotem(ItemStack itemStack) {
        return itemStack instanceof LabelledItemStack labelledItemStack && labelledItemStack.isFightStartTotem();
    }
}
