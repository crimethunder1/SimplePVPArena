package de.simplepvparena.team;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Team {

    public static final int REQUIRED_MEMBER_COUNT = 2;

    private final String teamName;
    private final List<TeamMember> members;

    public Team(String teamName, List<TeamMember> members) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(members, "members");
        if (teamName.isBlank()) {
            throw new IllegalArgumentException("teamName must not be blank");
        }
        if (members.size() != REQUIRED_MEMBER_COUNT) {
            throw new IllegalArgumentException("a team consists of exactly " + REQUIRED_MEMBER_COUNT + " players");
        }
        if (members.get(0).uniqueId().equals(members.get(1).uniqueId())) {
            throw new IllegalArgumentException("a team must consist of two different players");
        }
        this.teamName = teamName;
        this.members = List.copyOf(members);
    }

    public String getTeamName() {
        return teamName;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public Set<UUID> getMemberUniqueIds() {
        return members.stream()
                .map(TeamMember::uniqueId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean containsPlayer(UUID playerUniqueId) {
        return members.stream().anyMatch(member -> member.uniqueId().equals(playerUniqueId));
    }

    public String getMemberNamesJoined() {
        return members.stream()
                .map(TeamMember::playerName)
                .collect(Collectors.joining(", "));
    }
}
