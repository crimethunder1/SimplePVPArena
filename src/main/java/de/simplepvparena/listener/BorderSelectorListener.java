package de.simplepvparena.listener;

import de.simplepvparena.border.BorderMarkToggle;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

public final class BorderSelectorListener implements Listener {

    private final BorderSelectorModeRegistry selectorModes;
    private final SpectatorBorderService spectatorBorderService;

    public BorderSelectorListener(BorderSelectorModeRegistry selectorModes,
                                  SpectatorBorderService spectatorBorderService) {
        this.selectorModes = Objects.requireNonNull(selectorModes, "selectorModes");
        this.spectatorBorderService = Objects.requireNonNull(spectatorBorderService, "spectatorBorderService");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (!selectorModes.isActive(player.getUniqueId())) {
            return;
        }
        if (!isStillAdmin(player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        report(player, spectatorBorderService.toggleBlockMark(clickedBlock), clickedBlock);
    }

    private boolean isStillAdmin(Player player) {
        if (player.hasPermission(TournamentCommand.ADMIN_PERMISSION)) {
            return true;
        }
        selectorModes.deactivate(player.getUniqueId());
        TournamentMessages.sendError(player, "You are not allowed to mark border blocks any more, "
                + "the border selector mode was switched off");
        return false;
    }

    private static void report(Player player, BorderMarkToggle toggle, Block block) {
        String message = toggle.describe() + ": " + block.getX() + " " + block.getY() + " " + block.getZ();
        if (toggle.marks()) {
            TournamentMessages.sendActionBarSuccess(player, message);
            return;
        }
        TournamentMessages.sendActionBarWarning(player, message);
    }
}
