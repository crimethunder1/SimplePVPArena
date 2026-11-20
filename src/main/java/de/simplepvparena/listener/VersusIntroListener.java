package de.simplepvparena.listener;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import de.simplepvparena.intro.VersusIntroService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class VersusIntroListener implements Listener {

    private final VersusIntroService introService;

    public VersusIntroListener(VersusIntroService introService) {
        this.introService = Objects.requireNonNull(introService, "introService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
        if (introService.isWatching(event.getPlayer().getUniqueId())
                && introService.isCamera(event.getSpectatorTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        introService.stopWatching(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        introService.restoreAfterAnInterruptedIntro(event.getPlayer());
    }
}
