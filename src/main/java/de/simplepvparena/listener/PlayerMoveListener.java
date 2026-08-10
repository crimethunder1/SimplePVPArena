package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegionType;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public final class PlayerMoveListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final ArenaRegistry arenaRegistry;

    public PlayerMoveListener(MatchService matchService, SpectatorService spectatorService, ArenaRegistry arenaRegistry) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.arenaRegistry = arenaRegistry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (!hasChangedBlock(from, to)) {
            return;
        }
        if (!isMovementAllowed(event.getPlayer(), to)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                || event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        if (!isMovementAllowed(event.getPlayer(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    private boolean isMovementAllowed(Player player, Location destination) {
        Optional<Match> matchOfPlayer = matchService.findMatchOfPlayer(player.getUniqueId());
        if (matchOfPlayer.isPresent() && matchService.isActiveFighter(player.getUniqueId())) {
            return matchOfPlayer.get().getArena().containsInFightRegion(destination);
        }
        Optional<SpectatorMode> spectatorMode = spectatorService.findSpectatorMode(player.getUniqueId());
        if (spectatorMode.isPresent() && spectatorMode.get() == SpectatorMode.SPECTATOR_BOX) {
            return arenaRegistry.getArena().getRegion(ArenaRegionType.SPECTATOR_BOX)
                    .map(spectatorBox -> spectatorBox.contains(destination))
                    .orElse(true);
        }
        return !arenaRegistry.containsInFightRegion(destination);
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
