package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.MatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class PlayerInteractListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;

    public PlayerInteractListener(MatchService matchService, SpectatorService spectatorService) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
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
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (matchService.isActiveFighter(player.getUniqueId())) {
            player.sendMessage("You cannot switch to spectator mode while you are fighting");
            return;
        }
        if (!spectatorService.toggleSpectatorMode(player)) {
            player.sendMessage("You are not watching the arena, use /tournament spectate");
        }
    }
}
