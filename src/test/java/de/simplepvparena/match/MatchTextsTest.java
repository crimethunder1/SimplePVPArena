package de.simplepvparena.match;

import de.simplepvparena.message.MatchMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.support.UseLanguage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatchTextsTest {

    private static final String MATCH_ID = "round1-match1";
    private static final String SCORE = "Alpha 0 : 0 Bravo";

    @Test
    @UseLanguage("en")
    void countsTheSecondsOfTheTimeLimitAndOfTheSuddenDeathWarningInEnglish() {
        assertEquals("Round 1 of match round1-match1 has started (Alpha 0 : 0 Bravo), time limit 1 second",
                MatchService.describeRoundStart(1, MATCH_ID, SCORE, true, 1));
        assertEquals("Round 1 of match round1-match1 has started (Alpha 0 : 0 Bravo), time limit 2 seconds",
                MatchService.describeRoundStart(1, MATCH_ID, SCORE, true, 2));
        assertEquals("Sudden death in 1 second", Messages.plural(MatchMessage.SUDDEN_DEATH_WARNING, 1));
        assertEquals("Sudden death in 2 seconds", Messages.plural(MatchMessage.SUDDEN_DEATH_WARNING, 2));
    }

    @Test
    @UseLanguage("de")
    void countsTheSecondsOfTheTimeLimitAndOfTheSuddenDeathWarningInGerman() {
        assertEquals("Runde 1 von Match round1-match1 hat begonnen (Alpha 0 : 0 Bravo), Zeitlimit 1 Sekunde",
                MatchService.describeRoundStart(1, MATCH_ID, SCORE, true, 1));
        assertEquals("Runde 1 von Match round1-match1 hat begonnen (Alpha 0 : 0 Bravo), Zeitlimit 2 Sekunden",
                MatchService.describeRoundStart(1, MATCH_ID, SCORE, true, 2));
        assertEquals("Runde 1 von Match round1-match1 hat begonnen (Alpha 0 : 0 Bravo), ohne Zeitlimit, die Runde "
                + "läuft, bis eines der Teams fällt",
                MatchService.describeRoundStart(1, MATCH_ID, SCORE, false, 300));
        assertEquals("Sudden Death in 1 Sekunde", Messages.plural(MatchMessage.SUDDEN_DEATH_WARNING, 1));
        assertEquals("Sudden Death in 2 Sekunden", Messages.plural(MatchMessage.SUDDEN_DEATH_WARNING, 2));
    }

    @Test
    @UseLanguage("de")
    void reportsADrawInGerman() {
        assertEquals(List.of("Runde 2 von Match match-1 endete ohne überlebendes Team, es gibt keinen Punkt",
                "Beide Teams sind in derselben Sekunde des Sudden Death gefallen, Runde 2 von Match match-1 wird "
                        + "wiederholt"), MatchService.buildDrawMessages(2, "match-1", RoundOutcome.suddenDeathDraw()));
    }
}
