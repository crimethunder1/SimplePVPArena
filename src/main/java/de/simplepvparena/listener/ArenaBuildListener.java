package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBuildRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.arena.ArenaWorldBuildRule;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.ListenerMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.permission.AdminPermission;
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
        boolean arenaAdmin = AdminPermission.isAdmin(player);
        boolean insideFightArea = arenaShape.get().containsCell(placedBlock.getX(), placedBlock.getZ());
        if (!ArenaWorldBuildRule.isChangeAllowed(arenaAdmin, insideFightArea,
                isInsideTheEffectArea(arenaRegistry, placedBlock))) {
            TournamentMessages.sendError(player, Messages.text(ListenerMessage.BUILD_OUTSIDE_THE_ARENA));
            return true;
        }
        if (ArenaBuildRule.isPlacementAllowed(matchService.isActiveFighter(player.getUniqueId()), arenaAdmin,
                insideFightArea)) {
            return false;
        }
        TournamentMessages.sendError(player, Messages.text(ListenerMessage.BUILD_NOT_A_FIGHTER));
        return true;
    }

    static boolean isInsideTheEffectArea(ArenaRegistry arenaRegistry, Block block) {
        return arenaRegistry.findEffectArea()
                .filter(effectArea -> effectArea.containsBlock(block.getWorld().getName(), block.getX(), block.getZ()))
                .isPresent();
    }

    static Optional<ArenaShape> findFightArea(ArenaRegistry arenaRegistry, Block block) {
        return arenaRegistry.getArena().getShape()
                .filter(shape -> block.getWorld().getName().equals(shape.getWorldName()));
    }
}
