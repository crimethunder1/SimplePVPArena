package de.simplepvparena.border;

public final class SpectatorBorderMovementRule {

    private SpectatorBorderMovementRule() {
    }

    public static boolean isMovementAllowed(boolean activeFighter, boolean arenaFlightSpectator, boolean operator,
                                            BlockedColumnLookup blockedColumns,
                                            int originBlockX, int originBlockZ,
                                            int destinationBlockX, int destinationBlockZ) {
        if (activeFighter || arenaFlightSpectator || operator) {
            return true;
        }
        return !isMovementBlocked(blockedColumns, originBlockX, originBlockZ,
                destinationBlockX, destinationBlockZ);
    }

    public static boolean isMovementBlocked(BlockedColumnLookup blockedColumns,
                                            int originBlockX, int originBlockZ,
                                            int destinationBlockX, int destinationBlockZ) {
        if (originBlockX == destinationBlockX && originBlockZ == destinationBlockZ) {
            return false;
        }
        if (blockedColumns.isBlockedColumn(destinationBlockX, destinationBlockZ)) {
            return true;
        }
        return cutsADiagonalCorner(blockedColumns, originBlockX, originBlockZ, destinationBlockX, destinationBlockZ);
    }

    private static boolean cutsADiagonalCorner(BlockedColumnLookup blockedColumns,
                                               int originBlockX, int originBlockZ,
                                               int destinationBlockX, int destinationBlockZ) {
        if (originBlockX == destinationBlockX || originBlockZ == destinationBlockZ) {
            return false;
        }
        return blockedColumns.isBlockedColumn(destinationBlockX, originBlockZ)
                && blockedColumns.isBlockedColumn(originBlockX, destinationBlockZ);
    }
}
