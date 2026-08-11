package de.simplepvparena.arena;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Objects;

public final class ArenaBaselineSnapshotBuilder {

    private static final short NOTHING_RECORDED = -1;

    private final ArenaShape shape;
    private final short[] materialOrdinals;

    private long recordedBlockCount;

    public ArenaBaselineSnapshotBuilder(ArenaShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
        long blockCount = shape.getBlockCount();
        if (blockCount > ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT) {
            throw new IllegalArgumentException("the fight area holds " + blockCount
                    + " blocks, a baseline is taken of at most " + ArenaBaselineSnapshot.MAXIMUM_BLOCK_COUNT);
        }
        this.materialOrdinals = new short[(int) blockCount];
        Arrays.fill(materialOrdinals, NOTHING_RECORDED);
    }

    public ArenaShape getShape() {
        return shape;
    }

    public long getRecordedBlockCount() {
        return recordedBlockCount;
    }

    public long getTotalBlockCount() {
        return materialOrdinals.length;
    }

    public boolean isComplete() {
        return recordedBlockCount == materialOrdinals.length;
    }

    public void record(long encodedPosition, Material material) {
        record(BlockPositionCodec.decodeX(encodedPosition), BlockPositionCodec.decodeY(encodedPosition),
                BlockPositionCodec.decodeZ(encodedPosition), material);
    }

    public void record(int blockX, int blockY, int blockZ, Material material) {
        Objects.requireNonNull(material, "material");
        long blockIndex = shape.findBlockIndex(blockX, blockY, blockZ);
        if (blockIndex < 0L) {
            throw new IllegalArgumentException("the block " + blockX + "/" + blockY + "/" + blockZ
                    + " does not belong to the fight area the baseline is taken of");
        }
        if (materialOrdinals[(int) blockIndex] == NOTHING_RECORDED) {
            recordedBlockCount++;
        }
        materialOrdinals[(int) blockIndex] = ArenaBaselineSnapshot.toOrdinal(material);
    }

    public ArenaBaselineSnapshot build() {
        if (!isComplete()) {
            throw new IllegalStateException("only " + recordedBlockCount + " of " + materialOrdinals.length
                    + " blocks of the fight area were read, the baseline is not complete");
        }
        return new ArenaBaselineSnapshot(shape, materialOrdinals.clone());
    }
}
