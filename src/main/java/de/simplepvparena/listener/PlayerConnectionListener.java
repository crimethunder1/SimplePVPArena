package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.border.BorderSelectorModeRegistry;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.ListenerMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
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
    private final KitService kitService;

    public PlayerConnectionListener(MatchService matchService, SpectatorService spectatorService,
                                    ArenaRegistry arenaRegistry, BorderSelectorModeRegistry selectorModes,
                                    KitService kitService) {
        this.matchService = matchService;
        this.spectatorService = spectatorService;
        this.arenaRegistry = arenaRegistry;
        this.selectorModes = selectorModes;
        this.kitService = kitService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        restoreLeftoverState(player);
        Optional<Match> matchOfPlayer = matchService.findMatchOfPlayer(player.getUniqueId());
        if (matchOfPlayer.isPresent()) {
            spectatorService.sendToTribune(player);
            TournamentMessages.sendInfo(player, Messages.text(ListenerMessage.REJOINED_DURING_MATCH,
                    Placeholder.of("match", matchOfPlayer.get().getMatchId())));
            return;
        }
        if (arenaRegistry.isTeleportOnJoin() && arenaRegistry.isArenaFullyConfigured()) {
            spectatorService.sendToTribune(player);
        }
    }

    private void restoreLeftoverState(Player player) {
        if (kitService.hasAppliedKit(player) && !matchService.isActiveFighter(player.getUniqueId())) {
            kitService.clearKit(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        matchService.handlePlayerEliminated(event.getPlayer().getUniqueId());
        spectatorService.clearAssignment(event.getPlayer().getUniqueId());
        selectorModes.deactivate(event.getPlayer().getUniqueId());
    }
}
