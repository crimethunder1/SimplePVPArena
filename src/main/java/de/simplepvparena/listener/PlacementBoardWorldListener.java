package de.simplepvparena.listener;

import de.simplepvparena.placement.PlacementBoard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Objects;

public final class PlacementBoardWorldListener implements Listener {

    private final PlacementBoard placementBoard;

    public PlacementBoardWorldListener(PlacementBoard placementBoard) {
        this.placementBoard = Objects.requireNonNull(placementBoard, "placementBoard");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        placementBoard.showChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        placementBoard.hideChunk(event.getChunk());
    }
}
