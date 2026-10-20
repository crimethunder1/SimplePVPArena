package de.simplepvparena.gate;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.StartGateMessage;

public final class StartGateDuration {

    public static final int DEFAULT_SECONDS = 10;

    public static final int MINIMUM_SECONDS = 1;

    private volatile int seconds = DEFAULT_SECONDS;

    public StartGateDuration() {
    }

    public StartGateDuration(int seconds) {
        setSeconds(seconds);
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = Math.max(MINIMUM_SECONDS, seconds);
    }

    public long getTotalTicks() {
        return seconds * StartGateOpeningSchedule.TICKS_PER_SECOND;
    }

    public String describe() {
        return Messages.plural(StartGateMessage.DURATION, seconds);
    }
}
