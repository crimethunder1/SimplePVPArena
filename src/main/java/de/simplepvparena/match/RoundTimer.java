package de.simplepvparena.match;

import java.util.Objects;

public final class RoundTimer {

    private final RoundTimeoutSettings settings;

    private int elapsedSeconds;
    private int suddenDeathSeconds;
    private boolean suddenDeathActive;
    private boolean paused;

    public RoundTimer(RoundTimeoutSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public RoundTimerSignal tick() {
        if (paused) {
            return RoundTimerSignal.PAUSED;
        }
        if (suddenDeathActive) {
            suddenDeathSeconds++;
            return RoundTimerSignal.SUDDEN_DEATH_DAMAGE;
        }
        elapsedSeconds++;
        int remainingSeconds = getRemainingSeconds();
        if (remainingSeconds == 0) {
            suddenDeathActive = true;
            return RoundTimerSignal.SUDDEN_DEATH_STARTED;
        }
        if (remainingSeconds <= settings.suddenDeathWarningSeconds()) {
            return RoundTimerSignal.WARNING;
        }
        return RoundTimerSignal.RUNNING;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isSuddenDeathActive() {
        return suddenDeathActive;
    }

    public int getRemainingSeconds() {
        return Math.max(0, settings.roundTimeoutSeconds() - elapsedSeconds);
    }

    public int getSuddenDeathSeconds() {
        return suddenDeathSeconds;
    }

    public double getCurrentSuddenDeathDamage() {
        return settings.damageForSuddenDeathSecond(suddenDeathSeconds);
    }

    public RoundTimeoutSettings getSettings() {
        return settings;
    }
}
