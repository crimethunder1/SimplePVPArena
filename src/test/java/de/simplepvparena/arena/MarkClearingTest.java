package de.simplepvparena.arena;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkClearingTest {

    private static final String RUNNING_MATCH = "A match is running, the arena can only be reset between matches";
    private static final String NOTHING_MARKED = "Nothing is marked";

    private final List<Component> sentMessages = new ArrayList<>();
    private final List<String> loggedWarnings = new ArrayList<>();
    private final CommandSender admin = SharedBukkitServer.proxy(CommandSender.class, (method, arguments) -> {
        if (method.getName().equals("sendMessage") && arguments[0] instanceof Component message) {
            sentMessages.add(message);
            return null;
        }
        throw new UnsupportedOperationException("CommandSender." + method.getName());
    });

    private int markCount;
    private int forgettingCount;
    private Optional<String> arenaChangeRejection = Optional.empty();
    private IOException writeFailure;

    @Test
    void countsTheMarksItWouldForget() {
        markCount = 96;

        assertEquals(96, clearing(MarkClearingKind.BORDER_MARKS).countMarks());
    }

    @Test
    void forgetsTheBorderMarksAndNamesTheirNumber() {
        markCount = 96;

        clearing(MarkClearingKind.BORDER_MARKS).clearEveryMark(admin);

        assertEquals(1, forgettingCount);
        assertEquals(0, markCount);
        assertEquals(List.of(TournamentMessages.success("96 border marks were forgotten")), sentMessages);
        assertEquals(List.of(), loggedWarnings);
    }

    @Test
    void namesASingleForgottenMarkInTheSingular() {
        markCount = 1;
        clearing(MarkClearingKind.BORDER_MARKS).clearEveryMark(admin);
        markCount = 1;
        clearing(MarkClearingKind.START_GATE_MARKS).clearEveryMark(admin);

        assertEquals(List.of(TournamentMessages.success("1 border mark was forgotten"),
                TournamentMessages.success("1 start gate mark was forgotten")), sentMessages);
    }

    @Test
    void forgetsTheStartGateMarksAndNamesTheirNumber() {
        markCount = 12;

        clearing(MarkClearingKind.START_GATE_MARKS).clearEveryMark(admin);

        assertEquals(1, forgettingCount);
        assertEquals(List.of(TournamentMessages.success("12 start gate marks were forgotten")), sentMessages);
    }

    @Test
    void namesTheNumberOfTheMomentOfTheClearingAndNotTheOneTheMenuShowed() {
        markCount = 96;
        MarkClearing clearing = clearing(MarkClearingKind.BORDER_MARKS);
        int shownMarkCount = clearing.countMarks();
        markCount = 40;

        clearing.clearEveryMark(admin);

        assertEquals(96, shownMarkCount);
        assertEquals(List.of(TournamentMessages.success("40 border marks were forgotten")), sentMessages);
    }

    @Test
    void refusesToClearWhenNothingIsMarked() {
        MarkClearing clearing = clearing(MarkClearingKind.START_GATE_MARKS);

        assertEquals(Optional.of(NOTHING_MARKED), clearing.findRejection());
        clearing.clearEveryMark(admin);

        assertEquals(0, forgettingCount);
        assertEquals(List.of(TournamentMessages.error(NOTHING_MARKED)), sentMessages);
    }

    @Test
    void refusesToClearWhileTheArenaMustNotChange() {
        markCount = 96;
        arenaChangeRejection = Optional.of(RUNNING_MATCH);
        MarkClearing clearing = clearing(MarkClearingKind.BORDER_MARKS);

        assertEquals(Optional.of(RUNNING_MATCH), clearing.findRejection());
        clearing.clearEveryMark(admin);

        assertEquals(0, forgettingCount);
        assertEquals(96, markCount);
        assertEquals(List.of(TournamentMessages.error(RUNNING_MATCH)), sentMessages);
    }

    @Test
    void namesTheMissingMarksBeforeTheLockedArena() {
        arenaChangeRejection = Optional.of(RUNNING_MATCH);

        assertEquals(Optional.of(NOTHING_MARKED), clearing(MarkClearingKind.BORDER_MARKS).findRejection());
    }

    @Test
    void findsNoRejectionWhenMarksExistAndTheArenaMayChange() {
        markCount = 3;

        assertEquals(Optional.empty(), clearing(MarkClearingKind.BORDER_MARKS).findRejection());
    }

    @Test
    void warnsTheAdminAndTheLogWhenTheBorderFileCannotBeWritten() {
        markCount = 96;
        writeFailure = new IOException("disk full");

        clearing(MarkClearingKind.BORDER_MARKS).clearEveryMark(admin);

        String warning = "96 border marks were forgotten, but borders.yml could not be written: disk full";
        assertEquals(0, markCount, "the marks stay forgotten in memory");
        assertEquals(List.of(TournamentMessages.warning(warning)), sentMessages);
        assertEquals(List.of(warning), loggedWarnings);
    }

    @Test
    void warnsTheAdminAndTheLogWhenTheStartGateFileCannotBeWritten() {
        markCount = 1;
        writeFailure = new IOException();

        clearing(MarkClearingKind.START_GATE_MARKS).clearEveryMark(admin);

        String warning = "1 start gate mark was forgotten, but startgates.yml could not be written: IOException";
        assertEquals(List.of(TournamentMessages.warning(warning)), sentMessages);
        assertEquals(List.of(warning), loggedWarnings);
        assertEquals(0, markCount);
    }

    private MarkClearing clearing(MarkClearingKind kind) {
        return new MarkClearing(kind, () -> markCount, this::forgetEveryMark, () -> arenaChangeRejection,
                loggedWarnings::add);
    }

    private void forgetEveryMark() throws IOException {
        forgettingCount++;
        markCount = 0;
        if (writeFailure != null) {
            throw writeFailure;
        }
    }
}
