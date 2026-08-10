package de.simplepvparena.border;

public final class SpectatorBorderMovementRule {

    private SpectatorBorderMovementRule() {
    }

    public static BorderMovementDecision decide(boolean activeFighter, boolean operator,
                                                BlockedColumnLookup blockedColumns,
                                                int originBlockX, int originBlockZ,
                                                int destinationBlockX, int destinationBlockZ) {
        if (!isMovementBlocked(blockedColumns, originBlockX, originBlockZ, destinationBlockX, destinationBlockZ)) {
            return BorderMovementDecision.ALLOWED;
        }
        if (activeFighter) {
            return BorderMovementDecision.ALLOWED;
        }
        if (!operator) {
            return BorderMovementDecision.BLOCKED;
        }
        return blockedColumns.isBlockedColumn(originBlockX, originBlockZ)
                ? BorderMovementDecision.ALLOWED
                : BorderMovementDecision.OPERATOR_PASS;
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
