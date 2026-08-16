package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.MatchService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public final class PlayerRespawnListener implements Listener {

    private final Plugin plugin;
    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final ArenaRegistry arenaRegistry;

    public PlayerRespawnListener(Plugin plugin, MatchService matchService, SpectatorService spectatorService,
                                 ArenaRegistry arenaRegistry) {
        this.plugin = plugin;
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.arenaRegistry = arenaRegistry;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Location> fighterSpawn = matchService.findActiveFighterSpawn(player.getUniqueId());
        if (fighterSpawn.isPresent()) {
            returnToTheRound(event, player, fighterSpawn.get());
            return;
        }
        boolean belongsToArena = matchService.findMatchOfPlayer(player.getUniqueId()).isPresent()
                || spectatorService.findSpectatorMode(player.getUniqueId()).isPresent();
        if (!belongsToArena) {
            return;
        }
        arenaRegistry.getArena().getTribuneSpawn()
                .flatMap(PositionSnapshot::toLocation)
                .ifPresent(event::setRespawnLocation);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                spectatorService.sendToTribune(player);
            }
        });
    }

    private void returnToTheRound(PlayerRespawnEvent event, Player player, Location fighterSpawn) {
        event.setRespawnLocation(fighterSpawn);
        spectatorService.clearAssignment(player.getUniqueId());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                matchService.equipActiveFighter(player);
            }
        });
    }
}
