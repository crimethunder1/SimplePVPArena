package de.simplepvparena.team;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamMessage;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Team {

    public static final int MINIMUM_MEMBER_COUNT = 1;
    public static final int MAXIMUM_MEMBER_COUNT = 8;

    private final String teamName;
    private final List<TeamMember> members;
    private final TeamColour colour;

    public Team(String teamName, List<TeamMember> members) {
        this(teamName, members, null);
    }

    public Team(String teamName, List<TeamMember> members, TeamColour colour) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(members, "members");
        if (teamName.isBlank()) {
            throw new IllegalArgumentException("teamName must not be blank");
        }
        List<TeamMember> copiedMembers = List.copyOf(members);
        if (copiedMembers.size() < MINIMUM_MEMBER_COUNT || copiedMembers.size() > MAXIMUM_MEMBER_COUNT) {
            throw new IllegalArgumentException(Messages.text(TeamMessage.MEMBER_COUNT_OUT_OF_RANGE,
                    Placeholder.of("minimum", MINIMUM_MEMBER_COUNT), Placeholder.of("maximum", MAXIMUM_MEMBER_COUNT)));
        }
        Set<UUID> memberUniqueIds = new HashSet<>();
        for (TeamMember member : copiedMembers) {
            if (!memberUniqueIds.add(member.uniqueId())) {
                throw new IllegalArgumentException(Messages.text(TeamMessage.SAME_PLAYER_TWICE));
            }
        }
        this.teamName = teamName;
        this.members = copiedMembers;
        this.colour = colour;
    }

    public String getTeamName() {
        return teamName;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public Optional<TeamColour> getColour() {
        return Optional.ofNullable(colour);
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
