package de.simplepvparena.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundTimerTest {

    private final RoundTimeoutSettings settings = new RoundTimeoutSettings(10, 3, 2.0D, 1.0D);

    @Test
    void countsDownWithoutSignalUntilTheWarningWindowIsReached() {
        RoundTimer timer = new RoundTimer(settings);

        for (int second = 1; second <= 6; second++) {
            assertEquals(RoundTimerSignal.RUNNING, timer.tick());
        }
        assertEquals(4, timer.getRemainingSeconds());
        assertFalse(timer.isSuddenDeathActive());
    }

    @Test
    void warnsForEverySecondOfTheWarningWindow() {
        RoundTimer timer = new RoundTimer(settings);
        tickTimes(timer, 6);

        assertEquals(RoundTimerSignal.WARNING, timer.tick());
        assertEquals(3, timer.getRemainingSeconds());
        assertEquals(RoundTimerSignal.WARNING, timer.tick());
        assertEquals(2, timer.getRemainingSeconds());
        assertEquals(RoundTimerSignal.WARNING, timer.tick());
        assertEquals(1, timer.getRemainingSeconds());
    }

    @Test
    void startsSuddenDeathExactlyOnceWhenTheTimeLimitIsOver() {
        RoundTimer timer = new RoundTimer(settings);
        tickTimes(timer, 9);

        assertEquals(RoundTimerSignal.SUDDEN_DEATH_STARTED, timer.tick());
        assertTrue(timer.isSuddenDeathActive());
        assertEquals(0, timer.getRemainingSeconds());
        assertEquals(0, timer.getSuddenDeathSeconds());
        assertEquals(RoundTimerSignal.SUDDEN_DEATH_DAMAGE, timer.tick());
    }

    @Test
    void buildsUpTheSuddenDeathDamageSecondBySecond() {
        RoundTimer timer = new RoundTimer(settings);
        tickTimes(timer, 10);

        assertEquals(0.0D, timer.getCurrentSuddenDeathDamage());
        timer.tick();
        assertEquals(2.0D, timer.getCurrentSuddenDeathDamage());
        timer.tick();
        assertEquals(3.0D, timer.getCurrentSuddenDeathDamage());
        timer.tick();
        assertEquals(4.0D, timer.getCurrentSuddenDeathDamage());
    }

    @Test
    void keepsTheRemainingTimeWhilePaused() {
        RoundTimer timer = new RoundTimer(settings);
        tickTimes(timer, 4);

        timer.pause();
        assertEquals(RoundTimerSignal.PAUSED, timer.tick());
        assertEquals(RoundTimerSignal.PAUSED, timer.tick());
        assertEquals(6, timer.getRemainingSeconds());
        assertTrue(timer.isPaused());

        timer.resume();
        assertFalse(timer.isPaused());
        assertEquals(RoundTimerSignal.RUNNING, timer.tick());
        assertEquals(5, timer.getRemainingSeconds());
    }

    @Test
    void stopsTheSuddenDeathDamageWhilePaused() {
        RoundTimer timer = new RoundTimer(settings);
        tickTimes(timer, 11);

        timer.pause();
        assertEquals(RoundTimerSignal.PAUSED, timer.tick());
        assertEquals(2.0D, timer.getCurrentSuddenDeathDamage());

        timer.resume();
        assertEquals(RoundTimerSignal.SUDDEN_DEATH_DAMAGE, timer.tick());
        assertEquals(3.0D, timer.getCurrentSuddenDeathDamage());
    }

    @Test
    void clampsAWarningWindowThatIsLongerThanTheRound() {
        RoundTimeoutSettings clampedSettings = new RoundTimeoutSettings(5, 30, 2.0D, 1.0D);

        assertEquals(5, clampedSettings.suddenDeathWarningSeconds());
        assertEquals(RoundTimerSignal.WARNING, new RoundTimer(clampedSettings).tick());
    }

    @Test
    void rejectsARoundWithoutATimeLimit() {
        assertThrows(IllegalArgumentException.class, () -> new RoundTimeoutSettings(0, 10, 2.0D, 1.0D));
    }

    @Test
    void neverReportsNegativeSuddenDeathDamage() {
        RoundTimeoutSettings negativeSettings = new RoundTimeoutSettings(10, 3, -5.0D, -1.0D);

        assertEquals(0.0D, negativeSettings.damageForSuddenDeathSecond(0));
        assertEquals(0.0D, negativeSettings.damageForSuddenDeathSecond(1));
        assertEquals(0.0D, negativeSettings.damageForSuddenDeathSecond(7));
    }

    private void tickTimes(RoundTimer timer, int secondCount) {
        for (int second = 0; second < secondCount; second++) {
            timer.tick();
        }
    }
}
