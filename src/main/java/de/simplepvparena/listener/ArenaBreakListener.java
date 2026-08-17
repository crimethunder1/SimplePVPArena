package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaBreakRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaWorldBuildRule;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import java.util.Objects;
import java.util.Optional;

public final class ArenaBreakListener implements Listener {

    private final MatchService matchService;
    private final ArenaRegistry arenaRegistry;
    private final ArenaBaselineService arenaBaselineService;

    public ArenaBreakListener(MatchService matchService, ArenaRegistry arenaRegistry,
                              ArenaBaselineService arenaBaselineService) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.arenaBaselineService = Objects.requireNonNull(arenaBaselineService, "arenaBaselineService");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isRemovalRefused(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        if (isRemovalRefused(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        if (ArenaBuildListener.isPlacementRefused(matchService, arenaRegistry, event.getPlayer(),
                event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private boolean isRemovalRefused(Player player, Block block) {
        Optional<ArenaShape> arenaShape = ArenaBuildListener.findFightArea(arenaRegistry, block);
        if (arenaShape.isEmpty()) {
            return false;
        }
        ArenaShape shape = arenaShape.get();
        boolean arenaAdmin = ArenaBuildListener.isArenaAdmin(player);
        boolean insideFightArea = shape.containsCell(block.getX(), block.getZ());
        if (!ArenaWorldBuildRule.isChangeAllowed(arenaAdmin, insideFightArea)) {
            TournamentMessages.sendActionBarError(player, ArenaWorldBuildRule.REFUSAL);
            return true;
        }
        boolean activeFighter = matchService.isActiveFighter(player.getUniqueId());
        boolean partOfTheOriginalArena = ArenaBreakRule.isPartOfTheOriginalArena(
                arenaBaselineService.findSnapshot(shape), block.getX(), block.getY(), block.getZ(), block.getType());
        if (ArenaBreakRule.isBreakingAllowed(activeFighter, arenaAdmin, insideFightArea,
                partOfTheOriginalArena)) {
            return false;
        }
        TournamentMessages.sendActionBarError(player, activeFighter
                ? "The arena stays as it is, only what was placed during the match comes down again"
                : "Only the fighters of the running round change blocks inside the arena");
        return true;
    }
}
