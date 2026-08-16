package de.simplepvparena.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTeamImportFileTest {

    @Test
    void readsTheTeamsAndTheirPlayers() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "team_name": "team_apex", "players": ["Notch", "Jeb_"] },
                    { "team_name": "Team 3", "players": ["Grumm", "Dinnerbone"] }
                  ]
                }
                """);

        assertTrue(importFile.isReadable());
        assertEquals(List.of(), importFile.getProblems());
        assertEquals(List.of(new EventTeamEntry("team_apex", "Notch", "Jeb_"),
                new EventTeamEntry("Team 3", "Grumm", "Dinnerbone")), importFile.getEntries());
    }

    @Test
    void readsAFileWithoutASingleTeam() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("{ \"teams\": [] }");

        assertTrue(importFile.isReadable());
        assertEquals(List.of(), importFile.getEntries());
        assertEquals(List.of(), importFile.getProblems());
    }

    @Test
    void skipsAnEntryWithoutATeamNameAndKeepsTheOthers() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "players": ["Notch", "Jeb_"] },
                    { "team_name": "  ", "players": ["Grumm", "Dinnerbone"] },
                    { "team_name": "Bravo", "players": ["Steve", "Alex"] }
                  ]
                }
                """);

        assertTrue(importFile.isReadable());
        assertEquals(List.of("Bravo"), teamNames(importFile));
        assertEquals(2, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("Entry 1"));
        assertTrue(importFile.getProblems().get(0).contains("team_name"));
        assertTrue(importFile.getProblems().get(1).contains("Entry 2"));
    }

    @Test
    void skipsAnEntryWithoutAPlayersList() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "team_name": "Alpha" },
                    { "team_name": "Bravo", "players": "Steve" },
                    { "team_name": "Charlie", "players": ["Steve", 7] }
                  ]
                }
                """);

        assertTrue(importFile.isReadable());
        assertEquals(List.of(), importFile.getEntries());
        assertEquals(3, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("Alpha"));
        assertTrue(importFile.getProblems().get(1).contains("Bravo"));
        assertTrue(importFile.getProblems().get(2).contains("Charlie"));
    }

    @Test
    void skipsATeamThatDoesNotNameExactlyTwoPlayers() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch"] },
                    { "team_name": "Bravo", "players": ["Steve", "Alex", "Grumm"] },
                    { "team_name": "Charlie", "players": ["Jeb_", "Dinnerbone"] }
                  ]
                }
                """);

        assertEquals(List.of("Charlie"), teamNames(importFile));
        assertEquals(2, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("Alpha"));
        assertTrue(importFile.getProblems().get(0).contains("1 player,"));
        assertTrue(importFile.getProblems().get(1).contains("3 players"));
    }

    @Test
    void skipsATeamThatNamesTheSamePlayerTwice() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch", "notch"] },
                    { "team_name": "Bravo", "players": ["Steve", "Alex"] }
                  ]
                }
                """);

        assertEquals(List.of("Bravo"), teamNames(importFile));
        assertEquals(1, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("two different players"));
    }

    @Test
    void skipsEveryEntryOfADuplicateTeamName() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch", "Jeb_"] },
                    { "team_name": "Bravo", "players": ["Steve", "Alex"] },
                    { "team_name": "ALPHA", "players": ["Grumm", "Dinnerbone"] }
                  ]
                }
                """);

        assertEquals(List.of("Bravo"), teamNames(importFile));
        assertEquals(1, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("Alpha"));
        assertTrue(importFile.getProblems().get(0).contains("2 times"));
    }

    @Test
    void skipsEveryTeamOfAPlayerThatStandsInSeveralTeams() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch", "Jeb_"] },
                    { "team_name": "Bravo", "players": ["Steve", "NOTCH"] },
                    { "team_name": "Charlie", "players": ["Grumm", "Dinnerbone"] }
                  ]
                }
                """);

        assertEquals(List.of("Charlie"), teamNames(importFile));
        assertEquals(1, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("Notch"));
        assertTrue(importFile.getProblems().get(0).contains("Alpha"));
        assertTrue(importFile.getProblems().get(0).contains("Bravo"));
    }

    @Test
    void skipsAnEntryThatIsNoJsonObject() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("""
                { "teams": ["Alpha", { "team_name": "Bravo", "players": ["Steve", "Alex"] }] }
                """);

        assertEquals(List.of("Bravo"), teamNames(importFile));
        assertEquals(1, importFile.getProblems().size());
        assertTrue(importFile.getProblems().get(0).contains("Entry 1"));
    }

    @Test
    void reportsAFileThatIsNoJsonAtAll() {
        EventTeamImportFile importFile = EventTeamImportFile.parse("{ \"teams\": [ ");

        assertFalse(importFile.isReadable());
        assertTrue(importFile.getFailureReason().orElseThrow().contains(EventTeamImportFile.FILE_NAME));
        assertEquals(List.of(), importFile.getEntries());
    }

    @Test
    void reportsAFileThatHoldsNoTeamsList() {
        assertFalse(EventTeamImportFile.parse("{ \"players\": [] }").isReadable());
        assertFalse(EventTeamImportFile.parse("{ \"teams\": { } }").isReadable());
        assertFalse(EventTeamImportFile.parse("[]").isReadable());
    }

    @Test
    void readsTheFileOfThePluginFolder(@TempDir Path pluginFolder) throws IOException {
        File file = write(pluginFolder, """
                { "teams": [ { "team_name": "Alpha", "players": ["Notch", "Jeb_"] } ] }
                """);

        EventTeamImportFile importFile = EventTeamImportFile.read(file);

        assertTrue(importFile.isReadable());
        assertEquals(List.of("Alpha"), teamNames(importFile));
    }

    @Test
    void reportsAMissingFileInsteadOfFailing(@TempDir Path pluginFolder) {
        EventTeamImportFile importFile = EventTeamImportFile.read(
                pluginFolder.resolve(EventTeamImportFile.FILE_NAME).toFile());

        assertFalse(importFile.isReadable());
        assertTrue(importFile.getFailureReason().orElseThrow().contains(EventTeamImportFile.FILE_NAME));
    }

    private static List<String> teamNames(EventTeamImportFile importFile) {
        return importFile.getEntries().stream()
                .map(EventTeamEntry::teamName)
                .toList();
    }

    private static File write(Path pluginFolder, String json) throws IOException {
        Path file = pluginFolder.resolve(EventTeamImportFile.FILE_NAME);
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file.toFile();
    }
}
