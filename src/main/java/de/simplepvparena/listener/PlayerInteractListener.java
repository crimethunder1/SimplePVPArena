package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.ListenerMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.SpectatorMessage;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class PlayerInteractListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final BorderSelectorModeRegistry selectorModes;
    private final Logger logger;

    public PlayerInteractListener(MatchService matchService, SpectatorService spectatorService,
                                  BorderSelectorModeRegistry selectorModes, Logger logger) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.selectorModes = selectorModes;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!spectatorService.isSpectatorToggleItem(event.getItem())) {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && selectorModes.isActive(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (matchService.isActiveFighter(player.getUniqueId())) {
            TournamentMessages.sendError(player, Messages.text(SpectatorMessage.SWITCH_WHILE_FIGHTING));
            return;
        }
        boolean switched;
        try {
            switched = spectatorService.toggleSpectatorMode(player);
        } catch (RuntimeException failure) {
            logger.log(Level.WARNING, Messages.text(ListenerMessage.SPECTATOR_MODE_NOT_SWITCHED,
                    Placeholder.of("player", player.getName())), failure);
            return;
        }
        if (!switched) {
            TournamentMessages.sendError(player, Messages.text(SpectatorMessage.NOT_WATCHING));
        }
    }
}
