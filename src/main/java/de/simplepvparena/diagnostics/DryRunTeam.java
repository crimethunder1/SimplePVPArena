package de.simplepvparena.diagnostics;

import de.simplepvparena.team.TeamMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DryRunTeam(String teamName, List<TeamMember> members) {

    public static final String FIRST_TEAM_NAME = "Dry-Run-Alpha";

    public static final String SECOND_TEAM_NAME = "Dry-Run-Bravo";

    public DryRunTeam {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(members, "members");
        members = List.copyOf(members);
    }

    public static List<DryRunTeam> createTestTeams(int memberCount) {
        return List.of(create(FIRST_TEAM_NAME, memberCount), create(SECOND_TEAM_NAME, memberCount));
    }

    private static DryRunTeam create(String teamName, int memberCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int memberNumber = 1; memberNumber <= memberCount; memberNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), teamName + "-" + memberNumber));
        }
        return new DryRunTeam(teamName, members);
    }
}
