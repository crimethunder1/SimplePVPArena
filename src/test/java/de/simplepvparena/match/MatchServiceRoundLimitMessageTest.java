package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchServiceRoundLimitMessageTest {

    @Test
    void namesTheSecondsOfARoundThatHasATimeLimit() {
        assertEquals("time limit 300 seconds", MatchService.describeRoundLimit(true, 300));
        assertEquals("time limit 90 seconds", MatchService.describeRoundLimit(true, 90));
    }

    @Test
    void saysWhatDecidesARoundThatHasNone() {
        assertEquals("no time limit, the round runs until one of the teams falls",
                MatchService.describeRoundLimit(false, 300));
    }
}
