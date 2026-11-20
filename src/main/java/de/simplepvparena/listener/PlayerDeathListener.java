package de.simplepvparena.listener;

import de.simplepvparena.match.MatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class PlayerDeathListener implements Listener {

    private final MatchService matchService;

    public PlayerDeathListener(MatchService matchService) {
        this.matchService = matchService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!matchService.isActiveFighter(player.getUniqueId())) {
            return;
        }
        event.setKeepInventory(false);
        event.getDrops().clear();
        event.setDroppedExp(0);
        matchService.handlePlayerEliminated(player.getUniqueId());
    }
}
