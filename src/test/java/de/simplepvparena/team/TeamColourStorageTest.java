package de.simplepvparena.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamColourStorageTest {

    @TempDir
    Path pluginFolder;

    private final List<LogRecord> logRecords = new ArrayList<>();
    private final Logger logger = createRecordingLogger();
    private final TeamMember steve = new TeamMember(UUID.randomUUID(), "Steve");
    private final TeamMember alex = new TeamMember(UUID.randomUUID(), "Alex");
    private final TeamMember notch = new TeamMember(UUID.randomUUID(), "Notch");

    @Test
    void aTeamHasNoColourUnlessOneIsGiven() {
        TeamRegistry registry = new TeamRegistry();

        Team team = registry.registerTeam("Alpha", List.of(steve));

        assertEquals(Optional.empty(), team.getColour());
        assertEquals(Optional.empty(), new Team("Bravo", List.of(alex)).getColour());
    }

    @Test
    void registersATeamWithAColour() {
        TeamRegistry registry = new TeamRegistry();

        Team team = registry.registerTeam("Red", List.of(steve), TeamColour.RED);

        assertEquals(Optional.of(TeamColour.RED), team.getColour());
        assertEquals(Optional.of(TeamColour.RED), registry.findTeam("red").orElseThrow().getColour());
    }

    @Test
    void writesAndReadsTheColour() throws IOException {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("LightBlue", List.of(steve, alex), TeamColour.LIGHT_BLUE);
        registry.registerTeam("Manual", List.of(notch));

        storage().save(registry);
        TeamRegistry reloadedRegistry = new TeamRegistry();
        storage().load(reloadedRegistry);

        assertEquals(Optional.of(TeamColour.LIGHT_BLUE),
                reloadedRegistry.findTeam("LightBlue").orElseThrow().getColour());
        assertEquals(Optional.empty(), reloadedRegistry.findTeam("Manual").orElseThrow().getColour());
        String storedTeams = Files.readString(teamsFile(), StandardCharsets.UTF_8);
        assertTrue(storedTeams.contains("  colour: light-blue\n"), storedTeams);
        assertEquals(1, storedTeams.split("colour:", -1).length - 1, "a team without colour writes no colour");
    }

    @Test
    void readsAnOlderFileWithoutColoursUnchanged() throws IOException {
        Files.writeString(teamsFile(), """
                teams:
                - name: Alpha
                  members:
                  - unique-id: %s
                    name: Steve
                - name: Bravo
                  members:
                  - unique-id: %s
                    name: Alex
                """.formatted(steve.uniqueId(), alex.uniqueId()), StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();

        storage().load(registry);

        assertEquals(List.of("Alpha", "Bravo"), registry.getTeamNames());
        assertEquals(Optional.empty(), registry.findTeam("Alpha").orElseThrow().getColour());
        assertEquals(List.of(steve), registry.findTeam("Alpha").orElseThrow().getMembers());
        assertEquals(List.of(), messagesAt(Level.WARNING));
        assertFalse(Files.exists(pluginFolder.resolve("teams.yml.broken")));
    }

    @Test
    void skipsATeamWithAnUnknownColour() throws IOException {
        Files.writeString(teamsFile(), """
                teams:
                - name: Alpha
                  colour: violet
                  members:
                  - unique-id: %s
                    name: Steve
                - name: Bravo
                  colour: red
                  members:
                  - unique-id: %s
                    name: Alex
                """.formatted(steve.uniqueId(), alex.uniqueId()), StandardCharsets.UTF_8);
        TeamRegistry registry = new TeamRegistry();

        storage().load(registry);

        assertEquals(List.of("Bravo"), registry.getTeamNames());
        assertEquals(Optional.of(TeamColour.RED), registry.findTeam("Bravo").orElseThrow().getColour());
        assertTrue(messagesAt(Level.WARNING).get(0).endsWith("Team Alpha: the team has the unknown colour violet"),
                messagesAt(Level.WARNING).get(0));
    }

    @Test
    void aRenamedTeamKeepsItsColour() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Red", List.of(steve), TeamColour.RED);

        Team renamedTeam = registry.renameTeam("Red", "Dragons");

        assertEquals(Optional.of(TeamColour.RED), renamedTeam.getColour());
        assertEquals(Optional.of(TeamColour.RED), registry.findTeam("Dragons").orElseThrow().getColour());
    }

    @Test
    void aTeamKeepsItsColourWhenPlayersAreAddedOrRemoved() {
        TeamRegistry registry = new TeamRegistry();
        registry.registerTeam("Blue", List.of(steve), TeamColour.BLUE);

        registry.addMember("Blue", alex);
        assertEquals(Optional.of(TeamColour.BLUE), registry.findTeam("Blue").orElseThrow().getColour());
        registry.removeMember("Blue", steve.uniqueId());

        assertEquals(Optional.of(TeamColour.BLUE), registry.findTeam("Blue").orElseThrow().getColour());
        assertEquals(List.of(alex), registry.findTeam("Blue").orElseThrow().getMembers());
    }

    private TeamStorage storage() {
        return new TeamStorage(teamsFile().toFile(), logger);
    }

    private Path teamsFile() {
        return pluginFolder.resolve("teams.yml");
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
