package de.simplepvparena.match;

import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Objects;

public record MatchTeamColour(String scoreboardTeamName, String teamName, NamedTextColor colour,
                              List<TeamMember> fighters) {

    public MatchTeamColour {
        Objects.requireNonNull(scoreboardTeamName, "scoreboardTeamName");
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(colour, "colour");
        fighters = List.copyOf(Objects.requireNonNull(fighters, "fighters"));
    }

    public List<String> fighterNames() {
        return fighters.stream().map(TeamMember::playerName).toList();
    }
}
