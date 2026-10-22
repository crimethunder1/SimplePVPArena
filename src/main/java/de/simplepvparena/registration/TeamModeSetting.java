package de.simplepvparena.registration;

import java.util.Objects;

public final class TeamModeSetting {

    private TeamMode teamMode = TeamMode.DEFAULT_TEAM_MODE;

    public TeamMode getTeamMode() {
        return teamMode;
    }

    public void setTeamMode(TeamMode teamMode) {
        this.teamMode = Objects.requireNonNull(teamMode, "teamMode");
    }

    public boolean isFreeChoice() {
        return teamMode == TeamMode.FREE_CHOICE;
    }
}
