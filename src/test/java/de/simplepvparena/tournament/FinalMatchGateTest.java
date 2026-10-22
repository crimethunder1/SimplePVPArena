package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalMatchGateTest {

    private final RecordingAdminNotifier adminNotifier = new RecordingAdminNotifier();
    private final FinalMatchGate gate = new FinalMatchGate(adminNotifier);
    private final Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

    @Test
    void staysPendingWhileAFinalistIsStillUnknown() {
        synchronizeGate();
        assertEquals(FinalMatchStage.PENDING, gate.getStage());

        bracket.recordWinner("round1-match1", "alpha");
        synchronizeGate();

        assertEquals(FinalMatchStage.PENDING, gate.getStage());
        assertEquals(List.of(), adminNotifier.messages);
    }

    @Test
    void becomesReadyWhenBothFinalistsAreDetermined() {
        determineBothFinalists();
        synchronizeGate();

        assertEquals(FinalMatchStage.READY, gate.getStage());
        assertEquals(List.of("The final alpha against bravo is ready, "
                + "an admin starts it with \"Start the final\" in /tournament"), adminNotifier.messages);
    }

    @Test
    void notifiesTheAdminsOnlyOnceWhileTheFinalStaysReady() {
        determineBothFinalists();
        synchronizeGate();
        synchronizeGate();
        synchronizeGate();

        assertEquals(1, adminNotifier.messages.size());
    }

    @Test
    void rejectsTheStartWhileTheFinalIsNotReady() {
        synchronizeGate();

        assertEquals("The final is not ready yet, both finalists have to be determined first",
                gate.findStartRejection().orElseThrow());
        assertThrows(IllegalStateException.class, gate::markRunning);
    }

    @Test
    void allowsTheStartExactlyOnceAndRejectsARunningFinal() {
        determineBothFinalists();
        synchronizeGate();
        assertTrue(gate.findStartRejection().isEmpty());

        gate.markRunning();

        assertEquals(FinalMatchStage.RUNNING, gate.getStage());
        assertEquals("The final is already running", gate.findStartRejection().orElseThrow());
        assertThrows(IllegalStateException.class, gate::markRunning);
    }

    @Test
    void keepsTheRunningStageWhileTheFinalIsUndecided() {
        determineBothFinalists();
        synchronizeGate();
        gate.markRunning();
        synchronizeGate();

        assertEquals(FinalMatchStage.RUNNING, gate.getStage());
        assertEquals(1, adminNotifier.messages.size());
    }

    @Test
    void rejectsTheStartOfADecidedFinal() {
        determineBothFinalists();
        synchronizeGate();
        gate.markRunning();
        bracket.recordWinner("round2-match1", "alpha");
        synchronizeGate();

        assertEquals(FinalMatchStage.FINISHED, gate.getStage());
        assertEquals("The final is already decided", gate.findStartRejection().orElseThrow());
    }

    @Test
    void announcesAgainForTheFinalOfANewTournament() {
        determineBothFinalists();
        synchronizeGate();
        gate.reset();

        assertEquals(FinalMatchStage.PENDING, gate.getStage());

        synchronizeGate();

        assertEquals(FinalMatchStage.READY, gate.getStage());
        assertEquals(2, adminNotifier.messages.size());
    }

    @Test
    void offersTheStartAgainWhenARunningFinalReturnsToReadyWithoutANewNotice() {
        determineBothFinalists();
        synchronizeGate();
        gate.markRunning();

        gate.returnToReady();
        synchronizeGate();

        assertEquals(FinalMatchStage.READY, gate.getStage());
        assertTrue(gate.findStartRejection().isEmpty());
        assertEquals(1, adminNotifier.messages.size());
    }

    @Test
    void refusesToReturnToReadyWhileTheFinalIsNotRunning() {
        determineBothFinalists();
        synchronizeGate();

        assertThrows(IllegalStateException.class, gate::returnToReady);
        assertEquals(FinalMatchStage.READY, gate.getStage());
    }

    private void determineBothFinalists() {
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "bravo");
    }

    private void synchronizeGate() {
        gate.synchronizeWith(bracket.getFinalMatch());
    }

    private static final class RecordingAdminNotifier implements AdminNotifier {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void notifyAdmins(String message) {
            messages.add(message);
        }
    }
}
