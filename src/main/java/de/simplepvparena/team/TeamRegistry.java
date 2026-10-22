package de.simplepvparena.team;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TeamMessage;
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

    private static final String TEAMS_KEY = "teams";
    private static final String TEAM_NAME_KEY = "name";
    private static final String MEMBERS_KEY = "members";
    private static final String COLOUR_KEY = "colour";
    private static final String MEMBER_UNIQUE_ID_KEY = "unique-id";
    private static final String MEMBER_NAME_KEY = "name";

    private final Map<String, Team> teamsByLowerCaseName = new LinkedHashMap<>();

    public static String describeNameRule() {
        return Messages.text(TeamMessage.NAME_RULE, Placeholder.of("maximumLength", MAXIMUM_NAME_LENGTH));
    }

    public static String describeLastMemberRejection() {
        return Messages.plural(TeamMessage.LAST_MEMBER, Team.MINIMUM_MEMBER_COUNT);
    }

    public Team registerTeam(String teamName, List<TeamMember> members) {
        return registerTeam(teamName, members, null);
    }

    public Team registerTeam(String teamName, List<TeamMember> members, TeamColour colour) {
        Objects.requireNonNull(teamName, "teamName");
        Optional<String> nameRejection = findNameRejection(teamName);
        if (nameRejection.isPresent()) {
            throw new IllegalArgumentException(nameRejection.get());
        }
        return register(teamName, members, colour);
    }

    public static boolean isAllowedTeamName(String teamName) {
        return findNameRejection(teamName).isEmpty();
    }

    private Team register(String teamName, List<TeamMember> members, TeamColour colour) {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(members, "members");
        String key = toKey(teamName);
        if (teamsByLowerCaseName.containsKey(key)) {
            throw new IllegalStateException(Messages.text(TeamMessage.ALREADY_EXISTS,
                    Placeholder.of("team", teamName)));
        }
        for (TeamMember member : members) {
            rejectAlreadyRegisteredPlayer(member);
        }
        Team team = new Team(teamName, members, colour);
        teamsByLowerCaseName.put(key, team);
        return team;
    }

    public boolean removeTeam(String teamName) {
        return teamsByLowerCaseName.remove(toKey(teamName)) != null;
    }

    public Team renameTeam(String teamName, String newName) {
        Objects.requireNonNull(newName, "newName");
        Team team = requireTeam(teamName);
        Optional<String> nameRejection = findNameRejection(newName);
        if (nameRejection.isPresent()) {
            throw new IllegalArgumentException(nameRejection.get());
        }
        String key = toKey(team.getTeamName());
        String newKey = toKey(newName);
        if (!newKey.equals(key) && teamsByLowerCaseName.containsKey(newKey)) {
            throw new IllegalStateException(Messages.text(TeamMessage.ALREADY_EXISTS,
                    Placeholder.of("team", newName)));
        }
        Team renamedTeam = new Team(newName, team.getMembers(), team.getColour().orElse(null));
        replaceTeam(key, newKey, renamedTeam);
        return renamedTeam;
    }

    public Team addMember(String teamName, TeamMember member) {
        Objects.requireNonNull(member, "member");
        Team team = requireTeam(teamName);
        rejectAlreadyRegisteredPlayer(member);
        if (team.getMembers().size() >= Team.MAXIMUM_MEMBER_COUNT) {
            throw new IllegalStateException(Messages.plural(TeamMessage.TEAM_FULL, Team.MAXIMUM_MEMBER_COUNT,
                    Placeholder.of("team", team.getTeamName())));
        }
        List<TeamMember> members = new ArrayList<>(team.getMembers());
        members.add(member);
        return replaceMembers(team, members);
    }

    public Team removeMember(String teamName, UUID memberUniqueId) {
        Objects.requireNonNull(memberUniqueId, "memberUniqueId");
        Team team = requireTeam(teamName);
        if (!team.containsPlayer(memberUniqueId)) {
            throw new IllegalArgumentException(Messages.text(TeamMessage.PLAYER_NOT_IN_TEAM,
                    Placeholder.of("uniqueId", memberUniqueId), Placeholder.of("team", team.getTeamName())));
        }
        if (team.getMembers().size() <= Team.MINIMUM_MEMBER_COUNT) {
            throw new IllegalStateException(describeLastMemberRejection());
        }
        List<TeamMember> members = team.getMembers().stream()
                .filter(member -> !member.uniqueId().equals(memberUniqueId))
                .toList();
        return replaceMembers(team, members);
    }

    private Team requireTeam(String teamName) {
        return findTeam(teamName).orElseThrow(() -> new IllegalArgumentException(Messages.text(
                TeamMessage.NOT_REGISTERED, Placeholder.of("team", teamName))));
    }

    private Team replaceMembers(Team team, List<TeamMember> members) {
        Team changedTeam = new Team(team.getTeamName(), members, team.getColour().orElse(null));
        String key = toKey(team.getTeamName());
        replaceTeam(key, key, changedTeam);
        return changedTeam;
    }

    private void replaceTeam(String key, String newKey, Team changedTeam) {
        if (key.equals(newKey)) {
            teamsByLowerCaseName.put(key, changedTeam);
            return;
        }
        Map<String, Team> reorderedTeams = new LinkedHashMap<>();
        for (Map.Entry<String, Team> entry : teamsByLowerCaseName.entrySet()) {
            if (entry.getKey().equals(key)) {
                reorderedTeams.put(newKey, changedTeam);
            } else {
                reorderedTeams.put(entry.getKey(), entry.getValue());
            }
        }
        teamsByLowerCaseName.clear();
        teamsByLowerCaseName.putAll(reorderedTeams);
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
            team.getColour().ifPresent(colour -> storedTeam.put(COLOUR_KEY, colour.getStorageName()));
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
            register(teamName, readMembers(storedTeam.get(MEMBERS_KEY)), readColour(storedTeam.get(COLOUR_KEY)));
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

    private TeamColour readColour(Object storedColour) {
        if (storedColour == null) {
            return null;
        }
        String colourName = readText(storedColour);
        return TeamColour.ofStorageName(colourName).orElseThrow(() -> new IllegalArgumentException(
                "the team has the unknown " + COLOUR_KEY + " " + storedColour));
    }

    private String readText(Object storedValue) {
        if (storedValue instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private static Optional<String> findNameRejection(String teamName) {
        if (teamName.isBlank()) {
            return Optional.of(Messages.text(TeamMessage.NAME_MISSING, Placeholder.of("rule", describeNameRule())));
        }
        if (!isAllowedName(teamName)) {
            return Optional.of(Messages.text(TeamMessage.NAME_NOT_ALLOWED, Placeholder.of("team", teamName),
                    Placeholder.of("rule", describeNameRule())));
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
            throw new IllegalStateException(Messages.text(TeamMessage.PLAYER_ALREADY_IN_TEAM,
                    Placeholder.of("player", member.playerName()), Placeholder.of("team", team.getTeamName())));
        });
    }

    private String toKey(String teamName) {
        return teamName.toLowerCase(Locale.ROOT);
    }
}
