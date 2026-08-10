package de.simplepvparena.arena;

import java.util.Objects;

public final class RegionBlockCursor {

    private final RegionBoundary region;
    private final int widthX;
    private final int heightY;
    private final int depthZ;
    private final long totalBlockCount;

    private long visitedBlockCount;

    public RegionBlockCursor(RegionBoundary region) {
        this.region = Objects.requireNonNull(region, "region");
        this.widthX = region.getMaximumX() - region.getMinimumX() + 1;
        this.heightY = region.getMaximumY() - region.getMinimumY() + 1;
        this.depthZ = region.getMaximumZ() - region.getMinimumZ() + 1;
        this.totalBlockCount = (long) widthX * heightY * depthZ;
    }

    public RegionBoundary getRegion() {
        return region;
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
            batch[batchIndex] = encodePosition(visitedBlockCount + batchIndex);
        }
        visitedBlockCount += batchLength;
        return batch;
    }

    private long encodePosition(long blockIndex) {
        long horizontalLayerSize = (long) widthX * depthZ;
        int offsetY = (int) (blockIndex / horizontalLayerSize);
        long remainderInLayer = blockIndex % horizontalLayerSize;
        int offsetZ = (int) (remainderInLayer / widthX);
        int offsetX = (int) (remainderInLayer % widthX);
        return BlockPositionCodec.encode(region.getMinimumX() + offsetX,
                region.getMinimumY() + offsetY,
                region.getMinimumZ() + offsetZ);
    }
}
