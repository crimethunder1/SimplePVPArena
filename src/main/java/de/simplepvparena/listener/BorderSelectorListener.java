package de.simplepvparena.listener;

import de.simplepvparena.border.BorderMarkToggle;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.message.BorderMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;

public final class BorderSelectorListener implements Listener {

    private final BorderSelectorModeRegistry selectorModes;
    private final SpectatorBorderService spectatorBorderService;
    private final Predicate<ItemStack> pluginToolCheck;

    public BorderSelectorListener(BorderSelectorModeRegistry selectorModes,
                                  SpectatorBorderService spectatorBorderService,
                                  Predicate<ItemStack> pluginToolCheck) {
        this.selectorModes = Objects.requireNonNull(selectorModes, "selectorModes");
        this.spectatorBorderService = Objects.requireNonNull(spectatorBorderService, "spectatorBorderService");
        this.pluginToolCheck = Objects.requireNonNull(pluginToolCheck, "pluginToolCheck");
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
        if (pluginToolCheck.test(event.getItem())) {
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
        if (AdminPermission.isAdmin(player)) {
            return true;
        }
        selectorModes.deactivate(player.getUniqueId());
        TournamentMessages.sendError(player, Messages.text(BorderMessage.SELECTOR_NO_LONGER_ALLOWED));
        return false;
    }

    private static void report(Player player, BorderMarkToggle toggle, Block block) {
        String message = Messages.text(BorderMessage.MARK_REPORT, Placeholder.of("state", toggle.describe()),
                Placeholder.of("blockX", block.getX()), Placeholder.of("blockY", block.getY()),
                Placeholder.of("blockZ", block.getZ()));
        if (toggle.marks()) {
            TournamentMessages.sendActionBarSuccess(player, message);
            return;
        }
        TournamentMessages.sendActionBarWarning(player, message);
    }
}
