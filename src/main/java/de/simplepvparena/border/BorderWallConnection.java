package de.simplepvparena.border;

import de.simplepvparena.arena.BlockPositionCodec;

public record BorderWallConnection(long firstPosition, long secondPosition) {

    public static BorderWallConnection between(long onePosition, long otherPosition) {
        return onePosition <= otherPosition
                ? new BorderWallConnection(onePosition, otherPosition)
                : new BorderWallConnection(otherPosition, onePosition);
    }

    public String describe() {
        return BlockPositionCodec.describe(firstPosition) + " - " + BlockPositionCodec.describe(secondPosition);
    }
}
