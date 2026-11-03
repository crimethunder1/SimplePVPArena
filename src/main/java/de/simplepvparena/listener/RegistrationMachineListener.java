package de.simplepvparena.listener;

import de.simplepvparena.permission.AdminPermission;
import de.simplepvparena.registration.RegistrationMachine;
import de.simplepvparena.registration.RegistrationMachineDisplays;
import de.simplepvparena.registration.RegistrationMachineService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;
import java.util.Optional;

public final class RegistrationMachineListener implements Listener {

    private final RegistrationMachineService machineService;
    private final RegistrationMachineDisplays machineDisplays;

    public RegistrationMachineListener(RegistrationMachineService machineService,
                                       RegistrationMachineDisplays machineDisplays) {
        this.machineService = Objects.requireNonNull(machineService, "machineService");
        this.machineDisplays = Objects.requireNonNull(machineDisplays, "machineDisplays");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!machineService.isMachineItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!AdminPermission.isAdmin(player)) {
            machineService.takeMachineItemsAway(player);
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null) {
            machineService.placeMachine(player, event.getClickedBlock(), event.getBlockFace());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Optional<RegistrationMachine> machine = machineDisplays.findMachineOfClickArea(event.getRightClicked());
        if (machine.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) {
            machineService.useMachine(event.getPlayer(), machine.get());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Optional<RegistrationMachine> machine = machineService.findMachineAt(event.getBlock());
        if (machine.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (AdminPermission.isAdmin(event.getPlayer())) {
            machineService.removeMachine(event.getPlayer(), machine.get());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        machineDisplays.showChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        machineDisplays.hideChunk(event.getChunk());
    }
}
