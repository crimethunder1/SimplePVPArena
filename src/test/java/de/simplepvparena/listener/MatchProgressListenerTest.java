package de.simplepvparena.listener;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchProgressListenerTest {

    @Test
    void hearsEveryRoundStartEveryPointThatDecidesNoMatchAndTheEndAfterTheResultIsRecorded() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        List<String> calls = recordCalls(harness);
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        harness.winRound("Alpha");
        harness.winRound("Alpha");

        assertEquals(List.of("Alpha 0 : 0 Bravo", "Alpha 1 : 0 Bravo", "Alpha 1 : 0 Bravo", "no match"), calls);
    }

    @Test
    void hearsACancelledMatch() {
        AutoMatchHarness harness = new AutoMatchHarness("Alpha", "Bravo");
        List<String> calls = recordCalls(harness);
        harness.switchOn();
        harness.advanceUntilTheAutoMatchRuns();

        harness.matchService().cancelActiveMatch();

        assertEquals(List.of("Alpha 0 : 0 Bravo", "no match"), calls);
    }

    private static List<String> recordCalls(AutoMatchHarness harness) {
        List<String> calls = new ArrayList<>();
        harness.matchService().setMatchProgressListener(() -> calls.add(harness.matchService().getActiveMatch()
                .map(match -> match.getScore().describe())
                .orElse("no match")));
        return calls;
    }
}
