package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.FailureDescriptions;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class MarkClearing {

    private final MarkClearingKind kind;
    private final IntSupplier markCount;
    private final MarkForgetting markForgetting;
    private final Supplier<Optional<String>> arenaChangeRejection;
    private final Consumer<String> warningLogger;

    public MarkClearing(MarkClearingKind kind, IntSupplier markCount, MarkForgetting markForgetting,
                        Supplier<Optional<String>> arenaChangeRejection, Consumer<String> warningLogger) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.markCount = Objects.requireNonNull(markCount, "markCount");
        this.markForgetting = Objects.requireNonNull(markForgetting, "markForgetting");
        this.arenaChangeRejection = Objects.requireNonNull(arenaChangeRejection, "arenaChangeRejection");
        this.warningLogger = Objects.requireNonNull(warningLogger, "warningLogger");
    }

    public int countMarks() {
        return markCount.getAsInt();
    }

    public Optional<String> findRejection() {
        if (countMarks() == 0) {
            return Optional.of(Messages.text(ArenaSetupMessage.NOTHING_MARKED));
        }
        return arenaChangeRejection.get();
    }

    public void clearEveryMark(CommandSender sender) {
        Optional<String> rejection = findRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(sender, rejection.get());
            return;
        }
        int forgottenMarkCount = countMarks();
        try {
            markForgetting.forgetEveryMark();
        } catch (IOException failure) {
            String warning = Messages.plural(kind.getForgottenNotWrittenSentence(), forgottenMarkCount,
                    Placeholder.of("reason", FailureDescriptions.describe(failure)));
            warningLogger.accept(warning);
            TournamentMessages.sendWarning(sender, warning);
            return;
        }
        TournamentMessages.sendSuccess(sender, Messages.plural(kind.getForgottenSentence(), forgottenMarkCount));
    }
}
