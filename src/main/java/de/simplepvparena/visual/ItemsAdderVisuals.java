package de.simplepvparena.visual;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class ItemsAdderVisuals implements CustomVisuals, Listener {

    private final Runnable loadListener;
    private volatile boolean ready = ItemsAdder.areItemsLoaded();

    public ItemsAdderVisuals() {
        this(() -> { });
    }

    public ItemsAdderVisuals(Runnable loadListener) {
        this.loadListener = Objects.requireNonNull(loadListener, "loadListener");
    }

    @Override
    public Optional<String> findFontImage(String identifier) {
        if (!ready) {
            return Optional.empty();
        }
        FontImageWrapper fontImage = FontImageWrapper.instance(identifier);
        if (fontImage == null || !fontImage.exists()) {
            return Optional.empty();
        }
        return Optional.ofNullable(fontImage.getString());
    }

    @Override
    public Optional<ItemStack> findItem(String identifier) {
        if (!ready) {
            return Optional.empty();
        }
        CustomStack customStack = CustomStack.getInstance(identifier);
        if (customStack == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customStack.getItemStack()).map(ItemStack::clone);
    }

    @Override
    public boolean playTotemAnimation(Player player, String identifier) {
        return ready && ItemsAdder.playTotemAnimation(player, identifier);
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @EventHandler
    public void onItemsAdderLoadData(ItemsAdderLoadDataEvent event) {
        ready = true;
        loadListener.run();
    }
}
