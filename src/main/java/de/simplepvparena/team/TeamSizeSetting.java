package de.simplepvparena.team;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamMessage;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TeamSizeSetting {

    public static final int DEFAULT_TEAM_SIZE = 2;

    private static final int MAXIMUM_NAMED_TEAM_COUNT = 5;

    private int teamSize = DEFAULT_TEAM_SIZE;

    public int getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(int teamSize) {
        requireAllowed(teamSize);
        this.teamSize = teamSize;
    }

    public static boolean isAllowed(int teamSize) {
        return teamSize >= Team.MINIMUM_MEMBER_COUNT && teamSize <= Team.MAXIMUM_MEMBER_COUNT;
    }

    static void requireAllowed(int teamSize) {
        if (!isAllowed(teamSize)) {
            throw new IllegalArgumentException("The team size must lie between %d and %d players, not %d"
                    .formatted(Team.MINIMUM_MEMBER_COUNT, Team.MAXIMUM_MEMBER_COUNT, teamSize));
        }
    }

    public static String describeMatchUp(int teamSize) {
        return Messages.text(TeamMessage.SIZE_MATCH_UP, Placeholder.of("teamSize", teamSize));
    }

    public static Optional<String> findChangeRejection(int currentTeamSize, int newTeamSize,
                                                       boolean tournamentInProgress, TeamRegistry teamRegistry) {
        if (!isAllowed(newTeamSize)) {
            return Optional.of(Messages.text(TeamMessage.SIZE_OUT_OF_RANGE,
                    Placeholder.of("minimum", Team.MINIMUM_MEMBER_COUNT),
                    Placeholder.of("maximum", Team.MAXIMUM_MEMBER_COUNT)));
        }
        if (tournamentInProgress) {
            return Optional.of(Messages.text(TeamMessage.SIZE_TOURNAMENT_IN_PROGRESS));
        }
        if (newTeamSize >= currentTeamSize) {
            return Optional.empty();
        }
        List<Team> largerTeams = findTeamsLargerThan(newTeamSize, teamRegistry);
        if (largerTeams.isEmpty()) {
            return Optional.empty();
        }
        String largerTeamsDescription = describeLargerTeams(largerTeams, newTeamSize,
                TeamSizeSetting::describeWithMembers);
        return Optional.of(Messages.plural(TeamMessage.REMOVE_LARGER_TEAMS_FIRST, largerTeams.size(),
                Placeholder.of("largerTeams", largerTeamsDescription)));
    }

    public static Optional<String> describeTeamsLargerThan(int teamSize, TeamRegistry teamRegistry) {
        List<Team> largerTeams = findTeamsLargerThan(teamSize, teamRegistry);
        if (largerTeams.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(describeLargerTeams(largerTeams, teamSize, TeamSizeSetting::describeWithMemberCount));
    }

    public static Optional<String> describeTeamsSmallerThan(int teamSize, TeamRegistry teamRegistry) {
        List<Team> smallerTeams = findTeamsSmallerThan(teamSize, teamRegistry);
        if (smallerTeams.isEmpty()) {
            return Optional.empty();
        }
        TeamMessage key = teamSize == 1
                ? TeamMessage.TEAMS_SMALLER_THAN_ONE_PLAYER
                : TeamMessage.TEAMS_SMALLER_THAN_SEVERAL_PLAYERS;
        return Optional.of(describeTeams(key, smallerTeams, teamSize, TeamSizeSetting::describeWithMemberCount));
    }

    public static List<Team> findTeamsLargerThan(int teamSize, TeamRegistry teamRegistry) {
        return teamRegistry.getTeams().stream()
                .filter(team -> team.getMembers().size() > teamSize)
                .toList();
    }

    public static List<Team> findTeamsSmallerThan(int teamSize, TeamRegistry teamRegistry) {
        return teamRegistry.getTeams().stream()
                .filter(team -> team.getMembers().size() < teamSize)
                .toList();
    }

    private static String describeLargerTeams(List<Team> teams, int teamSize, Function<Team, String> describeTeam) {
        TeamMessage key = teamSize == 1
                ? TeamMessage.TEAMS_LARGER_THAN_ONE_PLAYER
                : TeamMessage.TEAMS_LARGER_THAN_SEVERAL_PLAYERS;
        return describeTeams(key, teams, teamSize, describeTeam);
    }

    private static String describeTeams(TeamMessage key, List<Team> teams, int teamSize,
                                        Function<Team, String> describeTeam) {
        List<String> describedTeams = teams.stream().map(describeTeam).toList();
        return Messages.plural(key, teams.size(), Placeholder.of("teamSize", teamSize),
                Placeholder.of("teams", Messages.listLimited(describedTeams, MAXIMUM_NAMED_TEAM_COUNT)));
    }

    private static String describeWithMembers(Team team) {
        return Messages.text(TeamMessage.TEAM_WITH_MEMBERS, Placeholder.of("team", team.getTeamName()),
                Placeholder.of("members", team.getMemberNamesJoined()));
    }

    private static String describeWithMemberCount(Team team) {
        return Messages.text(TeamMessage.TEAM_WITH_MEMBER_COUNT, Placeholder.of("team", team.getTeamName()),
                Placeholder.of("memberCount", team.getMembers().size()));
    }
}
