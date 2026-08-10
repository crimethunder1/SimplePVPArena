package de.simplepvparena.team;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TeamRegistry {

    private final Map<String, Team> teamsByLowerCaseName = new LinkedHashMap<>();

    public Team registerTeam(String teamName, TeamMember firstMember, TeamMember secondMember) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(firstMember, "firstMember");
        Objects.requireNonNull(secondMember, "secondMember");
        String key = toKey(teamName);
        if (teamsByLowerCaseName.containsKey(key)) {
            throw new IllegalStateException("a team named " + teamName + " already exists");
        }
        rejectAlreadyRegisteredPlayer(firstMember);
        rejectAlreadyRegisteredPlayer(secondMember);
        Team team = new Team(teamName, List.of(firstMember, secondMember));
        teamsByLowerCaseName.put(key, team);
        return team;
    }

    public boolean removeTeam(String teamName) {
        return teamsByLowerCaseName.remove(toKey(teamName)) != null;
    }

    public Optional<Team> findTeam(String teamName) {
        if (teamName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(teamsByLowerCaseName.get(toKey(teamName)));
    }

    public Optional<Team> findTeamByPlayer(UUID playerUniqueId) {
        if (playerUniqueId == null) {
            return Optional.empty();
        }
        return teamsByLowerCaseName.values().stream()
                .filter(team -> team.containsPlayer(playerUniqueId))
                .findFirst();
    }

    public Collection<Team> getTeams() {
        return List.copyOf(teamsByLowerCaseName.values());
    }

    public List<String> getTeamNames() {
        return teamsByLowerCaseName.values().stream()
                .map(Team::getTeamName)
                .toList();
    }

    public int getTeamCount() {
        return teamsByLowerCaseName.size();
    }

    public void clear() {
        teamsByLowerCaseName.clear();
    }

    private void rejectAlreadyRegisteredPlayer(TeamMember member) {
        findTeamByPlayer(member.uniqueId()).ifPresent(team -> {
            throw new IllegalStateException(member.playerName() + " is already registered in team " + team.getTeamName());
        });
    }

    private String toKey(String teamName) {
        return teamName.toLowerCase(Locale.ROOT);
    }
}
