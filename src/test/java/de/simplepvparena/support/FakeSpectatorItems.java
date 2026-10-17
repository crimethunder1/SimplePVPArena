package de.simplepvparena.support;

import de.simplepvparena.arena.SpectatorItems;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class FakeSpectatorItems implements SpectatorItems {

    public static final String SPECTATOR_TOGGLE_LABEL = "spectator toggle";

    private final List<ItemStack> createdItems = new ArrayList<>();

    private RuntimeException creationFailure;

    @Override
    public ItemStack createSpectatorToggleItem() {
        if (creationFailure != null) {
            throw creationFailure;
        }
        ItemStack toggleItem = new FakeItemStack(SPECTATOR_TOGGLE_LABEL);
        createdItems.add(toggleItem);
        return toggleItem;
    }

    @Override
    public boolean isSpectatorToggleItem(ItemStack itemStack) {
        return itemStack instanceof FakeItemStack fakeItemStack
                && SPECTATOR_TOGGLE_LABEL.equals(fakeItemStack.getLabel());
    }

    public void failCreationWith(RuntimeException failure) {
        this.creationFailure = failure;
    }

    public List<ItemStack> getCreatedItems() {
        return List.copyOf(createdItems);
    }
}
