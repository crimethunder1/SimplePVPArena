package de.simplepvparena.listener;

import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.placement.BracketBoard;
import de.simplepvparena.placement.BracketBoardDisplays;
import de.simplepvparena.placement.BracketBoardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Optional;

@NullMarked
public final class BracketBoardListener implements Listener {

    private final BracketBoardService boardService;
    private final BracketBoardDisplays boardDisplays;

    public BracketBoardListener(BracketBoardService boardService, BracketBoardDisplays boardDisplays) {
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.boardDisplays = Objects.requireNonNull(boardDisplays, "boardDisplays");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!boardService.isBoardItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!AdminPermission.isAdmin(player)) {
            boardService.takeBoardItemsAway(player);
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null) {
            boardService.placeBoard(player, event.getClickedBlock(), event.getBlockFace());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Optional<BracketBoard> board = boardService.findBoardAt(event.getBlock());
        if (board.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (AdminPermission.isAdmin(event.getPlayer())) {
            boardService.removeBoard(event.getPlayer(), board.get());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        boardDisplays.showChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        boardDisplays.hideChunk(event.getChunk());
    }
}
