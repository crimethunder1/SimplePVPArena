package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchServiceDrawMessageTest {

    private static final String DRAW_MESSAGE =
            "Round 2 of match match-1 ended without a surviving team, no point is awarded";

    @Test
    void announcesOnlyTheRegularDrawMessageForADoubleDeath() {
        List<String> messages = MatchService.buildDrawMessages(2, "match-1", RoundOutcome.draw());

        assertEquals(List.of(DRAW_MESSAGE), messages);
    }

    @Test
    void addsASecondMessageForADrawCausedByASuddenDeathSecond() {
        List<String> messages = MatchService.buildDrawMessages(2, "match-1", RoundOutcome.suddenDeathDraw());

        assertEquals(2, messages.size());
        assertEquals(DRAW_MESSAGE, messages.get(0));
        assertEquals("Both teams fell in the same sudden death second, round 2 of match match-1 is repeated",
                messages.get(1));
    }
}
