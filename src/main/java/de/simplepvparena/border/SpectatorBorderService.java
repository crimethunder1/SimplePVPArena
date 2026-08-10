package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;
import de.simplepvparena.arena.RegionBoundary;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class SpectatorBorderService {

    public static final String BORDER_KEY_NAME = "spectator_border";

    private static final String ITEM_MARKER_VALUE = "spectator-border";
    private static final int CHUNK_SIZE = 16;

    private final NamespacedKey borderKey;

    public SpectatorBorderService(Plugin plugin) {
        this.borderKey = new NamespacedKey(plugin, BORDER_KEY_NAME);
    }

    public NamespacedKey getBorderKey() {
        return borderKey;
    }

    public ItemStack createBorderItem(BorderItemRequest request) {
        ItemStack itemStack = new ItemStack(request.material(), request.amount());
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text("Spectator border").decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(List.of(Component.text("Blocks everybody who is not fighting")
                .decoration(TextDecoration.ITALIC, false)));
        itemMeta.getPersistentDataContainer().set(borderKey, PersistentDataType.STRING, ITEM_MARKER_VALUE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public boolean isBorderItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        return ITEM_MARKER_VALUE.equals(itemStack.getItemMeta().getPersistentDataContainer()
                .get(borderKey, PersistentDataType.STRING));
    }

    public void markBlock(Block block) {
        Chunk chunk = block.getChunk();
        long encodedPosition = BlockPositionCodec.encode(block.getX(), block.getY(), block.getZ());
        writeMarkedPositions(chunk, MarkedBlockPositions.add(readMarkedPositions(chunk), encodedPosition));
    }

    public void clearBlockMark(Block block) {
        Chunk chunk = block.getChunk();
        long encodedPosition = BlockPositionCodec.encode(block.getX(), block.getY(), block.getZ());
        writeMarkedPositions(chunk, MarkedBlockPositions.remove(readMarkedPositions(chunk), encodedPosition));
    }

    public boolean isBorderBlock(Block block) {
        long encodedPosition = BlockPositionCodec.encode(block.getX(), block.getY(), block.getZ());
        return MarkedBlockPositions.contains(readMarkedPositions(block.getChunk()), encodedPosition);
    }

    public boolean isMovementBlocked(boolean activeFighter, Location destination) {
        if (activeFighter || destination == null || destination.getWorld() == null) {
            return false;
        }
        Chunk chunk = destination.getChunk();
        long[] markedPositions = readMarkedPositions(chunk);
        if (markedPositions.length == 0) {
            return false;
        }
        return SpectatorBorderMovementRule.isMovementBlocked(false, markedPositions,
                destination.getBlockX(), destination.getBlockY(), destination.getBlockZ());
    }

    public int clearMarksInRegion(World world, RegionBoundary region) {
        if (!world.getName().equals(region.getWorldName())) {
            return 0;
        }
        int clearedMarkCount = 0;
        int minimumChunkX = Math.floorDiv(region.getMinimumX(), CHUNK_SIZE);
        int maximumChunkX = Math.floorDiv(region.getMaximumX(), CHUNK_SIZE);
        int minimumChunkZ = Math.floorDiv(region.getMinimumZ(), CHUNK_SIZE);
        int maximumChunkZ = Math.floorDiv(region.getMaximumZ(), CHUNK_SIZE);
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                long[] markedPositions = readMarkedPositions(chunk);
                if (markedPositions.length == 0) {
                    continue;
                }
                long[] remainingPositions = MarkedBlockPositions.removeInsideRegion(markedPositions, region);
                if (remainingPositions.length == markedPositions.length) {
                    continue;
                }
                clearedMarkCount += markedPositions.length - remainingPositions.length;
                writeMarkedPositions(chunk, remainingPositions);
            }
        }
        return clearedMarkCount;
    }

    private long[] readMarkedPositions(Chunk chunk) {
        long[] markedPositions = chunk.getPersistentDataContainer()
                .get(borderKey, PersistentDataType.LONG_ARRAY);
        return markedPositions == null ? MarkedBlockPositions.EMPTY_POSITIONS : markedPositions;
    }

    private void writeMarkedPositions(Chunk chunk, long[] markedPositions) {
        PersistentDataContainer chunkContainer = chunk.getPersistentDataContainer();
        if (markedPositions.length == 0) {
            chunkContainer.remove(borderKey);
            return;
        }
        chunkContainer.set(borderKey, PersistentDataType.LONG_ARRAY, markedPositions);
    }
}
