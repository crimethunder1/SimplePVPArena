package de.simplepvparena.listener;

import de.simplepvparena.match.Match;
import de.simplepvparena.match.MatchState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryEntityRoundStartTest {

    private static final String REMOVAL_STEP = "remove temporary entities in " + ArenaMatchHarness.WORLD_NAME;

    private ArenaMatchHarness harness;
    private ArenaMatchHarness.FakePlayer fighter;
    private ArenaMatchHarness.FakePlayer mate;
    private ArenaMatchHarness.FakePlayer opponent;
    private ArenaMatchHarness.FakePlayer opponentMate;

    @BeforeEach
    void setUp() {
        harness = new ArenaMatchHarness();
        fighter = harness.addPlayer("Fighter");
        mate = harness.addPlayer("Mate");
        opponent = harness.addPlayer("Opponent");
        opponentMate = harness.addPlayer("OpponentMate");
    }

    @Test
    void removesTheTemporaryEntitiesOnceBeforeTheFightersAreTeleportedAndEquipped() {
        harness.startMatch(fighter, mate, opponent, opponentMate);

        assertEquals(List.of(REMOVAL_STEP,
                "teleport Fighter", "kit Fighter", "teleport Mate", "kit Mate",
                "teleport Opponent", "kit Opponent", "teleport OpponentMate", "kit OpponentMate"),
                harness.getRoundStartSteps());
        assertEquals(1, harness.getTemporaryEntityRemovalWorlds().size());
        assertSame(harness.getWorld(), harness.getTemporaryEntityRemovalWorlds().get(0),
                "the loaded world of the fight area is handed over");
    }

    @Test
    void removesThemAgainAtTheStartOfEveryFollowingRound() {
        Match match = harness.startMatch(fighter, mate, opponent, opponentMate);
        harness.getMatchService().handlePlayerEliminated(opponent.uniqueId());
        harness.getMatchService().handlePlayerEliminated(opponentMate.uniqueId());
        assertEquals(MatchState.BETWEEN_ROUNDS, match.getState());
        harness.getRoundStartSteps().clear();

        harness.runTick();

        assertEquals(2, match.getRoundNumber());
        assertEquals(REMOVAL_STEP, harness.getRoundStartSteps().get(0),
                "the second round starts with the removal: " + harness.getRoundStartSteps());
        assertEquals(1, harness.getRoundStartSteps().stream().filter(REMOVAL_STEP::equals).count(),
                "and runs it once: " + harness.getRoundStartSteps());
        assertEquals(2, harness.getTemporaryEntityRemovalWorlds().size());
    }

    @Test
    void startsTheRoundAnywayWhenTheRemovalFails() {
        RuntimeException failure = new IllegalStateException("the entities could not be looked up");
        harness.failTemporaryEntityRemovalWith(failure);
        List<LogRecord> logRecords = new ArrayList<>();
        Handler recordingHandler = recordingHandler(logRecords);
        Logger logger = harness.getPlugin().getLogger();
        logger.addHandler(recordingHandler);
        try {
            Match match = harness.startMatch(fighter, mate, opponent, opponentMate);

            assertEquals(MatchState.RUNNING, match.getState(), "the round has started");
            assertEquals(List.of(REMOVAL_STEP,
                    "teleport Fighter", "kit Fighter", "teleport Mate", "kit Mate",
                    "teleport Opponent", "kit Opponent", "teleport OpponentMate", "kit OpponentMate"),
                    harness.getRoundStartSteps());
            assertTrue(logRecords.stream().anyMatch(logRecord -> logRecord.getLevel() == Level.WARNING
                            && logRecord.getMessage().contains("temporary entities of the arena could not be removed")
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
