package de.simplepvparena.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRegistrationStorageTest {

    @TempDir
    Path pluginFolder;

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final List<String> adminNotifications = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void startsWithoutRegistrationsWhenTheFileIsMissing() {
        RegistrationList registrationList = new RegistrationList();
        registrationList.register(UUID.randomUUID(), "Steve");

        storage().load(registrationList, new RegistrationStations());

        assertEquals(0, registrationList.getCount());
        assertEquals(List.of("No registrations.yml found, starting without registrations and registration stations"),
                messagesAt(Level.INFO));
        assertEquals(List.of(), adminNotifications);
    }

    @Test
    void writesAndReadsTheRegistrationsInTheirOrder() {
        RegistrationList registrationList = new RegistrationList();
        UUID notchUniqueId = UUID.randomUUID();
        UUID steveUniqueId = UUID.randomUUID();
        UUID alexUniqueId = UUID.randomUUID();
        registrationList.register(notchUniqueId, "Notch");
        registrationList.register(steveUniqueId, "Steve");
        registrationList.register(alexUniqueId, "Alex");

        assertTrue(storage().save(registrationList, new RegistrationStations()));
        RegistrationList reloadedList = new RegistrationList();
        storage().load(reloadedList, new RegistrationStations());

        assertEquals(List.of(new RegistrationEntry(notchUniqueId, "Notch"),
                new RegistrationEntry(steveUniqueId, "Steve"),
                new RegistrationEntry(alexUniqueId, "Alex")), reloadedList.getEntries());
        assertEquals(List.of("Loaded 3 registrations and 0 registration stations from registrations.yml"),
                messagesAt(Level.INFO));
        assertEquals(List.of(), messagesAt(Level.WARNING));
    }

    @Test
    void writesAnEmptyListThatCanBeReadAgain() throws IOException {
        storage().save(new RegistrationList(), new RegistrationStations());
        RegistrationList reloadedList = new RegistrationList();

        storage().load(reloadedList, new RegistrationStations());

        assertEquals(0, reloadedList.getCount());
        assertEquals("format-version: 1\nplayers: []\nstations: []\n",
                Files.readString(registrationFile(), StandardCharsets.UTF_8));
    }

    @Test
    void writesAndReadsTheStationsInTheirOrder() throws IOException {
        RegistrationStations stations = new RegistrationStations();
        stations.add(new RegistrationStation("world", 10, 64, -20));
        stations.add(new RegistrationStation("event", -3, 70, 5));

        storage().save(new RegistrationList(), stations);
        RegistrationStations reloadedStations = new RegistrationStations();
        reloadedStations.add(new RegistrationStation("lobby", 0, 0, 0));
        storage().load(new RegistrationList(), reloadedStations);

        assertEquals(List.of(new RegistrationStation("world", 10, 64, -20),
                new RegistrationStation("event", -3, 70, 5)), reloadedStations.stations());
        assertTrue(Files.readString(registrationFile(), StandardCharsets.UTF_8).contains("""
                stations:
                - world: world
                  x: 10
                  y: 64
                  z: -20
                """));
    }

    @Test
    void treatsAStationWithoutCoordinatesAsUnreadable() throws IOException {
        Files.writeString(registrationFile(), """
                format-version: 1
                players: []
                stations:
                - world: world
                  x: 10
                  y: 64
                """, StandardCharsets.UTF_8);
        RegistrationStations stations = new RegistrationStations();
        stations.add(new RegistrationStation("lobby", 0, 0, 0));

        storage().load(new RegistrationList(), stations);

        assertEquals(List.of(), stations.stations());
        assertEquals(List.of("The file registrations.yml could not be read (entry 1 of stations has no readable "
                + "world, x, y or z), it was set aside as registrations.yml.broken, the registrations and "
                + "registration stations start empty"), adminNotifications);
    }

    @Test
    void leavesNoTemporaryFileBehind() throws IOException {
        RegistrationList registrationList = new RegistrationList();
        registrationList.register(UUID.randomUUID(), "Steve");

        storage().save(registrationList, new RegistrationStations());
        storage().save(registrationList, new RegistrationStations());

        assertEquals(List.of("registrations.yml"), listFolder());
    }

    @Test
    void setsAnUnreadableFileAsideAndWarnsTheLogAndTheAdmins() throws IOException {
        Files.writeString(registrationFile(), "players: [unclosed", StandardCharsets.UTF_8);
        RegistrationList registrationList = new RegistrationList();
        registrationList.register(UUID.randomUUID(), "Steve");

        storage().load(registrationList, new RegistrationStations());

        assertEquals(0, registrationList.getCount());
        assertFalse(Files.exists(registrationFile()));
        assertEquals("players: [unclosed", Files.readString(brokenFile(), StandardCharsets.UTF_8));
        assertEquals(1, adminNotifications.size());
        assertTrue(adminNotifications.get(0).startsWith("The file registrations.yml could not be read ("),
                adminNotifications.get(0));
        assertTrue(adminNotifications.get(0).endsWith("), it was set aside as registrations.yml.broken, the "
                + "registrations and registration stations start empty"), adminNotifications.get(0));
        assertEquals(adminNotifications, messagesAt(Level.WARNING));
    }

    @Test
    void keepsAnOlderBrokenFileAndSetsTheNewOneAsideUnderAnotherName() throws IOException {
        Files.writeString(brokenFile(), "an older copy", StandardCharsets.UTF_8);
        Files.writeString(registrationFile(), "players: [unclosed", StandardCharsets.UTF_8);

        storage().load(new RegistrationList(), new RegistrationStations());

        assertEquals("an older copy", Files.readString(brokenFile(), StandardCharsets.UTF_8));
        List<String> files = listFolder();
        assertEquals(2, files.size(), files.toString());
        String newBrokenFile = files.get(0);
        assertTrue(newBrokenFile.matches("registrations\\.yml\\.\\d+\\.broken"), newBrokenFile);
        assertEquals("players: [unclosed", Files.readString(pluginFolder.resolve(newBrokenFile),
                StandardCharsets.UTF_8));
        assertTrue(adminNotifications.get(0).contains("it was set aside as " + newBrokenFile),
                adminNotifications.get(0));
    }

    @Test
    void treatsAFileWithoutFormatVersionAsUnreadable() throws IOException {
        Files.writeString(registrationFile(), "players: []\n", StandardCharsets.UTF_8);

        storage().load(new RegistrationList(), new RegistrationStations());

        assertTrue(Files.exists(brokenFile()));
        assertEquals(List.of("The file registrations.yml could not be read (missing or unknown format-version), it "
                + "was set aside as registrations.yml.broken, the registrations and registration stations start "
                + "empty"), adminNotifications);
    }

    @Test
    void treatsAnInvalidUniqueIdAsUnreadable() throws IOException {
        Files.writeString(registrationFile(), """
                format-version: 1
                players:
                - unique-id: %s
                  name: Steve
                - unique-id: not-a-uuid
                  name: Alex
                """.formatted(UUID.randomUUID()), StandardCharsets.UTF_8);
        RegistrationList registrationList = new RegistrationList();

        storage().load(registrationList, new RegistrationStations());

        assertEquals(0, registrationList.getCount());
        assertEquals(List.of("The file registrations.yml could not be read (entry 2 of players has the invalid "
                + "unique-id not-a-uuid), it was set aside as registrations.yml.broken, the registrations and "
                + "registration stations start empty"), adminNotifications);
    }

    @Test
    void treatsAPlayerNamedTwiceAsUnreadable() throws IOException {
        UUID uniqueId = UUID.randomUUID();
        Files.writeString(registrationFile(), """
                format-version: 1
                players:
                - unique-id: %s
                  name: Steve
                - unique-id: %s
                  name: Steve
                """.formatted(uniqueId, uniqueId), StandardCharsets.UTF_8);

        storage().load(new RegistrationList(), new RegistrationStations());

        assertEquals(List.of("The file registrations.yml could not be read (entry 2 of players names the player "
                + uniqueId + " a second time), it was set aside as registrations.yml.broken, the registrations "
                + "and registration stations start empty"), adminNotifications);
    }

    @Test
    void writesANewFileNextToTheBrokenOne() throws IOException {
        Files.writeString(registrationFile(), "players: [unclosed", StandardCharsets.UTF_8);
        RegistrationList registrationList = new RegistrationList();
        storage().load(registrationList, new RegistrationStations());
        registrationList.register(UUID.randomUUID(), "Steve");

        storage().save(registrationList, new RegistrationStations());
        RegistrationList reloadedList = new RegistrationList();
        storage().load(reloadedList, new RegistrationStations());

        assertEquals(1, reloadedList.getCount());
        assertEquals("players: [unclosed", Files.readString(brokenFile(), StandardCharsets.UTF_8));
    }

    @Test
    void reportsAWriteFailureAndKeepsTheFolderClean() throws IOException {
        Files.createDirectories(registrationFile());
        Files.writeString(registrationFile().resolve("blocker"), "blocks the move", StandardCharsets.UTF_8);
        RegistrationList registrationList = new RegistrationList();
        registrationList.register(UUID.randomUUID(), "Steve");

        assertFalse(storage().save(registrationList, new RegistrationStations()));

        assertEquals(List.of("Could not write registrations.yml"), messagesAt(Level.SEVERE));
        assertEquals(List.of("registrations.yml"), listFolder());
    }

    private FileRegistrationStorage storage() {
        return new FileRegistrationStorage(registrationFile(), logger, adminNotifications::add);
    }

    private Path registrationFile() {
        return pluginFolder.resolve("registrations.yml");
    }

    private Path brokenFile() {
        return pluginFolder.resolve("registrations.yml.broken");
    }

    private List<String> listFolder() throws IOException {
        try (Stream<Path> entries = Files.list(pluginFolder)) {
            return entries.map(entry -> entry.getFileName().toString()).sorted().toList();
        }
    }

    private List<String> messagesAt(Level level) {
        return logRecords.stream()
                .filter(logRecord -> logRecord.getLevel().equals(level))
                .map(LogRecord::getMessage)
                .toList();
    }

    private Logger createRecordingLogger() {
        Logger recordingLogger = Logger.getAnonymousLogger();
        recordingLogger.setUseParentHandlers(false);
        recordingLogger.setLevel(Level.ALL);
        recordingLogger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord logRecord) {
                logRecords.add(logRecord);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return recordingLogger;
    }
}
