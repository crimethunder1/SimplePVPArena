package de.simplepvparena.match;

import java.util.Objects;

public record UnequalTeamSettings(UnequalTeamsMode mode, int voteSeconds) {

    public static final UnequalTeamsMode DEFAULT_MODE = UnequalTeamsMode.VOTE;

    public static final int DEFAULT_VOTE_SECONDS = 30;
    public static final int MINIMUM_VOTE_SECONDS = 5;
    public static final int MAXIMUM_VOTE_SECONDS = 120;

    public UnequalTeamSettings {
        Objects.requireNonNull(mode, "mode");
        voteSeconds = Math.clamp(voteSeconds, MINIMUM_VOTE_SECONDS, MAXIMUM_VOTE_SECONDS);
    }

    public static UnequalTeamSettings defaults() {
        return new UnequalTeamSettings(DEFAULT_MODE, DEFAULT_VOTE_SECONDS);
    }
}
