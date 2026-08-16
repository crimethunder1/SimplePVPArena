package de.simplepvparena.team;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTeamImportServiceTest {

    private final Map<String, TeamMember> knownPlayers = new LinkedHashMap<>();
    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final AtomicInteger saveCount = new AtomicInteger();

    EventTeamImportServiceTest() {
        knownPlayer("Notch");
        knownPlayer("Jeb_");
        knownPlayer("Grumm");
        knownPlayer("Dinnerbone");
        knownPlayer("Steve");
        knownPlayer("Alex");
    }

    @Test
    void importsTheTeamsOfTheFileAndWritesThem(@TempDir Path pluginFolder) throws IOException {
        EventTeamImportService importService = serviceFor(pluginFolder, """
                {
                  "teams": [
                    { "team_name": "team_apex", "players": ["Notch", "Jeb_"] },
                    { "team_name": "Team 3", "players": ["Grumm", "Dinnerbone"] }
                  ]
                }
                """);

        EventTeamImportResult result = importService.importTeams();

        assertTrue(result.isImported());
        assertEquals(List.of("team_apex", "Team 3"), teamRegistry.getTeamNames());
        assertEquals(List.of(knownPlayers.get("notch"), knownPlayers.get("jeb_")),
                teamRegistry.findTeam("TEAM_APEX").orElseThrow().getMembers());
        assertEquals(1, saveCount.get());
    }

    @Test
    void replacesEveryTeamThatWasRegisteredBefore(@TempDir Path pluginFolder) throws IOException {
        teamRegistry.registerTeam("Old", knownPlayers.get("steve"), knownPlayers.get("alex"));
        EventTeamImportService importService = serviceFor(pluginFolder, """
                { "teams": [ { "team_name": "Alpha", "players": ["Notch", "Jeb_"] } ] }
                """);

        EventTeamImportResult result = importService.importTeams();

        assertTrue(result.isImported());
        assertEquals(List.of("Alpha"), teamRegistry.getTeamNames());
        assertTrue(result.describe().get(0).contains("the one team that was registered before is gone"));
    }

    @Test
    void skipsATeamWithAPlayerThatCannotBeResolved(@TempDir Path pluginFolder) throws IOException {
        EventTeamImportService importService = serviceFor(pluginFolder, """
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch", "Typo"] },
                    { "team_name": "Bravo", "players": ["Steve", "Alex"] }
                  ]
                }
                """);

        EventTeamImportResult result = importService.importTeams();

        assertTrue(result.isImported());
        assertEquals(List.of("Bravo"), teamRegistry.getTeamNames());
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).contains("Alpha"));
        assertTrue(result.getProblems().get(0).contains("Typo"));
    }

    @Test
    void registersTheSpellingTheLookupAnswersWith(@TempDir Path pluginFolder) throws IOException {
        EventTeamImportService importService = serviceFor(pluginFolder, """
                { "teams": [ { "team_name": "Alpha", "players": ["nOtCh", "JEB_"] } ] }
                """);

        importService.importTeams();

        assertEquals("Notch, Jeb_", teamRegistry.findTeam("Alpha").orElseThrow().getMemberNamesJoined());
    }

    @Test
    void skipsATeamThatTheRegistryRefuses(@TempDir Path pluginFolder) throws IOException {
        TeamMember notch = knownPlayers.get("notch");
        knownPlayers.put("notchsecondname", new TeamMember(notch.uniqueId(), "Notch"));
        EventTeamImportService importService = serviceFor(pluginFolder, """
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch", "Jeb_"] },
                    { "team_name": "Bravo", "players": ["NotchSecondName", "Steve"] }
                  ]
                }
                """);

        EventTeamImportResult result = importService.importTeams();

        assertEquals(List.of("Alpha"), teamRegistry.getTeamNames());
        assertEquals(1, result.getProblems().size());
        assertTrue(result.getProblems().get(0).contains("Bravo"));
    }

    @Test
    void keepsTheRegisteredTeamsWhenTheFileIsMissing(@TempDir Path pluginFolder) {
        teamRegistry.registerTeam("Old", knownPlayers.get("steve"), knownPlayers.get("alex"));
        EventTeamImportService importService = service(pluginFolder);

        EventTeamImportResult result = importService.importTeams();

        assertFalse(result.isImported());
        assertEquals(List.of("Old"), teamRegistry.getTeamNames());
        assertEquals(0, saveCount.get());
        assertTrue(result.describe().get(1).contains("stay exactly as they are"));
    }

    @Test
    void keepsTheRegisteredTeamsWhenNoEntryCanBeImported(@TempDir Path pluginFolder) throws IOException {
        teamRegistry.registerTeam("Old", knownPlayers.get("steve"), knownPlayers.get("alex"));
        EventTeamImportService importService = serviceFor(pluginFolder, """
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Typo", "Jeb_"] },
                    { "team_name": "Bravo", "players": ["Grumm"] }
                  ]
                }
                """);

        EventTeamImportResult result = importService.importTeams();

        assertFalse(result.isImported());
        assertEquals(List.of("Old"), teamRegistry.getTeamNames());
        assertEquals(0, saveCount.get());
        assertEquals(2, result.getProblems().size());
    }

    @Test
    void reportsWhatWasImportedAndWhatWasSkipped(@TempDir Path pluginFolder) throws IOException {
        EventTeamImportService importService = serviceFor(pluginFolder, """
                {
                  "teams": [
                    { "team_name": "Alpha", "players": ["Notch", "Jeb_"] },
                    { "team_name": "Bravo", "players": ["Steve"] }
                  ]
                }
                """);

        List<String> lines = importService.importTeams().describe();

        assertTrue(lines.get(0).contains("1 team imported from " + EventTeamImportFile.FILE_NAME));
        assertTrue(lines.get(1).contains("Alpha"));
        assertTrue(lines.get(2).contains("teams.yml"));
        assertTrue(lines.get(3).contains("1 entry of the file could not be used"));
        assertTrue(lines.get(4).contains("Bravo"));
    }

    @Test
    void saysWhetherTheFileLiesInThePluginFolder(@TempDir Path pluginFolder) throws IOException {
        EventTeamImportService missingFileService = service(pluginFolder);

        assertFalse(missingFileService.isImportFileAvailable());
        assertTrue(missingFileService.describeImportFile().get(0).contains("is not in the plugin folder"));
        assertTrue(missingFileService.describeMissingImportFile().contains(EventTeamImportFile.FILE_NAME));

        EventTeamImportService availableFileService = serviceFor(pluginFolder, "{ \"teams\": [] }");

        assertTrue(availableFileService.isImportFileAvailable());
        assertTrue(availableFileService.describeImportFile().get(0).contains("lies in the plugin folder"));
    }

    private EventTeamImportService serviceFor(Path pluginFolder, String json) throws IOException {
        Files.writeString(pluginFolder.resolve(EventTeamImportFile.FILE_NAME), json, StandardCharsets.UTF_8);
        return service(pluginFolder);
    }

    private EventTeamImportService service(Path pluginFolder) {
        File importFile = new File(pluginFolder.toFile(), EventTeamImportFile.FILE_NAME);
        return new EventTeamImportService(importFile, teamRegistry, this::findKnownPlayer,
                saveCount::incrementAndGet);
    }

    private Optional<TeamMember> findKnownPlayer(String playerName) {
        return Optional.ofNullable(knownPlayers.get(playerName.toLowerCase(Locale.ROOT)));
    }

    private void knownPlayer(String playerName) {
        knownPlayers.put(playerName.toLowerCase(Locale.ROOT), new TeamMember(UUID.randomUUID(), playerName));
    }
}
