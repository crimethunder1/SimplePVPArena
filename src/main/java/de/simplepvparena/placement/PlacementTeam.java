package de.simplepvparena.placement;

import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamColour;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Objects;
import java.util.Optional;

public record PlacementTeam(String name, NamedTextColor colour) {

    public static final NamedTextColor COLOUR_WITHOUT_TEAM_COLOUR = NamedTextColor.WHITE;

    public PlacementTeam {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(colour, "colour");
    }

    public static PlacementTeam of(Team team) {
        return of(team.getTeamName(), Optional.of(team));
    }

    public static PlacementTeam of(String teamName, Optional<Team> team) {
        return new PlacementTeam(teamName, team.flatMap(Team::getColour)
                .map(TeamColour::getTextColour)
                .orElse(COLOUR_WITHOUT_TEAM_COLOUR));
    }
}
