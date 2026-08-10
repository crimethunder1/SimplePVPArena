package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathListener implements Listener {

    private final MatchService matchService;

    public PlayerDeathListener(MatchService matchService) {
        this.matchService = matchService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!matchService.isActiveFighter(player.getUniqueId())) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        matchService.handlePlayerEliminated(player.getUniqueId());
    }
}
