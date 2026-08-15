package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class PlayerInteractListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final BorderSelectorModeRegistry selectorModes;

    public PlayerInteractListener(MatchService matchService, SpectatorService spectatorService,
                                  BorderSelectorModeRegistry selectorModes) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.selectorModes = selectorModes;
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
            TournamentMessages.sendError(player, "You cannot switch to spectator mode while you are fighting");
            return;
        }
        if (!spectatorService.toggleSpectatorMode(player)) {
            TournamentMessages.sendError(player, "You are not watching the arena, use /tournament spectate");
        }
    }
}
