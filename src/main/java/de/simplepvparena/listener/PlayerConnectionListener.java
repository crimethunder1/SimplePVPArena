package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
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
    private final BorderSelectorModeRegistry selectorModes;

    public PlayerConnectionListener(MatchService matchService, SpectatorService spectatorService,
                                    ArenaRegistry arenaRegistry, BorderSelectorModeRegistry selectorModes) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.arenaRegistry = arenaRegistry;
        this.selectorModes = selectorModes;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Optional<Match> matchOfPlayer = matchService.findMatchOfPlayer(player.getUniqueId());
        if (matchOfPlayer.isPresent()) {
            spectatorService.sendToTribune(player);
            TournamentMessages.sendInfo(player, "You rejoined during match "
                    + matchOfPlayer.get().getMatchId() + ", you take part again from the next round on");
            return;
        }
        if (arenaRegistry.isArenaFullyConfigured()) {
            spectatorService.sendToTribune(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        matchService.handlePlayerEliminated(event.getPlayer().getUniqueId());
        spectatorService.clearAssignment(event.getPlayer().getUniqueId());
        selectorModes.deactivate(event.getPlayer().getUniqueId());
    }
}
