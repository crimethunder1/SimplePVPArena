package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaHeightMarkerService;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();
        if (!AdminPermission.isAdmin(player)) {
            event.setCancelled(true);
            heightMarkerService.takeMarkersAway(player);
            return;
        }
        heightMarkerService.markPlacedBlock(player, event.getBlockPlaced(), event.getHand());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!heightMarkerService.isMarkerItem(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        if (!AdminPermission.isAdmin(player)) {
            event.setCancelled(true);
            heightMarkerService.takeMarkersAway(player);
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        event.setCancelled(true);
        player.getInventory().setItemInMainHand(heightMarkerService.switchMarkedEnd(player, event.getItem()));
    }
}
