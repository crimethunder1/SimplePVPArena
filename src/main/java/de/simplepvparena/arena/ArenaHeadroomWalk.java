package de.simplepvparena.arena;

import org.bukkit.HeightMap;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Objects;

/**
 * Walks the blocks above the wall top column by column. A column is only walked up to its highest block, read from
 * the height map of the world, or up to the highest block recorded with the baseline. Columns with nothing above the
 * wall top cost a single look-up, so the headroom reaches the top of the world without slowing the cleaning down.
 */
@NullMarked
public final class ArenaHeadroomWalk {

    private final ArenaShape fightArea;
    private final int lowestY;
    private final int worldTopY;
    private final int recordedTopY;

    private int cellIndex;
    private boolean columnOpen;
    private int columnX;
    private int columnZ;
    private int nextY;
    private int columnTopY;
    private long walkedBlockCount;

    public ArenaHeadroomWalk(ArenaShape fightArea, int worldTopY, int recordedTopY) {
        this.fightArea = Objects.requireNonNull(fightArea, "fightArea");
        this.lowestY = fightArea.getWallTopY() + 1;
        this.worldTopY = worldTopY;
        this.recordedTopY = recordedTopY;
    }

    public boolean hasRemainingBlocks() {
        return columnOpen || cellIndex < fightArea.getCellCount();
    }

    public long getWalkedBlockCount() {
        return walkedBlockCount;
    }

    /**
     * Next positions to look at. Every height map look-up counts as one unit of the budget as well, so a tick never
     * does more work than the budget allows.
     */
    public long[] nextBatch(World world, int budget) {
        long[] batch = new long[Math.max(1, budget)];
        int count = 0;
        int work = 0;
        while (work < budget) {
            if (!columnOpen) {
                if (cellIndex >= fightArea.getCellCount()) {
                    break;
                }
                long cell = fightArea.getCell(cellIndex++);
                columnX = HorizontalCellCodec.decodeX(cell);
                columnZ = HorizontalCellCodec.decodeZ(cell);
                int highestBlockY = world.getHighestBlockYAt(columnX, columnZ, HeightMap.WORLD_SURFACE);
                columnTopY = Math.min(worldTopY, Math.max(highestBlockY, recordedTopY));
                nextY = lowestY;
                columnOpen = nextY <= columnTopY;
                work++;
                continue;
            }
            batch[count++] = BlockPositionCodec.encode(columnX, nextY, columnZ);
            nextY++;
            columnOpen = nextY <= columnTopY;
            work++;
        }
        walkedBlockCount += count;
        return count == batch.length ? batch : Arrays.copyOf(batch, count);
    }
}
