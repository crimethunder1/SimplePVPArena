package de.simplepvparena.gate;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.StartGateMessage;

public final class StartGateCountdown {

    public static final int RELEASE_SECOND = 0;

    private StartGateCountdown() {
    }

    public static boolean isRelease(int remainingSeconds) {
        return remainingSeconds <= RELEASE_SECOND;
    }

    public static String describeReleaseTitle() {
        return Messages.text(StartGateMessage.RELEASE_TITLE);
    }

    public static String describeTitle(int remainingSeconds) {
        return isRelease(remainingSeconds) ? describeReleaseTitle() : String.valueOf(remainingSeconds);
    }

    public static String describeSubtitle(int remainingSeconds) {
        return Messages.text(isRelease(remainingSeconds)
                ? StartGateMessage.RELEASE_SUBTITLE
                : StartGateMessage.COUNTING_SUBTITLE);
    }
}
