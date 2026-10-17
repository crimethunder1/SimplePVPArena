package de.simplepvparena.team;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TeamSizeSetting {

    public static final int DEFAULT_TEAM_SIZE = 2;

    public static final String TOURNAMENT_IN_PROGRESS_REJECTION =
            "The team size cannot be changed while a tournament is in progress";

    public static final String ALLOWED_RANGE_REJECTION = "The team size must lie between "
            + Team.MINIMUM_MEMBER_COUNT + " and " + Team.MAXIMUM_MEMBER_COUNT + " players";

    private static final int MAXIMUM_NAMED_TEAM_COUNT = 5;

    private int teamSize = DEFAULT_TEAM_SIZE;

    public int getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(int teamSize) {
        if (!isAllowed(teamSize)) {
            throw new IllegalArgumentException(ALLOWED_RANGE_REJECTION + ", not " + teamSize);
        }
        this.teamSize = teamSize;
    }

    public static boolean isAllowed(int teamSize) {
        return teamSize >= Team.MINIMUM_MEMBER_COUNT && teamSize <= Team.MAXIMUM_MEMBER_COUNT;
    }

    public static String describeMatchUp(int teamSize) {
        return teamSize + " (" + teamSize + " versus " + teamSize + ")";
    }

    public static String describePlayerRange(int teamSize) {
        if (teamSize == Team.MINIMUM_MEMBER_COUNT) {
            return teamSize + " player";
        }
        return Team.MINIMUM_MEMBER_COUNT + " to " + teamSize + " players";
    }

    public static Optional<String> findChangeRejection(int newTeamSize, boolean tournamentInProgress,
                                                       TeamRegistry teamRegistry) {
        if (!isAllowed(newTeamSize)) {
            return Optional.of(ALLOWED_RANGE_REJECTION);
        }
        if (tournamentInProgress) {
            return Optional.of(TOURNAMENT_IN_PROGRESS_REJECTION);
        }
        List<Team> largerTeams = findTeamsLargerThan(newTeamSize, teamRegistry);
        if (largerTeams.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(describeTeams(largerTeams, "more than", newTeamSize, Team::getMemberNamesJoined)
                + ". Remove " + (largerTeams.size() == 1 ? "it" : "them") + " first");
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

    public static String describeTeams(List<Team> teams, String sizeComparison, int teamSize,
                                       Function<Team, String> teamDetail) {
        String namedTeams = teams.stream()
                .limit(MAXIMUM_NAMED_TEAM_COUNT)
                .map(team -> team.getTeamName() + " (" + teamDetail.apply(team) + ")")
                .collect(Collectors.joining(", "));
        int unnamedTeamCount = teams.size() - Math.min(teams.size(), MAXIMUM_NAMED_TEAM_COUNT);
        return teams.size() + (teams.size() == 1 ? " team has " : " teams have ") + sizeComparison + " "
                + teamSize + (teamSize == 1 ? " player: " : " players: ") + namedTeams
                + (unnamedTeamCount > 0 ? " and " + unnamedTeamCount + " more" : "");
    }
}
