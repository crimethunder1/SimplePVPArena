package de.simplepvparena.tournament;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class FileTournamentStateStorage implements TournamentStateStorage {

    public static final String FILE_NAME = "tournament.yml";

    private static final int FORMAT_VERSION = 1;
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String EMPTY_SLOT = "";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String STARTED_AT_KEY = "started-at";
    private static final String SAVED_AT_KEY = "saved-at";
    private static final String STATE_KEY = "state";
    private static final String FIRST_ROUND_KEY = "first-round";
    private static final String RESULTS_KEY = "results";
    private static final String MATCH_ID_KEY = "match-id";
    private static final String FIRST_KEY = "first";
    private static final String SECOND_KEY = "second";
    private static final String WINNER_KEY = "winner";
    private static final String DECIDED_BY_KEY = "decided-by";

    private final Path stateFile;

    public FileTournamentStateStorage(Path stateFile) {
        this.stateFile = Objects.requireNonNull(stateFile, "stateFile");
    }

    @Override
    public void save(SavedTournament savedTournament) throws IOException {
        String serializedTournament = serialize(savedTournament);
        Path directory = stateFile.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, stateFile.getFileName().toString(),
                TEMPORARY_FILE_EXTENSION);
        try {
            Files.writeString(temporaryFile, serializedTournament, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, stateFile);
        } catch (IOException | RuntimeException writeFailure) {
            removeTemporaryFile(temporaryFile, writeFailure);
            throw writeFailure;
        }
    }

    @Override
    public TournamentStateLookup find() {
        String fileName = stateFile.getFileName().toString();
        if (!Files.isRegularFile(stateFile)) {
            return TournamentStateLookup.missing(fileName);
        }
        try {
            SavedTournament savedTournament = read(stateFile);
            savedTournament.restoreBracket();
            return TournamentStateLookup.found(savedTournament, fileName);
        } catch (IOException | InvalidConfigurationException | RuntimeException unreadableTournament) {
            return TournamentStateLookup.broken(moveAside(stateFile), describe(unreadableTournament));
        }
    }

    @Override
    public void delete() throws IOException {
        Files.deleteIfExists(stateFile);
    }

    private static String serialize(SavedTournament savedTournament) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        configuration.set(STARTED_AT_KEY, savedTournament.startedAt().toString());
        configuration.set(SAVED_AT_KEY, savedTournament.savedAt().toString());
        configuration.set(STATE_KEY, savedTournament.state().name());
        List<Map<String, Object>> firstRound = new ArrayList<>();
        for (SavedFirstRoundMatch savedMatch : savedTournament.firstRound()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put(MATCH_ID_KEY, savedMatch.matchId());
            entry.put(FIRST_KEY, savedMatch.firstTeamName().orElse(EMPTY_SLOT));
            entry.put(SECOND_KEY, savedMatch.secondTeamName().orElse(EMPTY_SLOT));
            firstRound.add(entry);
        }
        configuration.set(FIRST_ROUND_KEY, firstRound);
        List<Map<String, Object>> results = new ArrayList<>();
        for (SavedMatchResult savedResult : savedTournament.results()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put(MATCH_ID_KEY, savedResult.matchId());
            entry.put(WINNER_KEY, savedResult.winnerTeamName());
            savedResult.decidedBy().ifPresent(decisionKind -> entry.put(DECIDED_BY_KEY,
                    decisionKind.getStoredValue()));
            results.add(entry);
        }
        configuration.set(RESULTS_KEY, results);
        return configuration.saveToString();
    }

    private static SavedTournament read(Path file) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        Object formatVersion = configuration.get(FORMAT_VERSION_KEY);
        if (!(formatVersion instanceof Integer version)) {
            throw new InvalidConfigurationException("missing or unreadable " + FORMAT_VERSION_KEY);
        }
        if (version != FORMAT_VERSION) {
            throw new InvalidConfigurationException("unknown " + FORMAT_VERSION_KEY + " " + version);
        }
        Instant startedAt = readInstant(configuration, STARTED_AT_KEY);
        Instant savedAt = readInstant(configuration, SAVED_AT_KEY);
        TournamentState state = readState(configuration);
        List<SavedFirstRoundMatch> firstRound = new ArrayList<>();
        for (Map<?, ?> entry : readEntries(configuration, FIRST_ROUND_KEY)) {
            String matchId = readText(entry, MATCH_ID_KEY, FIRST_ROUND_KEY);
            firstRound.add(new SavedFirstRoundMatch(matchId, readSlot(entry, FIRST_KEY, matchId),
                    readSlot(entry, SECOND_KEY, matchId)));
        }
        List<SavedMatchResult> results = new ArrayList<>();
        for (Map<?, ?> entry : readEntries(configuration, RESULTS_KEY)) {
            String matchId = readText(entry, MATCH_ID_KEY, RESULTS_KEY);
            results.add(new SavedMatchResult(matchId, readText(entry, WINNER_KEY, matchId),
                    readDecisionKind(entry)));
        }
        return new SavedTournament(startedAt, savedAt, state, firstRound, results);
    }

    private static Instant readInstant(YamlConfiguration configuration, String key)
            throws InvalidConfigurationException {
        Object storedValue = configuration.get(key);
        if (storedValue instanceof Date date) {
            return date.toInstant();
        }
        if (storedValue instanceof String text) {
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException unreadableInstant) {
                throw new InvalidConfigurationException("unreadable " + key + " " + text);
            }
        }
        throw new InvalidConfigurationException("missing or unreadable " + key);
    }

    private static TournamentState readState(YamlConfiguration configuration) throws InvalidConfigurationException {
        Object storedValue = configuration.get(STATE_KEY);
        if (TournamentState.RUNNING.name().equals(storedValue)) {
            return TournamentState.RUNNING;
        }
        if (TournamentState.PAUSED.name().equals(storedValue)) {
            return TournamentState.PAUSED;
        }
        throw new InvalidConfigurationException(STATE_KEY + " must be " + TournamentState.RUNNING + " or "
                + TournamentState.PAUSED + ", not " + storedValue);
    }

    private static List<Map<?, ?>> readEntries(YamlConfiguration configuration, String key)
            throws InvalidConfigurationException {
        if (!(configuration.get(key) instanceof List<?> storedEntries)) {
            throw new InvalidConfigurationException("missing or unreadable " + key);
        }
        List<Map<?, ?>> entries = new ArrayList<>();
        for (Object storedEntry : storedEntries) {
            if (!(storedEntry instanceof Map<?, ?> entry)) {
                throw new InvalidConfigurationException("unreadable entry in " + key + ": " + storedEntry);
            }
            entries.add(entry);
        }
        return entries;
    }

    private static String readText(Map<?, ?> entry, String key, String location)
            throws InvalidConfigurationException {
        if (entry.get(key) instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new InvalidConfigurationException("missing or unreadable " + key + " in " + location);
    }

    private static Optional<String> readSlot(Map<?, ?> entry, String key, String matchId)
            throws InvalidConfigurationException {
        Object storedValue = entry.get(key);
        if (storedValue == null) {
            return Optional.empty();
        }
        if (storedValue instanceof String text) {
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        }
        throw new InvalidConfigurationException("unreadable " + key + " in " + matchId + ": " + storedValue);
    }

    private static Optional<MatchDecisionKind> readDecisionKind(Map<?, ?> entry) {
        if (entry.get(DECIDED_BY_KEY) instanceof String text) {
            return MatchDecisionKind.fromStoredValue(text);
        }
        return Optional.empty();
    }

    private static String describe(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private static void moveIntoPlace(Path temporaryFile, Path targetFile) throws IOException {
        try {
            Files.move(temporaryFile, targetFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporaryFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void removeTemporaryFile(Path temporaryFile, Exception writeFailure) {
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private static String moveAside(Path file) {
        Path brokenFile = file.resolveSibling(file.getFileName() + BROKEN_FILE_EXTENSION);
        try {
            Files.move(file, brokenFile, StandardCopyOption.REPLACE_EXISTING);
            return brokenFile.getFileName().toString();
        } catch (IOException moveFailure) {
            return file.getFileName().toString();
        }
    }
}
