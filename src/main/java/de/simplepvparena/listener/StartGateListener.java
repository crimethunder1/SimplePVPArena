package de.simplepvparena.listener;

import de.simplepvparena.gate.StartGateItemService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Objects;

public final class StartGateListener implements Listener {

    private final StartGateItemService gateItemService;

    public StartGateListener(StartGateItemService gateItemService) {
        this.gateItemService = Objects.requireNonNull(gateItemService, "gateItemService");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!gateItemService.isGateItem(event.getItemInHand())) {
            return;
        }
        gateItemService.markPlacedBlock(event.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        gateItemService.clearBlockMark(event.getBlock());
    }
}
