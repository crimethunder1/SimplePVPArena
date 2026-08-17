package de.simplepvparena.gate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartGateCountdownTest {

    @Test
    void showsEverySecondAsItsNumber() {
        for (int remainingSeconds = StartGateDuration.DEFAULT_SECONDS; remainingSeconds > 0; remainingSeconds--) {
            assertFalse(StartGateCountdown.isRelease(remainingSeconds));
            assertEquals(String.valueOf(remainingSeconds), StartGateCountdown.describeTitle(remainingSeconds));
        }
        assertEquals("10", StartGateCountdown.describeTitle(10));
    }

    @Test
    void countsAConfiguredDurationDownTheSameWay() {
        assertEquals("5", StartGateCountdown.describeTitle(5));
        assertEquals("1", StartGateCountdown.describeTitle(1));
        assertEquals(StartGateCountdown.RELEASE_TITLE, StartGateCountdown.describeTitle(0));
        assertEquals(StartGateCountdown.describeSubtitle(10), StartGateCountdown.describeSubtitle(5),
                "every counted second of every length says the same");
    }

    @Test
    void showsTheLastSecondAsTheRelease() {
        assertTrue(StartGateCountdown.isRelease(StartGateCountdown.RELEASE_SECOND));
        assertEquals(StartGateCountdown.RELEASE_TITLE,
                StartGateCountdown.describeTitle(StartGateCountdown.RELEASE_SECOND));
    }

    @Test
    void neverCountsPastTheRelease() {
        assertTrue(StartGateCountdown.isRelease(-3));
        assertEquals(StartGateCountdown.RELEASE_TITLE, StartGateCountdown.describeTitle(-3));
    }

    @Test
    void saysWhatIsHappeningUnderTheNumberAndAnswersItAtTheEnd() {
        String counting = StartGateCountdown.describeSubtitle(StartGateDuration.DEFAULT_SECONDS);
        String release = StartGateCountdown.describeSubtitle(StartGateCountdown.RELEASE_SECOND);

        assertEquals(counting, StartGateCountdown.describeSubtitle(1), "every counted second says the same");
        assertFalse(counting.equals(release), "and the release says something else");
    }
}
