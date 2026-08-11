package de.simplepvparena.arena;

import java.util.Objects;

public final class ArenaShapeBlockCursor {

    private final ArenaShape shape;
    private final long totalBlockCount;

    private long visitedBlockCount;

    public ArenaShapeBlockCursor(ArenaShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.totalBlockCount = shape.getBlockCount();
    }

    public ArenaShape getShape() {
        return shape;
    }

    public long getTotalBlockCount() {
        return totalBlockCount;
    }

    public long getVisitedBlockCount() {
        return visitedBlockCount;
    }

    public long getRemainingBlockCount() {
        return totalBlockCount - visitedBlockCount;
    }

    public boolean hasRemainingBlocks() {
        return visitedBlockCount < totalBlockCount;
    }

    public long[] nextBatch(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least one");
        }
        int batchLength = (int) Math.min(batchSize, getRemainingBlockCount());
        long[] batch = new long[batchLength];
        for (int batchIndex = 0; batchIndex < batchLength; batchIndex++) {
            batch[batchIndex] = shape.encodeBlockAtIndex(visitedBlockCount + batchIndex);
        }
        visitedBlockCount += batchLength;
        return batch;
    }
}
