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

    public static final int MAXIMUM_NAME_LENGTH = 16;
    public static final String NAME_RULE = "1 to " + MAXIMUM_NAME_LENGTH
            + " letters, digits, hyphens or underscores";

    private static final String TEAMS_KEY = "teams";
    private static final String TEAM_NAME_KEY = "name";
    private static final String MEMBERS_KEY = "members";
    private static final String MEMBER_UNIQUE_ID_KEY = "unique-id";
    private static final String MEMBER_NAME_KEY = "name";

    private final Map<String, Team> teamsByLowerCaseName = new LinkedHashMap<>();

    public Team registerTeam(String teamName, List<TeamMember> members) {
        Objects.requireNonNull(teamName, "teamName");
        Optional<String> nameRejection = findNameRejection(teamName);
        if (nameRejection.isPresent()) {
            throw new IllegalArgumentException(nameRejection.get());
        }
        return register(teamName, members);
    }

    private Team register(String teamName, List<TeamMember> members) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(members, "members");
        String key = toKey(teamName);
        if (teamsByLowerCaseName.containsKey(key)) {
            throw new IllegalStateException("a team named " + teamName + " already exists");
        }
        for (TeamMember member : members) {
            rejectAlreadyRegisteredPlayer(member);
        }
        Team team = new Team(teamName, members);
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
        Object storedTeams = rootSection.get(TEAMS_KEY);
        if (storedTeams == null) {
            return List.of();
        }
        if (!(storedTeams instanceof List<?> teamEntries)) {
            return List.of("The " + TEAMS_KEY + " entry is no list, not a single team could be read");
        }
        List<String> skippedEntries = new ArrayList<>();
        int entryNumber = 0;
        for (Object teamEntry : teamEntries) {
            entryNumber++;
            loadTeam(teamEntry, entryNumber).ifPresent(skippedEntries::add);
        }
        return List.copyOf(skippedEntries);
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

    private Optional<String> loadTeam(Object teamEntry, int entryNumber) {
        String entryDescription = "Entry " + entryNumber + " of the " + TEAMS_KEY + " list";
        if (!(teamEntry instanceof Map<?, ?> storedTeam)) {
            return Optional.of(entryDescription + ": it is no team");
        }
        String teamName = readText(storedTeam.get(TEAM_NAME_KEY));
        if (teamName == null) {
            return Optional.of(entryDescription + ": the team has no readable " + TEAM_NAME_KEY);
        }
        try {
            register(teamName, readMembers(storedTeam.get(MEMBERS_KEY)));
            return Optional.empty();
        } catch (IllegalArgumentException | IllegalStateException refusal) {
            return Optional.of("Team " + teamName + ": " + refusal.getMessage());
        }
    }

    private List<TeamMember> readMembers(Object storedMembers) {
        if (!(storedMembers instanceof List<?> memberEntries)) {
            throw new IllegalArgumentException("the team has no readable " + MEMBERS_KEY + " list");
        }
        List<TeamMember> members = new ArrayList<>();
        int memberNumber = 0;
        for (Object memberEntry : memberEntries) {
            memberNumber++;
            members.add(readMember(memberEntry, memberNumber));
        }
        return members;
    }

    private TeamMember readMember(Object memberEntry, int memberNumber) {
        String memberDescription = "member " + memberNumber;
        if (!(memberEntry instanceof Map<?, ?> storedMember)) {
            throw new IllegalArgumentException(memberDescription + " is no player");
        }
        String uniqueIdText = readText(storedMember.get(MEMBER_UNIQUE_ID_KEY));
        if (uniqueIdText == null) {
            throw new IllegalArgumentException(memberDescription + " has no readable " + MEMBER_UNIQUE_ID_KEY);
        }
        String playerName = readText(storedMember.get(MEMBER_NAME_KEY));
        if (playerName == null) {
            throw new IllegalArgumentException(memberDescription + " has no readable " + MEMBER_NAME_KEY);
        }
        try {
            return new TeamMember(UUID.fromString(uniqueIdText), playerName);
        } catch (IllegalArgumentException invalidUniqueId) {
            throw new IllegalArgumentException(memberDescription + " has the invalid " + MEMBER_UNIQUE_ID_KEY + " "
                    + uniqueIdText);
        }
    }

    private String readText(Object storedValue) {
        if (storedValue instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private static Optional<String> findNameRejection(String teamName) {
        if (teamName.isBlank()) {
            return Optional.of("A team needs a name, a team name has " + NAME_RULE);
        }
        if (!isAllowedName(teamName)) {
            return Optional.of("The team name " + teamName + " is not allowed, a team name has " + NAME_RULE);
        }
        return Optional.empty();
    }

    private static boolean isAllowedName(String teamName) {
        int nameLength = teamName.codePointCount(0, teamName.length());
        return nameLength >= 1 && nameLength <= MAXIMUM_NAME_LENGTH
                && teamName.codePoints().allMatch(TeamRegistry::isAllowedNameCharacter);
    }

    private static boolean isAllowedNameCharacter(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '-' || codePoint == '_';
    }

    private void rejectAlreadyRegisteredPlayer(TeamMember member) {
        findTeamByPlayer(member.uniqueId()).ifPresent(team -> {
            throw new IllegalStateException(member.playerName() + " is already registered in team "
                    + team.getTeamName());
        });
    }

    private String toKey(String teamName) {
        return teamName.toLowerCase(Locale.ROOT);
    }
}
