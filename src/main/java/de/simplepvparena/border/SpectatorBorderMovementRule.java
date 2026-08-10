package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;

public final class SpectatorBorderMovementRule {

    private SpectatorBorderMovementRule() {
    }

    public static boolean isMovementBlocked(boolean activeFighter, long[] markedPositions,
                                            int destinationBlockX, int destinationBlockY, int destinationBlockZ) {
        if (activeFighter) {
            return false;
        }
        for (long markedPosition : markedPositions) {
            if (BlockPositionCodec.decodeX(markedPosition) != destinationBlockX
                    || BlockPositionCodec.decodeZ(markedPosition) != destinationBlockZ) {
                continue;
            }
            if (BlockPositionCodec.decodeY(markedPosition) <= destinationBlockY) {
                return true;
            }
        }
        return false;
    }
}
