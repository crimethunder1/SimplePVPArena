package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

public record ArenaHeightMismatchColumn(int blockX, int blockZ, int lowestBorderY, int seedY) {

    public ArenaHeightMismatchColumn {
        if (lowestBorderY <= seedY) {
            throw new IllegalArgumentException(
                    "a column whose lowest border block is at or below the seed height bars the fill");
        }
    }

    public int heightAboveSeed() {
        return lowestBorderY - seedY;
    }

    public String describe() {
        return Messages.text(ArenaGapMessage.HEIGHT_MISMATCH_COLUMN, Placeholder.of("blockX", blockX),
                Placeholder.of("blockZ", blockZ), Placeholder.of("lowestBorderY", lowestBorderY),
                Placeholder.of("heightAboveSeed", heightAboveSeed()));
    }
}
