package de.simplepvparena.listener;

import de.simplepvparena.arena.SpectatorMode;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchReleaseFailureTest {

    private ArenaMatchHarness harness;
    private KitService kitService;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness(KitService::new);
        kitService = harness.getKitService();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void releasesTheRemainingParticipantsWhenClearingTheKitOfOneOfThemFails() {
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        RuntimeException failure = new IllegalStateException("the inventory could not be cleared");
        fighter.failInventoryClearWith(failure);
        List<LogRecord> logRecords = new ArrayList<>();
        Handler recordingHandler = recordingHandler(logRecords);
        Logger logger = harness.getPlugin().getLogger();
        logger.addHandler(recordingHandler);
        try {
            assertTrue(harness.getMatchService().forceResult(match.getMatchId(),
                    match.getFirstTeam().getTeamName()));

            for (ArenaMatchHarness.FakePlayer participant : List.of(mate, opponent, opponentMate)) {
                String participantName = participant.player().getName();
                assertFalse(kitService.hasAppliedKit(participant.player()),
                        "the kit of " + participantName + " is cleared");
                assertEquals(Optional.of(SpectatorMode.TRIBUNE),
                        harness.getSpectatorService().findSpectatorMode(participant.uniqueId()),
                        participantName + " is sent to the tribune");
            }
            assertTrue(logRecords.stream().anyMatch(logRecord -> logRecord.getLevel() == Level.WARNING
                            && logRecord.getMessage().contains("Fighter")
                            && logRecord.getThrown() == failure),
                    "the failure is logged as a warning with the exception");
        } finally {
            logger.removeHandler(recordingHandler);
        }
    }

    private static Handler recordingHandler(List<LogRecord> logRecords) {
        return new Handler() {

            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }
}
