package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaFightRegion;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import java.util.Objects;

public final class ArenaItemRuleListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final ArenaRegistry arenaRegistry;

    public ArenaItemRuleListener(MatchService matchService, SpectatorService spectatorService,
                                 ArenaRegistry arenaRegistry) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.spectatorService = Objects.requireNonNull(spectatorService, "spectatorService");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (AdminPermission.isAdmin(player)) {
            return;
        }
        if (isBoundToTheArena(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || AdminPermission.isAdmin(player)) {
            return;
        }
        if (isBoundToTheArena(player) || isInsideTheFightArea(event.getItem().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean isBoundToTheArena(Player player) {
        return matchService.findMatchOfPlayer(player.getUniqueId()).isPresent()
                || spectatorService.findSpectatorMode(player.getUniqueId()).isPresent()
                || isInsideTheFightArea(player.getLocation());
    }

    private boolean isInsideTheFightArea(Location location) {
        return ArenaFightRegion.contains(arenaRegistry.getArena().getShape(), location);
    }
}
