package de.simplepvparena.listener;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
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

    public PlayerRespawnListener(Plugin plugin, MatchService matchService, SpectatorService spectatorService) {
        this.plugin = plugin;
        this.matchService = matchService;
        this.spectatorService = spectatorService;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Optional<Arena> arena = matchService.findMatchOfPlayer(player.getUniqueId())
                .map(Match::getArena)
                .or(() -> spectatorService.findAssignedArena(player.getUniqueId()));
        if (arena.isEmpty()) {
            return;
        }
        arena.get().getTribuneSpawn()
                .flatMap(PositionSnapshot::toLocation)
                .ifPresent(event::setRespawnLocation);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                spectatorService.sendToTribune(player, arena.get());
            }
        });
    }
}
