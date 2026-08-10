package de.simplepvparena.listener;

import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.match.MatchService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class SpectatorBorderListener implements Listener {

    private final MatchService matchService;
    private final SpectatorBorderService spectatorBorderService;

    public SpectatorBorderListener(MatchService matchService, SpectatorBorderService spectatorBorderService) {
        this.matchService = matchService;
        this.spectatorBorderService = spectatorBorderService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!spectatorBorderService.isBorderItem(event.getItemInHand())) {
            return;
        }
        spectatorBorderService.markBlock(event.getBlockPlaced());
        event.getPlayer().sendMessage("The block is now a spectator border, everybody who is not fighting "
                + "is stopped by it");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (spectatorBorderService.isBorderBlock(event.getBlock())) {
            spectatorBorderService.clearBlockMark(event.getBlock());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) {
            return;
        }
        Location destination = event.getTo();
        if (!hasChangedBlock(event.getFrom(), destination)) {
            return;
        }
        Player player = event.getPlayer();
        if (spectatorBorderService.isMovementBlocked(matchService.isActiveFighter(player.getUniqueId()), destination)) {
            event.setCancelled(true);
        }
    }

    private boolean hasChangedBlock(Location from, Location to) {
        if (to == null) {
            return false;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || !from.getWorld().equals(to.getWorld());
    }
}
