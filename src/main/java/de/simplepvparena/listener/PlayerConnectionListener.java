package de.simplepvparena.listener;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public final class PlayerConnectionListener implements Listener {

    private final MatchService matchService;
    private final SpectatorService spectatorService;
    private final ArenaRegistry arenaRegistry;

    public PlayerConnectionListener(MatchService matchService, SpectatorService spectatorService, ArenaRegistry arenaRegistry) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.arenaRegistry = arenaRegistry;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Optional<Match> matchOfPlayer = matchService.findMatchOfPlayer(player.getUniqueId());
        if (matchOfPlayer.isPresent()) {
            spectatorService.sendToTribune(player, matchOfPlayer.get().getArena());
            player.sendMessage("You rejoined during match " + matchOfPlayer.get().getMatchId()
                    + " and take part again from the next round on");
            return;
        }
        Optional<Arena> firstConfiguredArena = arenaRegistry.getArenas().stream()
                .filter(Arena::isFullyConfigured)
                .findFirst();
        firstConfiguredArena.ifPresent(arena -> spectatorService.sendToTribune(player, arena));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        matchService.handlePlayerEliminated(event.getPlayer().getUniqueId());
        spectatorService.clearAssignment(event.getPlayer().getUniqueId());
    }
}
