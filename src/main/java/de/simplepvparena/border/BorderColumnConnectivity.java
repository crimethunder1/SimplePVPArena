package de.simplepvparena.border;

public final class BorderColumnConnectivity {

    private static final int[] COLUMN_OFFSETS_X = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] COLUMN_OFFSETS_Z = {0, 0, 1, -1, 1, -1, 1, -1};

    private BorderColumnConnectivity() {
    }

    public static BorderConnectivity of(BlockedColumnLookup blockedColumns, int blockX, int blockZ) {
        return BorderConnectivity.ofConnectedNeighbourCount(
                countConnectedNeighbours(blockedColumns, blockX, blockZ));
    }

    public static int countConnectedNeighbours(BlockedColumnLookup blockedColumns, int blockX, int blockZ) {
        int connectedNeighbourCount = 0;
        for (int offsetIndex = 0; offsetIndex < COLUMN_OFFSETS_X.length; offsetIndex++) {
            if (blockedColumns.isBlockedColumn(blockX + COLUMN_OFFSETS_X[offsetIndex],
                    blockZ + COLUMN_OFFSETS_Z[offsetIndex])) {
                connectedNeighbourCount++;
            }
        }
        return connectedNeighbourCount;
    }
}
