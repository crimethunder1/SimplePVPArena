package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchServiceRoundLimitMessageTest {

    @Test
    void namesTheSecondsOfARoundThatHasATimeLimit() {
        assertEquals("Round 1 of match round1-match1 has started (Alpha 0 : 0 Bravo), time limit 300 seconds",
                MatchService.describeRoundStart(1, "round1-match1", "Alpha 0 : 0 Bravo", true, 300));
        assertEquals("Round 2 of match round1-match1 has started (Alpha 1 : 0 Bravo), time limit 90 seconds",
                MatchService.describeRoundStart(2, "round1-match1", "Alpha 1 : 0 Bravo", true, 90));
    }

    @Test
    void saysWhatDecidesARoundThatHasNone() {
        assertEquals("Round 1 of match round1-match1 has started (Alpha 0 : 0 Bravo), no time limit, the round runs "
                + "until one of the teams falls",
                MatchService.describeRoundStart(1, "round1-match1", "Alpha 0 : 0 Bravo", false, 300));
    }
}
