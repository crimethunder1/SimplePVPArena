package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.SpectatorBorderService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Objects;

public final class SpectatorBorderListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final SpectatorBorderService spectatorBorderService;

    public SpectatorBorderListener(MatchService matchService, SpectatorService spectatorService,
                                   SpectatorBorderService spectatorBorderService) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.spectatorService = Objects.requireNonNull(spectatorService, "spectatorService");
        this.spectatorBorderService = Objects.requireNonNull(spectatorBorderService, "spectatorBorderService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (spectatorBorderService.isBorderBlock(event.getBlock())) {
            spectatorBorderService.clearBlockMark(event.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) {
            return;
        }
        Location origin = event.getFrom();
        Location destination = event.getTo();
        if (!hasChangedBlock(origin, destination)) {
            return;
        }
        Player player = event.getPlayer();
        if (!spectatorBorderService.isMovementAllowed(countsAsActiveFighter(player),
                spectatorService.isFlyingSpectator(player.getUniqueId()), AdminPermission.isAdmin(player),
                origin, destination)) {
            event.setCancelled(true);
        }
    }

    private boolean countsAsActiveFighter(Player player) {
        return matchService.isActiveFighter(player.getUniqueId()) && !matchService.isCleaningBetweenRounds();
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
