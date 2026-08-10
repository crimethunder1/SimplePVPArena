package de.simplepvparena.tournament;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalMatchGateTest {

    private final RecordingOperatorNotifier operatorNotifier = new RecordingOperatorNotifier();
    private final FinalMatchGate gate = new FinalMatchGate(operatorNotifier);
    private final Bracket bracket = Bracket.createSingleElimination(List.of("alpha", "bravo", "charlie", "delta"));

    @Test
    void staysPendingWhileAFinalistIsStillUnknown() {
        synchronizeGate();
        assertEquals(FinalMatchStage.PENDING, gate.getStage());

        bracket.recordWinner("round1-match1", "alpha");
        synchronizeGate();

        assertEquals(FinalMatchStage.PENDING, gate.getStage());
        assertEquals(List.of(), operatorNotifier.messages);
    }

    @Test
    void becomesReadyWhenBothFinalistsAreDetermined() {
        determineBothFinalists();
        synchronizeGate();

        assertEquals(FinalMatchStage.READY, gate.getStage());
        assertEquals(List.of("The final alpha against bravo is ready, "
                + "an admin starts it with \"Start the final\" in /tournament"), operatorNotifier.messages);
    }

    @Test
    void notifiesTheOperatorsOnlyOnceWhileTheFinalStaysReady() {
        determineBothFinalists();
        synchronizeGate();
        synchronizeGate();
        synchronizeGate();

        assertEquals(1, operatorNotifier.messages.size());
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
        assertEquals(1, operatorNotifier.messages.size());
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
        assertEquals(2, operatorNotifier.messages.size());
    }

    private void determineBothFinalists() {
        bracket.recordWinner("round1-match1", "alpha");
        bracket.recordWinner("round1-match2", "bravo");
    }

    private void synchronizeGate() {
        gate.synchronizeWith(bracket.getFinalMatch());
    }

    private static final class RecordingOperatorNotifier implements OperatorNotifier {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void notifyOperators(String message) {
            messages.add(message);
        }
    }
}
