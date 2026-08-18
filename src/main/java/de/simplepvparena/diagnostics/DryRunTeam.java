package de.simplepvparena.diagnostics;

import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DryRunTeam(String teamName, TeamMember firstMember, TeamMember secondMember) {

    public static final String FIRST_TEAM_NAME = "Dry-Run-Alpha";

    public static final String SECOND_TEAM_NAME = "Dry-Run-Bravo";

    public DryRunTeam {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(firstMember, "firstMember");
        Objects.requireNonNull(secondMember, "secondMember");
    }

    public static List<DryRunTeam> createTestTeams() {
        return List.of(create(FIRST_TEAM_NAME), create(SECOND_TEAM_NAME));
    }

    private static DryRunTeam create(String teamName) {
        return new DryRunTeam(teamName,
                new TeamMember(UUID.randomUUID(), teamName + "-1"),
                new TeamMember(UUID.randomUUID(), teamName + "-2"));
    }

    public List<TeamMember> members() {
        return List.of(firstMember, secondMember);
    }
}
