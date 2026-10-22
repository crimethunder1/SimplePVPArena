package de.simplepvparena.registration;

import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMessage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileRegistrationStorage {

    public static final String FILE_NAME = "registrations.yml";

    private static final int FORMAT_VERSION = 1;
    private static final String BROKEN_FILE_EXTENSION = ".broken";
    private static final String TEMPORARY_FILE_EXTENSION = ".tmp";
    private static final String SEPARATOR = ".";
    private static final String FORMAT_VERSION_KEY = "format-version";
    private static final String PLAYERS_KEY = "players";
    private static final String PLAYER_UNIQUE_ID_KEY = "unique-id";
    private static final String PLAYER_NAME_KEY = "name";
    private static final String STATIONS_KEY = "stations";
    private static final String STATION_WORLD_KEY = "world";
    private static final String STATION_X_KEY = "x";
    private static final String STATION_Y_KEY = "y";
    private static final String STATION_Z_KEY = "z";

    private final Path registrationFile;
    private final Logger logger;
    private final Consumer<String> adminNotifier;

    public FileRegistrationStorage(Path registrationFile, Logger logger, Consumer<String> adminNotifier) {
        this.registrationFile = Objects.requireNonNull(registrationFile, "registrationFile");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.adminNotifier = Objects.requireNonNull(adminNotifier, "adminNotifier");
    }

    public void load(RegistrationList registrationList, RegistrationStations registrationStations) {
        registrationList.clear();
        registrationStations.clear();
        String fileName = registrationFile.getFileName().toString();
        if (!Files.isRegularFile(registrationFile)) {
            logger.info("No " + fileName + " found, starting without registrations and registration stations");
            return;
        }
        YamlConfiguration configuration;
        List<RegistrationEntry> entries;
        List<RegistrationStation> stations;
        try {
            configuration = read();
            entries = readPlayers(configuration.get(PLAYERS_KEY));
            stations = readStations(configuration.get(STATIONS_KEY));
        } catch (IOException | InvalidConfigurationException | RuntimeException unreadableFile) {
            reportUnreadableFile(describe(unreadableFile), moveAside());
            return;
        }
        for (RegistrationEntry entry : entries) {
            registrationList.register(entry.uniqueId(), entry.playerName());
        }
        stations.forEach(registrationStations::add);
        logger.info("Loaded " + registrationList.getCount() + " registrations and " + stations.size()
                + " registration stations from " + fileName);
    }

    public boolean save(RegistrationList registrationList, RegistrationStations registrationStations) {
        String fileName = registrationFile.getFileName().toString();
        String serializedRegistrations = serialize(registrationList, registrationStations);
        Path targetFile = registrationFile.toAbsolutePath();
        Path directory = targetFile.getParent();
        Path temporaryFile = null;
        try {
            Files.createDirectories(directory);
            temporaryFile = Files.createTempFile(directory, fileName, TEMPORARY_FILE_EXTENSION);
            Files.writeString(temporaryFile, serializedRegistrations, StandardCharsets.UTF_8);
            moveIntoPlace(temporaryFile, targetFile);
            return true;
        } catch (IOException | RuntimeException failure) {
            removeTemporaryFile(temporaryFile, failure);
            logger.log(Level.SEVERE, "Could not write " + fileName, failure);
            return false;
        }
    }

    private static String serialize(RegistrationList registrationList, RegistrationStations registrationStations) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set(FORMAT_VERSION_KEY, FORMAT_VERSION);
        List<Map<String, Object>> storedPlayers = new ArrayList<>();
        for (RegistrationEntry entry : registrationList.getEntries()) {
            Map<String, Object> storedPlayer = new LinkedHashMap<>();
            storedPlayer.put(PLAYER_UNIQUE_ID_KEY, entry.uniqueId().toString());
            storedPlayer.put(PLAYER_NAME_KEY, entry.playerName());
            storedPlayers.add(storedPlayer);
        }
        configuration.set(PLAYERS_KEY, storedPlayers);
        List<Map<String, Object>> storedStations = new ArrayList<>();
        for (RegistrationStation station : registrationStations.stations()) {
            Map<String, Object> storedStation = new LinkedHashMap<>();
            storedStation.put(STATION_WORLD_KEY, station.worldName());
            storedStation.put(STATION_X_KEY, station.blockX());
            storedStation.put(STATION_Y_KEY, station.blockY());
            storedStation.put(STATION_Z_KEY, station.blockZ());
            storedStations.add(storedStation);
        }
        configuration.set(STATIONS_KEY, storedStations);
        return configuration.saveToString();
    }

    private YamlConfiguration read() throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(Files.readString(registrationFile, StandardCharsets.UTF_8));
        if (!(configuration.get(FORMAT_VERSION_KEY) instanceof Integer formatVersion)
                || formatVersion != FORMAT_VERSION) {
            throw new InvalidConfigurationException("missing or unknown " + FORMAT_VERSION_KEY);
        }
        return configuration;
    }

    private static List<RegistrationEntry> readPlayers(Object storedPlayers) throws InvalidConfigurationException {
        if (storedPlayers == null) {
            return List.of();
        }
        if (!(storedPlayers instanceof List<?> playerEntries)) {
            throw new InvalidConfigurationException("the " + PLAYERS_KEY + " entry is no list");
        }
        List<RegistrationEntry> entries = new ArrayList<>();
        Set<UUID> readUniqueIds = new HashSet<>();
        int entryNumber = 0;
        for (Object playerEntry : playerEntries) {
            entryNumber++;
            RegistrationEntry entry = readPlayer(playerEntry, entryNumber);
            if (!readUniqueIds.add(entry.uniqueId())) {
                throw new InvalidConfigurationException("entry " + entryNumber + " of " + PLAYERS_KEY
                        + " names the player " + entry.uniqueId() + " a second time");
            }
            entries.add(entry);
        }
        return entries;
    }

    private static RegistrationEntry readPlayer(Object playerEntry, int entryNumber)
            throws InvalidConfigurationException {
        String entryDescription = "entry " + entryNumber + " of " + PLAYERS_KEY;
        if (!(playerEntry instanceof Map<?, ?> storedPlayer)) {
            throw new InvalidConfigurationException(entryDescription + " is no player");
        }
        String uniqueIdText = readText(storedPlayer.get(PLAYER_UNIQUE_ID_KEY));
        String playerName = readText(storedPlayer.get(PLAYER_NAME_KEY));
        if (uniqueIdText == null || playerName == null) {
            throw new InvalidConfigurationException(entryDescription + " has no readable " + PLAYER_UNIQUE_ID_KEY
                    + " or " + PLAYER_NAME_KEY);
        }
        try {
            return new RegistrationEntry(UUID.fromString(uniqueIdText), playerName);
        } catch (IllegalArgumentException invalidUniqueId) {
            throw new InvalidConfigurationException(entryDescription + " has the invalid " + PLAYER_UNIQUE_ID_KEY
                    + " " + uniqueIdText);
        }
    }

    private static List<RegistrationStation> readStations(Object storedStations)
            throws InvalidConfigurationException {
        if (storedStations == null) {
            return List.of();
        }
        if (!(storedStations instanceof List<?> stationEntries)) {
            throw new InvalidConfigurationException("the " + STATIONS_KEY + " entry is no list");
        }
        List<RegistrationStation> stations = new ArrayList<>();
        int entryNumber = 0;
        for (Object stationEntry : stationEntries) {
            entryNumber++;
            RegistrationStation station = readStation(stationEntry, entryNumber);
            if (stations.contains(station)) {
                throw new InvalidConfigurationException("entry " + entryNumber + " of " + STATIONS_KEY
                        + " names a station a second time");
            }
            stations.add(station);
        }
        return stations;
    }

    private static RegistrationStation readStation(Object stationEntry, int entryNumber)
            throws InvalidConfigurationException {
        String entryDescription = "entry " + entryNumber + " of " + STATIONS_KEY;
        if (!(stationEntry instanceof Map<?, ?> storedStation)) {
            throw new InvalidConfigurationException(entryDescription + " is no station");
        }
        String worldName = readText(storedStation.get(STATION_WORLD_KEY));
        if (worldName == null || !(storedStation.get(STATION_X_KEY) instanceof Integer blockX)
                || !(storedStation.get(STATION_Y_KEY) instanceof Integer blockY)
                || !(storedStation.get(STATION_Z_KEY) instanceof Integer blockZ)) {
            throw new InvalidConfigurationException(entryDescription + " has no readable " + STATION_WORLD_KEY
                    + ", " + STATION_X_KEY + ", " + STATION_Y_KEY + " or " + STATION_Z_KEY);
        }
        return new RegistrationStation(worldName, blockX, blockY, blockZ);
    }

    private static String readText(Object storedValue) {
        if (storedValue instanceof String text && !text.isBlank()) {
            return text;
        }
        return null;
    }

    private void reportUnreadableFile(String reason, Optional<String> brokenFileName) {
        Placeholder file = Placeholder.of("file", registrationFile.getFileName().toString());
        String notice = brokenFileName
                .map(name -> Messages.text(RegistrationMessage.FILE_SET_ASIDE, file, Placeholder.of("reason", reason),
                        Placeholder.of("brokenFile", name)))
                .orElseGet(() -> Messages.text(RegistrationMessage.FILE_NOT_SET_ASIDE, file,
                        Placeholder.of("reason", reason)));
        logger.warning(notice);
        adminNotifier.accept(notice);
    }

    private Optional<String> moveAside() {
        Path brokenFile = registrationFile.resolveSibling(registrationFile.getFileName() + BROKEN_FILE_EXTENSION);
        if (Files.exists(brokenFile)) {
            brokenFile = registrationFile.resolveSibling(registrationFile.getFileName() + SEPARATOR
                    + System.currentTimeMillis() + BROKEN_FILE_EXTENSION);
        }
        try {
            Files.move(registrationFile, brokenFile);
            return Optional.of(brokenFile.getFileName().toString());
        } catch (IOException | RuntimeException moveFailure) {
            return Optional.empty();
        }
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
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException deleteFailure) {
            writeFailure.addSuppressed(deleteFailure);
        }
    }

    private static String describe(Exception failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
