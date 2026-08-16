package de.simplepvparena.team;

import java.util.Objects;

public record EventTeamEntry(String teamName, String firstPlayerName, String secondPlayerName) {

    public EventTeamEntry {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(firstPlayerName, "firstPlayerName");
        Objects.requireNonNull(secondPlayerName, "secondPlayerName");
    }
}
