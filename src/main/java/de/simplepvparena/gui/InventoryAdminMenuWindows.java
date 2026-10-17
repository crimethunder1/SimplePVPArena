package de.simplepvparena.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.function.Consumer;

final class InventoryAdminMenuWindows implements AdminMenuWindows {

    private final Plugin plugin;

    InventoryAdminMenuWindows(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void open(Player player, AdminMenuHolder holder, int size, String title,
                     Consumer<AdminMenuCanvas> filler) {
        Inventory inventory = plugin.getServer().createInventory(holder, size, Component.text(title));
        holder.setInventory(inventory);
        filler.accept(new InventoryAdminMenuCanvas(inventory));
        plugin.getServer().getScheduler().runTask(plugin, () -> player.openInventory(inventory));
    }

    @Override
    public void refresh(Player player, AdminMenuHolder holder, Consumer<AdminMenuCanvas> filler) {
        filler.accept(new InventoryAdminMenuCanvas(holder.getInventory()));
    }

    @Override
    public void close(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> player.closeInventory());
    }

    @Override
    public boolean isShowing(Player player, AdminMenuType menuType) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof AdminMenuHolder holder
                && holder.getMenuType() == menuType;
    }
}
