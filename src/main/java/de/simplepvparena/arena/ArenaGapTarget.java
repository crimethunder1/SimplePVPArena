package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaGapMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

import java.util.Objects;

public record ArenaGapTarget(String worldName, int blockX, int blockY, int blockZ, Reason reason) {

    public enum Reason {

        FILL_FRONT(ArenaGapMessage.FILL_FRONT_LORE, ArenaGapMessage.FILL_FRONT_HAND_OUT_HINT,
                ArenaGapMessage.FILL_FRONT_ARRIVAL_HINT),

        BORDER_TOO_HIGH(ArenaGapMessage.BORDER_TOO_HIGH_LORE, ArenaGapMessage.BORDER_TOO_HIGH_HAND_OUT_HINT,
                ArenaGapMessage.BORDER_TOO_HIGH_ARRIVAL_HINT);

        private final MessageKey loreLine;
        private final MessageKey handOutHint;
        private final MessageKey arrivalHint;

        Reason(MessageKey loreLine, MessageKey handOutHint, MessageKey arrivalHint) {
            this.loreLine = loreLine;
            this.handOutHint = handOutHint;
            this.arrivalHint = arrivalHint;
        }

        public String getLoreLine() {
            return Messages.text(loreLine);
        }

        public String getHandOutHint() {
            return Messages.text(handOutHint);
        }

        public String getArrivalHint() {
            return Messages.text(arrivalHint);
        }
    }

    public ArenaGapTarget {
        Objects.requireNonNull(worldName, "worldName");
        Objects.requireNonNull(reason, "reason");
    }

    public ArenaGapTarget(String worldName, int blockX, int blockY, int blockZ) {
        this(worldName, blockX, blockY, blockZ, Reason.FILL_FRONT);
    }

    public static ArenaGapTarget fromDiagnostics(String worldName, int seedY, ArenaFillDiagnostics diagnostics) {
        return diagnostics.getNearestHeightMismatchColumn()
                .map(column -> new ArenaGapTarget(worldName, column.blockX(), seedY, column.blockZ(),
                        Reason.BORDER_TOO_HIGH))
                .orElseGet(() -> new ArenaGapTarget(worldName, diagnostics.getNearestFrontX(), seedY,
                        diagnostics.getNearestFrontZ(), Reason.FILL_FRONT));
    }

    public String describe() {
        return Messages.text(ArenaGapMessage.TARGET, Placeholder.of("blockX", blockX),
                Placeholder.of("blockY", blockY), Placeholder.of("blockZ", blockZ), Placeholder.of("world", worldName));
    }
}
