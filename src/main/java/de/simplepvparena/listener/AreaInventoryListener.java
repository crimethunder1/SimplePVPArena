package de.simplepvparena.listener;

import de.simplepvparena.arena.AreaInventoryService;
import de.simplepvparena.arena.EffectAreaPresence;
import de.simplepvparena.message.AreaInventoryMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AreaInventoryListener implements Listener {

    private final Plugin plugin;
    private final AreaInventoryService areaInventoryService;
    private final List<EffectAreaPresence> presences = new ArrayList<>();

    public AreaInventoryListener(Plugin plugin, AreaInventoryService areaInventoryService,
                                 EffectAreaPresence... furtherPresences) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.areaInventoryService = Objects.requireNonNull(areaInventoryService, "areaInventoryService");
        presences.add(areaInventoryService);
        for (EffectAreaPresence furtherPresence : furtherPresences) {
            presences.add(Objects.requireNonNull(furtherPresence, "furtherPresence"));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location destination = event.getTo();
        if (hasChangedBlock(event.getFrom(), destination)) {
            reconcile(event.getPlayer(), destination);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getTo() != null) {
            reconcile(event.getPlayer(), event.getTo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        reconcile(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                reconcile(player, player.getLocation());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        reconcile(event.getPlayer(), event.getPlayer().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (areaInventoryService.isLockedAt(player.getLocation())) {
            event.setCancelled(true);
            TournamentMessages.sendActionBarWarning(player, Messages.text(AreaInventoryMessage.DROP_LOCKED));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ENDER_CHEST
                || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (areaInventoryService.isLockedAt(player.getLocation())) {
            event.setCancelled(true);
            TournamentMessages.sendActionBarWarning(player,
                    Messages.text(AreaInventoryMessage.ENDER_CHEST_LOCKED));
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (areaInventoryService.isWearingKit(player) || !areaInventoryService.isLockedAt(player.getLocation())) {
            return;
        }
        event.getDrops().clear();
        if (!areaInventoryService.hasDeposited(player)) {
            event.setKeepInventory(true);
        }
    }

    private void reconcile(Player player, Location location) {
        for (EffectAreaPresence presence : presences) {
            presence.reconcile(player, location);
        }
    }

    private static boolean hasChangedBlock(Location from, Location to) {
        if (to == null) {
            return false;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || !Objects.equals(from.getWorld(), to.getWorld());
    }
}
