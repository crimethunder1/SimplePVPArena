package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaFlightExitRule;
import de.simplepvparena.arena.ArenaMovementRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.SpectatorMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
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

    public PlayerMoveListener(MatchService matchService,
                              SpectatorService spectatorService,
                              ArenaRegistry arenaRegistry) {
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
        Optional<ArenaShape> arenaShape = arenaRegistry.getArena().getShape();
        if (arenaShape.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        boolean originInside = arenaShape.get().contains(from);
        boolean destinationInside = arenaShape.get().contains(to);
        boolean flyingSpectator = isFlyingSpectator(player);
        boolean activeFighter = isActiveFighter(player);
        boolean admin = AdminPermission.isAdmin(player);
        if (!ArenaFlightExitRule.isMovementAllowed(activeFighter, belongsIntoArena(player), admin,
                flyingSpectator, originInside, destinationInside)) {
            event.setCancelled(true);
        }
        if (ArenaFlightExitRule.leavesTheArena(flyingSpectator, originInside, destinationInside)) {
            endArenaFlight(player);
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

    private void endArenaFlight(Player player) {
        if (!spectatorService.sendToTribune(player)) {
            TournamentMessages.sendWarning(player, Messages.text(SpectatorMessage.TRIBUNE_SPAWN_NOT_SET));
            return;
        }
        player.setFallDistance(0.0F);
    }

    private boolean isMovementAllowed(Player player, Location origin, Location destination) {
        Optional<ArenaShape> arenaShape = arenaRegistry.getArena().getShape();
        if (arenaShape.isEmpty()) {
            return true;
        }
        return ArenaMovementRule.isMovementAllowed(isActiveFighter(player), belongsIntoArena(player),
                AdminPermission.isAdmin(player), arenaShape.get().contains(origin),
                arenaShape.get().contains(destination));
    }

    private boolean belongsIntoArena(Player player) {
        if (isActiveFighter(player)) {
            return true;
        }
        return isFlyingSpectator(player);
    }

    private boolean isActiveFighter(Player player) {
        return matchService.isActiveFighter(player.getUniqueId());
    }

    private boolean isFlyingSpectator(Player player) {
        return spectatorService.isFlyingSpectator(player.getUniqueId());
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
