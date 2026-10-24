package de.simplepvparena.listener;

import de.simplepvparena.arena.StandingsBoardToolService;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

public final class StandingsBoardToolListener implements Listener {

    private final StandingsBoardToolService toolService;

    public StandingsBoardToolListener(StandingsBoardToolService toolService) {
        this.toolService = Objects.requireNonNull(toolService, "toolService");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!toolService.isTool(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!AdminPermission.isAdmin(player)) {
            toolService.takeToolAway(player);
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND && isRightClick(event.getAction())) {
            toolService.useTool(player, event.getClickedBlock());
        }
    }

    private static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR;
    }
}
