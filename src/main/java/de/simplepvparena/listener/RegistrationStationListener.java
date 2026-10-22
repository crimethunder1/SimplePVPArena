package de.simplepvparena.listener;

import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.registration.RegistrationStationService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

public final class RegistrationStationListener implements Listener {

    private final RegistrationStationService stationService;

    public RegistrationStationListener(RegistrationStationService stationService) {
        this.stationService = Objects.requireNonNull(stationService, "stationService");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (stationService.isTool(event.getItem())) {
            event.setCancelled(true);
            if (!AdminPermission.isAdmin(player)) {
                stationService.takeToolAway(player);
                return;
            }
            if (event.getHand() == EquipmentSlot.HAND && isRightClick(event.getAction())) {
                stationService.useTool(player, event.getClickedBlock());
            }
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || clickedBlock == null) {
            return;
        }
        stationService.useStation(player, clickedBlock);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (AdminPermission.isAdmin(event.getPlayer()) || !stationService.isStation(event.getBlock())) {
            return;
        }
        event.setCancelled(true);
        stationService.refuseBreaking(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStationBroken(BlockBreakEvent event) {
        stationService.removeBrokenStation(event.getPlayer(), event.getBlock());
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
    }
}
