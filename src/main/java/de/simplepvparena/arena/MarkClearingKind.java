package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.MessageKey;

public enum MarkClearingKind {

    BORDER_MARKS(ArenaSetupMessage.BORDER_MARKS_FORGOTTEN, ArenaSetupMessage.BORDER_MARKS_FORGOTTEN_NOT_WRITTEN),
    START_GATE_MARKS(ArenaSetupMessage.START_GATE_MARKS_FORGOTTEN,
            ArenaSetupMessage.START_GATE_MARKS_FORGOTTEN_NOT_WRITTEN);

    private final MessageKey forgottenSentence;
    private final MessageKey forgottenNotWrittenSentence;

    MarkClearingKind(MessageKey forgottenSentence, MessageKey forgottenNotWrittenSentence) {
        this.forgottenSentence = forgottenSentence;
        this.forgottenNotWrittenSentence = forgottenNotWrittenSentence;
    }

    MessageKey getForgottenSentence() {
        return forgottenSentence;
    }

    MessageKey getForgottenNotWrittenSentence() {
        return forgottenNotWrittenSentence;
    }
}
