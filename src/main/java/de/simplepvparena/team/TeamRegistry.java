package de.simplepvparena.team;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TeamRegistry {

    private static final String TEAMS_KEY = "teams";
    private static final String TEAM_NAME_KEY = "name";
    private static final String MEMBERS_KEY = "members";
    private static final String MEMBER_UNIQUE_ID_KEY = "unique-id";
    private static final String MEMBER_NAME_KEY = "name";
    private static final String UNNAMED_TEAM_PLACEHOLDER = "<team without a name>";

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

    public List<String> loadFromConfiguration(ConfigurationSection rootSection) {
        teamsByLowerCaseName.clear();
        List<String> skippedTeamNames = new ArrayList<>();
        for (Map<?, ?> storedTeam : rootSection.getMapList(TEAMS_KEY)) {
            String teamName = readText(storedTeam.get(TEAM_NAME_KEY));
            List<TeamMember> members = readMembers(storedTeam.get(MEMBERS_KEY));
            if (teamName == null || members.size() != Team.REQUIRED_MEMBER_COUNT) {
                skippedTeamNames.add(teamName == null ? UNNAMED_TEAM_PLACEHOLDER : teamName);
                continue;
            }
            try {
                registerTeam(teamName, members.get(0), members.get(1));
            } catch (IllegalArgumentException | IllegalStateException failure) {
                skippedTeamNames.add(teamName);
            }
        }
        return List.copyOf(skippedTeamNames);
    }

    public void saveToConfiguration(ConfigurationSection rootSection) {
        List<Map<String, Object>> storedTeams = new ArrayList<>();
        for (Team team : teamsByLowerCaseName.values()) {
            Map<String, Object> storedTeam = new LinkedHashMap<>();
            storedTeam.put(TEAM_NAME_KEY, team.getTeamName());
            storedTeam.put(MEMBERS_KEY, writeMembers(team));
            storedTeams.add(storedTeam);
        }
        rootSection.set(TEAMS_KEY, storedTeams);
    }

    private List<Map<String, Object>> writeMembers(Team team) {
        List<Map<String, Object>> storedMembers = new ArrayList<>();
        for (TeamMember member : team.getMembers()) {
            Map<String, Object> storedMember = new LinkedHashMap<>();
            storedMember.put(MEMBER_UNIQUE_ID_KEY, member.uniqueId().toString());
            storedMember.put(MEMBER_NAME_KEY, member.playerName());
            storedMembers.add(storedMember);
        }
        return storedMembers;
    }

    private List<TeamMember> readMembers(Object storedMembers) {
        List<TeamMember> members = new ArrayList<>();
        if (!(storedMembers instanceof List<?> memberEntries)) {
            return members;
        }
        for (Object memberEntry : memberEntries) {
            if (!(memberEntry instanceof Map<?, ?> storedMember)) {
                continue;
            }
            String uniqueIdText = readText(storedMember.get(MEMBER_UNIQUE_ID_KEY));
            String playerName = readText(storedMember.get(MEMBER_NAME_KEY));
            if (uniqueIdText == null || playerName == null) {
                continue;
            }
            try {
                members.add(new TeamMember(UUID.fromString(uniqueIdText), playerName));
            } catch (IllegalArgumentException invalidUniqueId) {
            }
        }
        return members;
    }

    private String readText(Object storedValue) {
        if (storedValue instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
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
