package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.MessageKey;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.jspecify.annotations.NullMarked;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The blocks above the wall top of the fight area, up to the top of the world: the fighters may build in the columns
 * of the fight area at any height. The baseline only covers the fight area up to the wall top, so towers the fighters
 * built higher stayed after a cleaning. The headroom keeps the few blocks that belonged there when the baseline was
 * taken, every other block in it is cleaned to air. Only the columns that hold a block above the wall top are walked,
 * so a high headroom costs nothing while it is empty.
 */
@NullMarked
public final class ArenaHeadroom implements ArenaCleaningTarget {

    /** Used when no headroom was recorded, for a baseline of an older version or a cleaning without baseline. */
    public static final ArenaHeadroom ASSUMED_EMPTY = new ArenaHeadroom(Map.of(), Map.of(), false);

    private final Map<Long, BlockData> blocks;
    private final Map<Long, BlockState> blockEntities;
    private final boolean recorded;

    private ArenaHeadroom(Map<Long, BlockData> blocks, Map<Long, BlockState> blockEntities, boolean recorded) {
        this.blocks = Map.copyOf(blocks);
        this.blockEntities = Map.copyOf(blockEntities);
        this.recorded = recorded;
    }

    /** The volume above the wall top up to the top of the world, empty when the world ends below it. */
    public static Optional<ArenaShape> volumeAbove(ArenaShape fightArea, int worldMaximumY) {
        int lowestY = fightArea.getWallTopY() + 1;
        if (worldMaximumY < lowestY) {
            return Optional.empty();
        }
        return Optional.of(fightArea.withVerticalExtent(lowestY, worldMaximumY));
    }

    /** Highest recorded block, or the given value when nothing was recorded. */
    public int findHighestRecordedY(int fallbackY) {
        int highestY = fallbackY;
        for (long position : blocks.keySet()) {
            highestY = Math.max(highestY, BlockPositionCodec.decodeY(position));
        }
        return highestY;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** True when the headroom was read together with the baseline, false when it is only assumed to be empty. */
    public boolean isRecorded() {
        return recorded;
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public Map<Long, BlockData> getBlocks() {
        return blocks;
    }

    @Override
    public Material findMaterial(int blockX, int blockY, int blockZ) {
        return findBlockData(blockX, blockY, blockZ).map(BlockData::getMaterial).orElse(Material.AIR);
    }

    @Override
    public Optional<BlockData> findBlockData(int blockX, int blockY, int blockZ) {
        return Optional.ofNullable(blocks.get(BlockPositionCodec.encode(blockX, blockY, blockZ)));
    }

    @Override
    public Optional<BlockState> findBlockEntity(int blockX, int blockY, int blockZ) {
        return Optional.ofNullable(blockEntities.get(BlockPositionCodec.encode(blockX, blockY, blockZ)));
    }

    @Override
    public MessageKey describeChange() {
        return ArenaCleaningMessage.REPORT_RESTORED;
    }

    @NullMarked
    public static final class Builder {

        private final Map<Long, BlockData> blocks = new HashMap<>();
        private final Map<Long, BlockState> blockEntities = new HashMap<>();

        private Builder() {
        }

        /** Keeps the block unless it is air, block entities keep their contents as well. */
        public Builder record(int blockX, int blockY, int blockZ, BlockData blockData,
                              Supplier<BlockState> blockEntityReader) {
            Objects.requireNonNull(blockData, "blockData");
            Objects.requireNonNull(blockEntityReader, "blockEntityReader");
            long position = BlockPositionCodec.encode(blockX, blockY, blockZ);
            if (blockData.getMaterial().isAir()) {
                blocks.remove(position);
                blockEntities.remove(position);
                return this;
            }
            blocks.put(position, blockData);
            if (blockData.createBlockState() instanceof TileState) {
                blockEntities.put(position, Objects.requireNonNull(blockEntityReader.get(), "blockEntity"));
            } else {
                blockEntities.remove(position);
            }
            return this;
        }

        public ArenaHeadroom build() {
            return new ArenaHeadroom(blocks, blockEntities, true);
        }
    }
}
