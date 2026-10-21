package de.simplepvparena.tournament;

public record AutoMatchSettings(boolean enabled, int startDelaySeconds, int pauseSeconds) {

    public static final boolean DEFAULT_ENABLED = false;

    public static final int DEFAULT_START_DELAY_SECONDS = 10;
    public static final int MINIMUM_START_DELAY_SECONDS = 3;
    public static final int MAXIMUM_START_DELAY_SECONDS = 120;

    public static final int DEFAULT_PAUSE_SECONDS = 20;
    public static final int MINIMUM_PAUSE_SECONDS = 0;
    public static final int MAXIMUM_PAUSE_SECONDS = 300;

    public AutoMatchSettings {
        startDelaySeconds = Math.clamp(startDelaySeconds, MINIMUM_START_DELAY_SECONDS, MAXIMUM_START_DELAY_SECONDS);
        pauseSeconds = Math.clamp(pauseSeconds, MINIMUM_PAUSE_SECONDS, MAXIMUM_PAUSE_SECONDS);
    }

    public static AutoMatchSettings defaults() {
        return new AutoMatchSettings(DEFAULT_ENABLED, DEFAULT_START_DELAY_SECONDS, DEFAULT_PAUSE_SECONDS);
    }

    public AutoMatchSettings withEnabled(boolean enabled) {
        return new AutoMatchSettings(enabled, startDelaySeconds, pauseSeconds);
    }
}
