package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.PositionSnapshot;
import de.simplepvparena.arena.SpectatorService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.CommandMessage;
import de.simplepvparena.message.ListenerMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.logging.Level;

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
        boolean waitsForTheRoundCleaning = fighterSpawn.isPresent() && matchService.isCleaningBetweenRounds();
        if (fighterSpawn.isPresent() && !waitsForTheRoundCleaning) {
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
                sendToTribune(player, waitsForTheRoundCleaning);
            }
        });
    }

    private void sendToTribune(Player player, boolean warnWithoutATribune) {
        boolean sentToTribune;
        try {
            sentToTribune = spectatorService.sendToTribune(player);
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, Messages.text(ListenerMessage.NOT_SENT_TO_TRIBUNE,
                    Placeholder.of("player", player.getName())), failure);
            return;
        }
        if (!sentToTribune && warnWithoutATribune) {
            TournamentMessages.sendWarning(player, Messages.text(CommandMessage.SPECTATE_NO_TRIBUNE_POSITION));
        }
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
