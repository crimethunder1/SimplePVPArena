package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.PlacedBlockTracker;
import de.simplepvparena.match.MatchService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

@NullMarked
public final class PlacedBlockListener implements Listener {

    private final MatchService matchService;
    private final ArenaRegistry arenaRegistry;
    private final PlacedBlockTracker tracker;

    public PlacedBlockListener(MatchService matchService, ArenaRegistry arenaRegistry, PlacedBlockTracker tracker) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isTracked(event.getPlayer(), event.getBlockPlaced())) {
            return;
        }
        if (event instanceof BlockMultiPlaceEvent multiPlace) {
            tracker.recordReplacedStates(multiPlace.getReplacedBlockStates());
            return;
        }
        tracker.recordReplacedStates(List.of(event.getBlockReplacedState()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block target = event.getBlock();
        if (isTracked(event.getPlayer(), target)) {
            tracker.recordBlock(target);
        }
    }

    private boolean isTracked(Player player, Block block) {
        if (matchService.findMatchOfPlayer(player.getUniqueId()).isEmpty()) {
            return false;
        }
        return arenaRegistry.findEffectArea()
                .filter(area -> area.containsBlock(block.getWorld().getName(), block.getX(), block.getZ()))
                .isPresent();
    }
}
