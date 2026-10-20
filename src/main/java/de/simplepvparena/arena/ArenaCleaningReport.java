package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaCleaningMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;

public record ArenaCleaningReport(long checkedBlockCount, long changedBlockCount, int clearedBorderMarkCount,
                                  MessageKey changeDescription, String failureReason) {

    public ArenaCleaningReport(long checkedBlockCount, long changedBlockCount, int clearedBorderMarkCount,
                               MessageKey changeDescription) {
        this(checkedBlockCount, changedBlockCount, clearedBorderMarkCount, changeDescription, null);
    }

    public static ArenaCleaningReport failed(long checkedBlockCount, long changedBlockCount,
                                             int clearedBorderMarkCount, MessageKey changeDescription,
                                             String failureReason) {
        return new ArenaCleaningReport(checkedBlockCount, changedBlockCount, clearedBorderMarkCount,
                changeDescription, failureReason);
    }

    public boolean isSuccessful() {
        return failureReason == null;
    }

    public String describeOutcome() {
        if (isSuccessful()) {
            return Messages.text(ArenaCleaningMessage.REPORT_CLEAN, Placeholder.of("report", describe()));
        }
        return Messages.text(ArenaCleaningMessage.REPORT_STOPPED, Placeholder.of("reason", failureReason),
                Placeholder.of("report", describe()), Placeholder.of("action", Arena.describeCleanAction()));
    }

    public String describe() {
        Placeholder borderMarks = Placeholder.of("borderMarks",
                Messages.plural(ArenaCleaningMessage.REPORT_BORDER_MARKS_REMOVED, clearedBorderMarkCount));
        if (changedBlockCount == 0L) {
            return Messages.plural(ArenaCleaningMessage.REPORT_UNCHANGED, checkedBlockCount, borderMarks);
        }
        String changedBlocks = Messages.plural(changeDescription, changedBlockCount,
                Placeholder.of("checkedCount", checkedBlockCount));
        return Messages.text(ArenaCleaningMessage.REPORT_CHANGED, Placeholder.of("blocks", changedBlocks),
                borderMarks);
    }
}
