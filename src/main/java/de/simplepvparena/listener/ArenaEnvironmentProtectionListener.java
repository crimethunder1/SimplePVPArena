package de.simplepvparena.listener;

import de.simplepvparena.arena.ArenaBaselineService;
import de.simplepvparena.arena.ArenaEnvironmentProtectionRule;
import de.simplepvparena.arena.ArenaRegistry;
import de.simplepvparena.arena.ArenaShape;
import de.simplepvparena.permission.AdminPermission;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ArenaEnvironmentProtectionListener implements Listener {

    private final ArenaRegistry arenaRegistry;
    private final ArenaBaselineService arenaBaselineService;

    public ArenaEnvironmentProtectionListener(ArenaRegistry arenaRegistry,
                                              ArenaBaselineService arenaBaselineService) {
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.arenaBaselineService = Objects.requireNonNull(arenaBaselineService, "arenaBaselineService");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
        removeProtectedBlocks(event.getLocation().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockExplode(BlockExplodeEvent event) {
        removeProtectedBlocks(event.getBlock().getWorld(), event.blockList());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block burningBlock = event.getBlock();
        if (findProtectedArea(burningBlock.getWorld()).contains(burningBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player ignitingPlayer = event.getPlayer();
        if (ignitingPlayer != null && AdminPermission.isAdmin(ignitingPlayer)) {
            return;
        }
        Block ignitedBlock = event.getBlock();
        if (findProtectedArea(ignitedBlock.getWorld()).contains(ignitedBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        BlockFace movement = event.getDirection();
        ProtectedArea protectedArea = findProtectedArea(piston.getWorld());
        if (protectedArea.contains(piston, movement)) {
            event.setCancelled(true);
            return;
        }
        for (Block movedBlock : event.getBlocks()) {
            if (touchesTheProtectedArea(protectedArea, movedBlock, movement)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        Block piston = event.getBlock();
        ProtectedArea protectedArea = findProtectedArea(piston.getWorld());
        for (Block pulledBlock : event.getBlocks()) {
            BlockFace movement = findDirectionTowardsThePiston(piston, pulledBlock, event.getDirection());
            if (touchesTheProtectedArea(protectedArea, pulledBlock, movement)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block changedBlock = event.getBlock();
        if (findProtectedArea(changedBlock.getWorld()).contains(changedBlock)) {
            event.setCancelled(true);
        }
    }

    static BlockFace findDirectionTowardsThePiston(Block piston, Block pulledBlock, BlockFace pistonAxis) {
        int distanceAlongTheAxis = (pulledBlock.getX() - piston.getX()) * pistonAxis.getModX()
                + (pulledBlock.getY() - piston.getY()) * pistonAxis.getModY()
                + (pulledBlock.getZ() - piston.getZ()) * pistonAxis.getModZ();
        return distanceAlongTheAxis > 0 ? pistonAxis.getOppositeFace() : pistonAxis;
    }

    private static boolean touchesTheProtectedArea(ProtectedArea protectedArea, Block movedBlock,
                                                   BlockFace movement) {
        return protectedArea.contains(movedBlock) || protectedArea.contains(movedBlock, movement);
    }

    private void removeProtectedBlocks(World world, List<Block> destroyedBlocks) {
        ProtectedArea protectedArea = findProtectedArea(world);
        destroyedBlocks.removeIf(protectedArea::contains);
    }

    private ProtectedArea findProtectedArea(World world) {
        Optional<ArenaShape> fightArea = arenaRegistry.getArena().getShape()
                .filter(shape -> world != null && shape.getWorldName().equals(world.getName()));
        int capturedFloorY = fightArea
                .map(shape -> ArenaEnvironmentProtectionRule.findCapturedFloorY(shape,
                        arenaBaselineService.findSnapshot(shape)))
                .orElse(Integer.MIN_VALUE);
        return new ProtectedArea(fightArea, capturedFloorY);
    }

    private record ProtectedArea(Optional<ArenaShape> fightArea, int capturedFloorY) {

        private boolean contains(Block block) {
            return contains(block, BlockFace.SELF);
        }

        private boolean contains(Block block, BlockFace offset) {
            return ArenaEnvironmentProtectionRule.isProtected(fightArea, capturedFloorY,
                    block.getWorld().getName(), block.getX() + offset.getModX(), block.getY() + offset.getModY(),
                    block.getZ() + offset.getModZ());
        }
    }
}
