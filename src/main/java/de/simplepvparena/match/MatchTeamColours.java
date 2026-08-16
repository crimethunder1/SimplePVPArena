package de.simplepvparena.match;

import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Objects;

public final class MatchTeamColours {

    public static final String FIRST_TEAM_SCOREBOARD_NAME = "etcc_team_1";

    public static final String SECOND_TEAM_SCOREBOARD_NAME = "etcc_team_2";

    public static final List<String> SCOREBOARD_TEAM_NAMES =
            List.of(FIRST_TEAM_SCOREBOARD_NAME, SECOND_TEAM_SCOREBOARD_NAME);

    private MatchTeamColours() {
    }

    public static List<MatchTeamColour> of(Match match, TeamColourSettings settings) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(settings, "settings");
        return List.of(
                colourOf(FIRST_TEAM_SCOREBOARD_NAME, match, match.getFirstTeam(), settings.firstTeamColour()),
                colourOf(SECOND_TEAM_SCOREBOARD_NAME, match, match.getSecondTeam(), settings.secondTeamColour()));
    }

    private static MatchTeamColour colourOf(String scoreboardTeamName, Match match, Team team,
                                            NamedTextColor colour) {
        List<TeamMember> fighters = team.getMembers().stream()
                .filter(member -> isFightingTheRunningRound(match, member))
                .toList();
        return new MatchTeamColour(scoreboardTeamName, team.getTeamName(), colour, fighters);
    }

    private static boolean isFightingTheRunningRound(Match match, TeamMember member) {
        return match.getRoundParticipants()
                .map(roundParticipants -> roundParticipants.isActive(member.uniqueId()))
                .orElse(false);
    }
}
