package de.simplepvparena.listener;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import de.simplepvparena.registration.RegistrationStationButtonCheck;
import de.simplepvparena.registration.RegistrationStationDisplays;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Objects;

public final class RegistrationStationWorldListener implements Listener {

    private final RegistrationStationDisplays stationDisplays;
    private final RegistrationStationButtonCheck buttonCheck;

    public RegistrationStationWorldListener(RegistrationStationDisplays stationDisplays,
                                            RegistrationStationButtonCheck buttonCheck) {
        this.stationDisplays = Objects.requireNonNull(stationDisplays, "stationDisplays");
        this.buttonCheck = Objects.requireNonNull(buttonCheck, "buttonCheck");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        buttonCheck.checkChunk(event.getChunk());
        stationDisplays.showChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        stationDisplays.hideChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        buttonCheck.checkSoonAround(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        buttonCheck.checkSoonAround(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDestroy(BlockDestroyEvent event) {
        buttonCheck.checkSoonAround(event.getBlock());
    }
}
