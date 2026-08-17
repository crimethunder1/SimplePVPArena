package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBuildRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaWorldBuildRule;
import de.simplepvparena.command.TournamentCommand;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Objects;
import java.util.Optional;

public final class ArenaBuildListener implements Listener {

    private final MatchService matchService;
    private final ArenaRegistry arenaRegistry;

    public ArenaBuildListener(MatchService matchService, ArenaRegistry arenaRegistry) {
        this.matchService = Objects.requireNonNull(matchService, "matchService");
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPlacementRefused(matchService, arenaRegistry, event.getPlayer(), event.getBlockPlaced())) {
            event.setCancelled(true);
        }
    }

    static boolean isPlacementRefused(MatchService matchService, ArenaRegistry arenaRegistry, Player player,
                                      Block placedBlock) {
        Optional<ArenaShape> arenaShape = findFightArea(arenaRegistry, placedBlock);
        if (arenaShape.isEmpty()) {
            return false;
        }
        boolean arenaAdmin = isArenaAdmin(player);
        boolean insideFightArea = arenaShape.get().containsCell(placedBlock.getX(), placedBlock.getZ());
        if (!ArenaWorldBuildRule.isChangeAllowed(arenaAdmin, insideFightArea)) {
            TournamentMessages.sendError(player, ArenaWorldBuildRule.REFUSAL);
            return true;
        }
        if (ArenaBuildRule.isPlacementAllowed(matchService.isActiveFighter(player.getUniqueId()), arenaAdmin,
                insideFightArea)) {
            return false;
        }
        TournamentMessages.sendError(player, "Only the fighters of the running round build inside the arena");
        return true;
    }

    static Optional<ArenaShape> findFightArea(ArenaRegistry arenaRegistry, Block block) {
        return arenaRegistry.getArena().getShape()
                .filter(shape -> block.getWorld().getName().equals(shape.getWorldName()));
    }

    static boolean isArenaAdmin(Player player) {
        return player.isOp() || player.hasPermission(TournamentCommand.ADMIN_PERMISSION);
    }
}
