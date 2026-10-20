package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ArenaBaselineSnapshotBuilder {

    public static final int MAXIMUM_PALETTE_SIZE = 65_535;

    private static final short NOTHING_RECORDED = -1;

    private final ArenaShape shape;
    private final ArenaShape capturedVolume;
    private final int depthBelowFloor;
    private final short[] paletteIndexes;
    private final List<BlockData> palette = new ArrayList<>();
    private final Map<BlockData, Integer> paletteIndexByBlockData = new HashMap<>();
    private final BitSet blockEntityPaletteIndexes = new BitSet();
    private final Map<Integer, BlockState> blockEntitiesByBlockIndex = new HashMap<>();

    private long recordedBlockCount;

    public ArenaBaselineSnapshotBuilder(ArenaShape shape) {
        this(shape, 0);
    }

    public ArenaBaselineSnapshotBuilder(ArenaShape shape, int depthBelowFloor) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.depthBelowFloor = depthBelowFloor;
        this.capturedVolume = shape.withFloorLoweredBy(depthBelowFloor);
        long blockCount = capturedVolume.getBlockCount();
        if (blockCount > ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT) {
            throw new IllegalArgumentException("the fight area holds " + blockCount
                    + " blocks, a baseline is taken of at most " + ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT);
        }
        this.paletteIndexes = new short[(int) blockCount];
        Arrays.fill(paletteIndexes, NOTHING_RECORDED);
    }

    public ArenaShape getShape() {
        return shape;
    }

    public ArenaShape getCapturedVolume() {
        return capturedVolume;
    }

    public long getRecordedBlockCount() {
        return recordedBlockCount;
    }

    public long getTotalBlockCount() {
        return paletteIndexes.length;
    }

    public int getPaletteSize() {
        return palette.size();
    }

    public boolean isComplete() {
        return recordedBlockCount == paletteIndexes.length;
    }

    public void record(long encodedPosition, BlockData blockData, Supplier<BlockState> blockEntityReader) {
        record(BlockPositionCodec.decodeX(encodedPosition), BlockPositionCodec.decodeY(encodedPosition),
                BlockPositionCodec.decodeZ(encodedPosition), blockData, blockEntityReader);
    }

    public void record(int blockX, int blockY, int blockZ, BlockData blockData,
                       Supplier<BlockState> blockEntityReader) {
        Objects.requireNonNull(blockData, "blockData");
        Objects.requireNonNull(blockEntityReader, "blockEntityReader");
        long blockIndex = capturedVolume.findBlockIndex(blockX, blockY, blockZ);
        if (blockIndex < 0L) {
            throw new IllegalArgumentException("the block " + blockX + "/" + blockY + "/" + blockZ
                    + " does not belong to the fight area the baseline is taken of");
        }
        int paletteIndex = findOrAddPaletteIndex(blockData);
        if (paletteIndexes[(int) blockIndex] == NOTHING_RECORDED) {
            recordedBlockCount++;
        }
        paletteIndexes[(int) blockIndex] = (short) paletteIndex;
        if (blockEntityPaletteIndexes.get(paletteIndex)) {
            blockEntitiesByBlockIndex.put((int) blockIndex,
                    Objects.requireNonNull(blockEntityReader.get(), "blockEntity"));
        } else {
            blockEntitiesByBlockIndex.remove((int) blockIndex);
        }
    }

    private int findOrAddPaletteIndex(BlockData blockData) {
        Integer knownPaletteIndex = paletteIndexByBlockData.get(blockData);
        if (knownPaletteIndex != null) {
            return knownPaletteIndex;
        }
        if (palette.size() >= MAXIMUM_PALETTE_SIZE) {
            throw new IllegalStateException(Messages.text(ArenaCleaningMessage.BASELINE_TOO_MANY_BLOCK_STATES,
                    Placeholder.of("maximum", MAXIMUM_PALETTE_SIZE)));
        }
        int paletteIndex = palette.size();
        palette.add(blockData);
        paletteIndexByBlockData.put(blockData, paletteIndex);
        if (blockData.createBlockState() instanceof TileState) {
            blockEntityPaletteIndexes.set(paletteIndex);
        }
        return paletteIndex;
    }

    public ArenaBaselineSnapshot build() {
        if (!isComplete()) {
            throw new IllegalStateException("only " + recordedBlockCount + " of " + paletteIndexes.length
                    + " blocks of the fight area were read, the baseline is not complete");
        }
        return new ArenaBaselineSnapshot(shape, depthBelowFloor, paletteIndexes.clone(),
                palette.toArray(new BlockData[0]), (BitSet) blockEntityPaletteIndexes.clone(),
                Map.copyOf(blockEntitiesByBlockIndex));
    }
}
