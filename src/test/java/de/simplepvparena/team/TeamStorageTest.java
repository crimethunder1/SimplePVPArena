package de.simplepvparena.team;

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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamStorageTest {

    private static final String LINE_BREAK = System.lineSeparator();

    @TempDir
    Path pluginFolder;

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();

    @Test
    void writesAndReadsTeamsOfOneToEightPlayers() {
        TeamRegistry registry = new TeamRegistry();
        for (int memberCount = 1; memberCount <= 8; memberCount++) {
            registry.registerTeam("Team" + memberCount, createMembers("Team" + memberCount, memberCount));
        }

        storage().save(registry);
        TeamRegistry reloadedRegistry = new TeamRegistry();
        storage().load(reloadedRegistry);

        assertEquals(registry.getTeamNames(), reloadedRegistry.getTeamNames());
        for (Team team : registry.getTeams()) {
            assertEquals(team.getMembers(),
                    reloadedRegistry.findTeam(team.getTeamName()).orElseThrow().getMembers());
        }
        assertFalse(Files.exists(brokenFile()));
        assertEquals(List.of(), messagesAt(Level.WARNING));
        assertEquals(List.of("Loaded 8 teams from teams.yml"), messagesAt(Level.INFO));
    }

    @Test
    void skipsAnUnreadableEntryKeepsTheReadableTeamsAndCopiesTheFileAside() throws IOException {
        String storedTeams = storedTeamsWithAnInvalidUniqueId();
        Files.writeString(teamsFile(), storedTeams, StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();

        storage().load(registry);

        assertEquals(List.of("Alpha", "Charlie"), registry.getTeamNames());
        assertArrayEquals(Files.readAllBytes(teamsFile()), Files.readAllBytes(brokenFile()));
        assertEquals(List.of("1 entry of teams.yml could not be read and was skipped, the file as it was read is "
                + "kept as teams.yml.broken:" + LINE_BREAK
                + "  Team Bravo: member 1 has the invalid unique-id not-a-uuid"), messagesAt(Level.WARNING));
        assertEquals(List.of("Loaded 2 teams from teams.yml"), messagesAt(Level.INFO));
    }

    @Test
    void keepsTheCopyWhenTheTeamsAreSavedAgain() throws IOException {
        String storedTeams = storedTeamsWithAnInvalidUniqueId();
        Files.writeString(teamsFile(), storedTeams, StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();
        storage().load(registry);
        logRecords.clear();

        storage().save(registry);
        TeamRegistry reloadedRegistry = new TeamRegistry();
        storage().load(reloadedRegistry);

        assertEquals(storedTeams, Files.readString(brokenFile(), StandardCharsets.UTF_8));
        assertFalse(Files.readString(teamsFile(), StandardCharsets.UTF_8).contains("Bravo"));
        assertEquals(List.of("Alpha", "Charlie"), reloadedRegistry.getTeamNames());
        assertEquals(List.of(), messagesAt(Level.WARNING));
    }

    @Test
    void overwritesAnOlderCopy() throws IOException {
        Files.writeString(brokenFile(), "an older copy", StandardCharsets.UTF_8);
        String storedTeams = storedTeamsWithAnInvalidUniqueId();
        Files.writeString(teamsFile(), storedTeams, StandardCharsets.UTF_8);

        storage().load(new TeamRegistry());

        assertEquals(storedTeams, Files.readString(brokenFile(), StandardCharsets.UTF_8));
    }

    @Test
    void namesTheNumberOfSkippedEntriesAndEveryReasonInItsOwnLine() throws IOException {
        Files.writeString(teamsFile(), """
                teams:
                - name: Alpha
                  members:
                  - unique-id: %s
                    name: Steve
                - name: Alpha
                  members:
                  - unique-id: %s
                    name: Alex
                - name: Bravo
                  members: []
                - members:
                  - unique-id: %s
                    name: Notch
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()), StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();

        storage().load(registry);

        assertEquals(List.of("Alpha"), registry.getTeamNames());
        assertEquals(List.of(String.join(LINE_BREAK,
                "3 entries of teams.yml could not be read and were skipped, the file as it was read is kept as "
                        + "teams.yml.broken:",
                "  Team Alpha: a team named Alpha already exists",
                "  Team Bravo: a team has 1 to 8 players",
                "  Entry 4 of the teams list: the team has no readable name")), messagesAt(Level.WARNING));
    }

    @Test
    void makesNoCopyOfAFileWithoutUnreadableEntries() throws IOException {
        Files.writeString(teamsFile(), """
                teams:
                - name: Alpha
                  members:
                  - unique-id: %s
                    name: Steve
                """.formatted(UUID.randomUUID()), StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();

        storage().load(registry);

        assertEquals(List.of("Alpha"), registry.getTeamNames());
        assertFalse(Files.exists(brokenFile()));
        assertEquals(List.of(), messagesAt(Level.WARNING));
    }

    @Test
    void copiesAFileThatIsNoReadableYamlAsideAndStartsWithoutTeams() throws IOException {
        Files.writeString(teamsFile(), "teams:\n- name: Alpha\n  members: [ {\n", StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Bravo", createMembers("Bravo", 2));

        storage().load(registry);

        assertEquals(0, registry.getTeamCount());
        assertArrayEquals(Files.readAllBytes(teamsFile()), Files.readAllBytes(brokenFile()));
        List<String> warnings = messagesAt(Level.WARNING);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().startsWith("teams.yml could not be read ("), warnings.getFirst());
        assertTrue(warnings.getFirst().endsWith("), not a single team was loaded, the file as it was read is kept "
                + "as teams.yml.broken"), warnings.getFirst());
    }

    @Test
    void startsWithoutTeamsWhenThereIsNoFile() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Bravo", createMembers("Bravo", 2));

        storage().load(registry);

        assertEquals(0, registry.getTeamCount());
        assertFalse(Files.exists(brokenFile()));
        assertEquals(List.of("No teams.yml found, starting with an empty team registry"), messagesAt(Level.INFO));
    }

    @Test
    void replacesTheFileWithoutLeavingATemporaryFileBehind() throws IOException {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", createMembers("Alpha", 1));
        storage().save(registry);
        registry.registerTeam("Bravo", createMembers("Bravo", 3));

        storage().save(registry);

        assertEquals(List.of("teams.yml"), listFolder(pluginFolder));
        assertTrue(Files.readString(teamsFile(), StandardCharsets.UTF_8).contains("Bravo"));
        assertEquals(List.of(), messagesAt(Level.SEVERE));
    }

    @Test
    void createsTheMissingFolderOfTheFile() {
        Path nestedTeamsFile = pluginFolder.resolve("nested").resolve("teams.yml");
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", createMembers("Alpha", 2));

        new TeamStorage(nestedTeamsFile.toFile(), logger).save(registry);

        assertTrue(Files.isRegularFile(nestedTeamsFile));
    }

    @Test
    void reportsAFailedWriteWithoutThrowingAndLeavesNoTemporaryFileBehind() throws IOException {
        Files.createDirectories(teamsFile());
        Files.writeString(teamsFile().resolve("kept.txt"), "kept", StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", createMembers("Alpha", 2));

        assertDoesNotThrow(() -> storage().save(registry));

        assertEquals(List.of("Could not write teams.yml"), messagesAt(Level.SEVERE));
        assertEquals(List.of("teams.yml"), listFolder(pluginFolder));
        assertEquals("kept", Files.readString(teamsFile().resolve("kept.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void reportsAFolderThatCannotBeCreatedWithoutThrowing() throws IOException {
        Path blockingFile = pluginFolder.resolve("blocked");
        Files.writeString(blockingFile, "no folder", StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Alpha", createMembers("Alpha", 2));

        assertDoesNotThrow(() -> new TeamStorage(blockingFile.resolve("teams.yml").toFile(), logger).save(registry));

        assertEquals(List.of("Could not create the directory of teams.yml, teams are not persisted"),
                messagesAt(Level.SEVERE));
        assertEquals("no folder", Files.readString(blockingFile, StandardCharsets.UTF_8));
    }

    private String storedTeamsWithAnInvalidUniqueId() {
        return """
                teams:
                - name: Alpha
                  members:
                  - unique-id: %s
                    name: Steve
                - name: Bravo
                  members:
                  - unique-id: not-a-uuid
                    name: Alex
                - name: Charlie
                  members:
                  - unique-id: %s
                    name: Notch
                  - unique-id: %s
                    name: Jeb_
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private TeamStorage storage() {
        return new TeamStorage(teamsFile().toFile(), logger);
    }

    private Path teamsFile() {
        return pluginFolder.resolve("teams.yml");
    }

    private Path brokenFile() {
        return pluginFolder.resolve("teams.yml.broken");
    }

    private List<String> messagesAt(Level level) {
        return logRecords.stream()
                .filter(logRecord -> logRecord.getLevel().equals(level))
                .map(LogRecord::getMessage)
                .toList();
    }

    private static List<String> listFolder(Path folder) throws IOException {
        try (Stream<Path> entries = Files.list(folder)) {
            return entries.map(entry -> entry.getFileName().toString()).sorted().toList();
        }
    }

    private static List<TeamMember> createMembers(String teamName, int memberCount) {
        List<TeamMember> members = new ArrayList<>();
        for (int memberNumber = 1; memberNumber <= memberCount; memberNumber++) {
            members.add(new TeamMember(UUID.randomUUID(), teamName + "Player" + memberNumber));
        }
        return members;
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
