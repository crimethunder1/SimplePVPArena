package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaHeightMarkerService;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class ArenaHeightMarkerListener implements Listener {

    private final ArenaHeightMarkerService heightMarkerService;

    public ArenaHeightMarkerListener(ArenaHeightMarkerService heightMarkerService) {
        this.heightMarkerService = heightMarkerService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!heightMarkerService.isMarkerItem(event.getItemInHand())) {
            return;
        }
        heightMarkerService.markPlacedBlock(event.getPlayer(), event.getBlockPlaced(), event.getHand());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        if (!heightMarkerService.isMarkerItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().getInventory().setItemInMainHand(
                heightMarkerService.switchMarkedEnd(event.getPlayer(), event.getItem()));
    }
}
