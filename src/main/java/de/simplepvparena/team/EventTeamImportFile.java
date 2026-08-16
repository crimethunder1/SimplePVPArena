package de.simplepvparena.team;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class EventTeamImportFile {

    public static final String FILE_NAME = "event.json";

    static final String TEAMS_KEY = "teams";
    static final String TEAM_NAME_KEY = "team_name";
    static final String PLAYERS_KEY = "players";

    private final List<EventTeamEntry> entries;
    private final List<String> problems;
    private final String failureReason;

    private EventTeamImportFile(List<EventTeamEntry> entries, List<String> problems, String failureReason) {
        this.entries = List.copyOf(entries);
        this.problems = List.copyOf(problems);
        this.failureReason = failureReason;
    }

    public static EventTeamImportFile read(File file) {
        if (file == null || !file.isFile()) {
            return failed("There is no " + FILE_NAME + " in the plugin folder");
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader));
        } catch (IOException | JsonParseException failure) {
            return failed(FILE_NAME + " could not be read: " + describe(failure));
        }
    }

    public static EventTeamImportFile parse(String json) {
        try {
            return parse(JsonParser.parseString(json));
        } catch (JsonParseException failure) {
            return failed(FILE_NAME + " could not be read: " + describe(failure));
        }
    }

    public boolean isReadable() {
        return failureReason == null;
    }

    public List<EventTeamEntry> getEntries() {
        return entries;
    }

    public List<String> getProblems() {
        return problems;
    }

    public Optional<String> getFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    private static EventTeamImportFile parse(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            return failed(FILE_NAME + " does not hold a json object");
        }
        JsonElement storedTeams = root.getAsJsonObject().get(TEAMS_KEY);
        if (storedTeams == null || !storedTeams.isJsonArray()) {
            return failed(FILE_NAME + " holds no \"" + TEAMS_KEY + "\" list");
        }
        List<String> problems = new ArrayList<>();
        List<EventTeamEntry> entries = new ArrayList<>();
        int entryNumber = 0;
        for (JsonElement storedTeam : storedTeams.getAsJsonArray()) {
            entryNumber++;
            readEntry(storedTeam, entryNumber, problems).ifPresent(entries::add);
        }
        return new EventTeamImportFile(dropAmbiguousEntries(entries, problems), problems, null);
    }

    private static Optional<EventTeamEntry> readEntry(JsonElement storedTeam, int entryNumber,
                                                      List<String> problems) {
        if (storedTeam == null || !storedTeam.isJsonObject()) {
            problems.add(describeEntry(entryNumber) + " is no json object");
            return Optional.empty();
        }
        JsonObject entry = storedTeam.getAsJsonObject();
        String teamName = readText(entry.get(TEAM_NAME_KEY));
        if (teamName == null) {
            problems.add(describeEntry(entryNumber) + " has no " + TEAM_NAME_KEY);
            return Optional.empty();
        }
        JsonElement storedPlayers = entry.get(PLAYERS_KEY);
        if (storedPlayers == null || !storedPlayers.isJsonArray()) {
            problems.add("Team " + teamName + " has no " + PLAYERS_KEY + " list");
            return Optional.empty();
        }
        return readPlayers(teamName, storedPlayers.getAsJsonArray(), problems);
    }

    private static Optional<EventTeamEntry> readPlayers(String teamName, JsonArray storedPlayers,
                                                        List<String> problems) {
        List<String> playerNames = new ArrayList<>();
        for (JsonElement storedPlayer : storedPlayers) {
            String playerName = readText(storedPlayer);
            if (playerName == null) {
                problems.add("Team " + teamName + " names a player that is no text");
                return Optional.empty();
            }
            playerNames.add(playerName);
        }
        if (playerNames.size() != Team.REQUIRED_MEMBER_COUNT) {
            problems.add("Team " + teamName + " names " + playerNames.size()
                    + (playerNames.size() == 1 ? " player" : " players") + ", a team consists of exactly "
                    + Team.REQUIRED_MEMBER_COUNT);
            return Optional.empty();
        }
        String firstPlayerName = playerNames.get(0);
        String secondPlayerName = playerNames.get(1);
        if (firstPlayerName.equalsIgnoreCase(secondPlayerName)) {
            problems.add("Team " + teamName + " names " + firstPlayerName
                    + " twice, a team consists of two different players");
            return Optional.empty();
        }
        return Optional.of(new EventTeamEntry(teamName, firstPlayerName, secondPlayerName));
    }

    private static List<EventTeamEntry> dropAmbiguousEntries(List<EventTeamEntry> entries, List<String> problems) {
        Set<EventTeamEntry> ambiguousEntries = new LinkedHashSet<>();
        collectDuplicateTeamNames(entries, problems, ambiguousEntries);
        collectPlayersInSeveralTeams(entries, problems, ambiguousEntries);
        return entries.stream()
                .filter(entry -> !ambiguousEntries.contains(entry))
                .toList();
    }

    private static void collectDuplicateTeamNames(List<EventTeamEntry> entries, List<String> problems,
                                                  Set<EventTeamEntry> ambiguousEntries) {
        Map<String, List<EventTeamEntry>> entriesByTeamKey = new LinkedHashMap<>();
        for (EventTeamEntry entry : entries) {
            entriesByTeamKey.computeIfAbsent(toKey(entry.teamName()), key -> new ArrayList<>()).add(entry);
        }
        for (List<EventTeamEntry> collidingEntries : entriesByTeamKey.values()) {
            if (collidingEntries.size() < 2) {
                continue;
            }
            problems.add("Team " + collidingEntries.get(0).teamName() + " stands " + collidingEntries.size()
                    + " times in the file, none of those entries is imported");
            ambiguousEntries.addAll(collidingEntries);
        }
    }

    private static void collectPlayersInSeveralTeams(List<EventTeamEntry> entries, List<String> problems,
                                                     Set<EventTeamEntry> ambiguousEntries) {
        Map<String, List<EventTeamEntry>> entriesByPlayerKey = new LinkedHashMap<>();
        Map<String, String> playerNamesByKey = new LinkedHashMap<>();
        for (EventTeamEntry entry : entries) {
            for (String playerName : List.of(entry.firstPlayerName(), entry.secondPlayerName())) {
                String playerKey = toKey(playerName);
                playerNamesByKey.putIfAbsent(playerKey, playerName);
                entriesByPlayerKey.computeIfAbsent(playerKey, key -> new ArrayList<>()).add(entry);
            }
        }
        for (Map.Entry<String, List<EventTeamEntry>> playerEntries : entriesByPlayerKey.entrySet()) {
            List<EventTeamEntry> collidingEntries = playerEntries.getValue();
            if (collidingEntries.size() < 2) {
                continue;
            }
            problems.add(playerNamesByKey.get(playerEntries.getKey()) + " is named in " + collidingEntries.size()
                    + " teams (" + describeTeamNames(collidingEntries) + "), none of them is imported");
            ambiguousEntries.addAll(collidingEntries);
        }
    }

    private static String describeTeamNames(List<EventTeamEntry> entries) {
        List<String> teamNames = new ArrayList<>();
        for (EventTeamEntry entry : entries) {
            teamNames.add(entry.teamName());
        }
        return String.join(", ", teamNames);
    }

    private static String describeEntry(int entryNumber) {
        return "Entry " + entryNumber + " of the " + TEAMS_KEY + " list";
    }

    private static String readText(JsonElement storedValue) {
        if (storedValue == null || !storedValue.isJsonPrimitive() || !storedValue.getAsJsonPrimitive().isString()) {
            return null;
        }
        String text = storedValue.getAsString().trim();
        return text.isEmpty() ? null : text;
    }

    private static String toKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static String describe(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private static EventTeamImportFile failed(String failureReason) {
        return new EventTeamImportFile(List.of(), List.of(), failureReason);
    }
}
