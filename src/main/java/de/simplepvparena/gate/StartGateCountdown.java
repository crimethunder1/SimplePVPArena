package de.simplepvparena.gate;

public final class StartGateCountdown {

    public static final int RELEASE_SECOND = 0;

    public static final String RELEASE_TITLE = "Fight!";

    private static final String COUNTING_SUBTITLE = "The gate comes down layer by layer";

    private static final String RELEASE_SUBTITLE = "The gate is open";

    private StartGateCountdown() {
    }

    public static boolean isRelease(int remainingSeconds) {
        return remainingSeconds <= RELEASE_SECOND;
    }

    public static String describeTitle(int remainingSeconds) {
        return isRelease(remainingSeconds) ? RELEASE_TITLE : String.valueOf(remainingSeconds);
    }

    public static String describeSubtitle(int remainingSeconds) {
        return isRelease(remainingSeconds) ? RELEASE_SUBTITLE : COUNTING_SUBTITLE;
    }
}
