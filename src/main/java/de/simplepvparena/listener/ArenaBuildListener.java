package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBuildRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
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
        Optional<ArenaShape> arenaShape = arenaRegistry.getArena().getShape();
        if (arenaShape.isEmpty()) {
            return;
        }
        Block placedBlock = event.getBlockPlaced();
        if (!placedBlock.getWorld().getName().equals(arenaShape.get().getWorldName())) {
            return;
        }
        Player player = event.getPlayer();
        if (ArenaBuildRule.isPlacementAllowed(matchService.isActiveFighter(player.getUniqueId()),
                isArenaAdmin(player), arenaShape.get().containsCell(placedBlock.getX(), placedBlock.getZ()))) {
            return;
        }
        event.setCancelled(true);
        TournamentMessages.sendError(player, "Only the fighters of the running round build inside the arena");
    }

    private static boolean isArenaAdmin(Player player) {
        return player.isOp() || player.hasPermission(TournamentCommand.ADMIN_PERMISSION);
    }
}
