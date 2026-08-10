package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaMovementRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.arena.SpectatorService;
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
        if (!isMovementAllowed(event.getPlayer(), from, to)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN
                || event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            return;
        }
        if (!isMovementAllowed(event.getPlayer(), event.getFrom(), event.getTo())) {
            event.setCancelled(true);
        }
    }

    private boolean isMovementAllowed(Player player, Location origin, Location destination) {
        Optional<ArenaShape> arenaShape = arenaRegistry.getArena().getShape();
        if (arenaShape.isEmpty()) {
            return true;
        }
        return ArenaMovementRule.isMovementAllowed(belongsIntoArena(player), player.isOp(),
                arenaShape.get().contains(origin), arenaShape.get().contains(destination));
    }

    private boolean belongsIntoArena(Player player) {
        if (matchService.isActiveFighter(player.getUniqueId())) {
            return true;
        }
        return spectatorService.findSpectatorMode(player.getUniqueId())
                .filter(spectatorMode -> spectatorMode == SpectatorMode.ARENA_FLIGHT)
                .isPresent();
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
