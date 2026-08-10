package de.simplepvparena.listener;

import de.simplepvparena.gui.AdminMenuHolder;
import de.simplepvparena.gui.AdminMenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Objects;

public final class AdminMenuListener implements Listener {

    private final AdminMenuService adminMenuService;

    public AdminMenuListener(AdminMenuService adminMenuService) {
        this.adminMenuService = Objects.requireNonNull(adminMenuService, "adminMenuService");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AdminMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !(clickedInventory.getHolder() instanceof AdminMenuHolder)) {
            return;
        }
        adminMenuService.handleClick(player, holder, event.getSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AdminMenuHolder) {
            event.setCancelled(true);
        }
    }
}
