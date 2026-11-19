package de.simplepvparena.registration;

import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.support.SharedBukkitServer;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamImportServiceTest {

    @TempDir
    Path importFolder;

    private final TeamRegistry teamRegistry = new TeamRegistry();
    private final TeamSizeSetting teamSizeSetting = new TeamSizeSetting();
    private final List<String> adminMessages = new ArrayList<>();
    private final Player admin = createAdmin(adminMessages);
    private final Set<String> unknownNames = new HashSet<>();

    private boolean tournamentInProgress;
    private boolean autoMatchAnnouncedOrRunning;
    private int teamSaveCount;

    @Test
    void listsOnlyTheTextFilesOfTheFolder() throws IOException {
        Files.writeString(importFolder.resolve("b-event.txt"), "Steve");
        Files.writeString(importFolder.resolve("A-event.TXT"), "Steve");
        Files.writeString(importFolder.resolve("notes.md"), "Steve");

        assertEquals(List.of("A-event.TXT", "b-event.txt"), service().listFileNames());
    }

    @Test
    void buildsTheTeamsOfAFileAndReplacesTheFormerTeams() throws IOException {
        teamRegistry.registerTeam("Old", List.of(new TeamMember(UUID.randomUUID(), "Former")));
        writeFile("teams-cup.txt", "Steve Alex", "Notch", "Herobrine Jeb_", "Dinnerbone");

        service().importFile(admin, "teams-cup");

        assertEquals(3, teamRegistry.getTeamCount());
        assertTrue(teamRegistry.findTeam("Old").isEmpty());
        assertTrue(memberNames().contains(Set.of("Steve", "Alex")));
        assertTrue(memberNames().contains(Set.of("Herobrine", "Jeb_")));
        assertTrue(memberNames().contains(Set.of("Notch", "Dinnerbone")));
        assertEquals(1, teamSaveCount);
        assertTrue(adminMessages.getLast().contains("3 teams with 6 players were built from teams-cup.txt"));
    }

    @Test
    void leavesOutUnknownPlayersAndReportsThem() throws IOException {
        unknownNames.add("ghost_1");
        writeFile("cup.txt", "Steve Ghost_1", "Notch", "Alex");

        service().importFile(admin, "cup.txt");

        assertEquals(2, teamRegistry.getTeamCount());
        assertTrue(adminMessages.stream().anyMatch(message -> message.contains("Ghost_1")));
    }

    @Test
    void keepsTheTeamsWhenTheFileGivesTooFewTeams() throws IOException {
        teamRegistry.registerTeam("Old", List.of(new TeamMember(UUID.randomUUID(), "Former")));
        writeFile("cup.txt", "Steve Alex");

        service().importFile(admin, "cup");

        assertEquals(List.of("Old"), teamRegistry.getTeamNames());
        assertEquals(0, teamSaveCount);
    }

    @Test
    void refusesTheImportWhileATournamentIsInProgress() throws IOException {
        tournamentInProgress = true;
        writeFile("cup.txt", "Steve Alex", "Notch Jeb_");

        service().importFile(admin, "cup");

        assertEquals(0, teamRegistry.getTeamCount());
        assertEquals(1, adminMessages.size());
    }

    @Test
    void refusesTheImportWhileAnAutoMatchIsAnnounced() throws IOException {
        autoMatchAnnouncedOrRunning = true;
        writeFile("cup.txt", "Steve Alex", "Notch Jeb_");

        service().importFile(admin, "cup");

        assertEquals(0, teamRegistry.getTeamCount());
    }

    @Test
    void reportsAMissingFile() {
        service().importFile(admin, "../secret");

        assertEquals(0, teamRegistry.getTeamCount());
        assertTrue(adminMessages.getFirst().contains("../secret"));
    }

    @Test
    void usesTheTeamSizeForTheTeams() throws IOException {
        teamSizeSetting.setTeamSize(3);
        writeFile("cup.txt", "Steve Alex", "Notch", "Herobrine Jeb_", "Dinnerbone");

        service().importFile(admin, "cup");

        assertEquals(List.of(3, 3), teamRegistry.getTeams().stream().map(team -> team.getMembers().size())
                .sorted().toList());
    }

    private void writeFile(String fileName, String... lines) throws IOException {
        Files.write(importFolder.resolve(fileName), List.of(lines), StandardCharsets.UTF_8);
    }

    private List<Set<String>> memberNames() {
        List<Set<String>> names = new ArrayList<>();
        for (Team team : teamRegistry.getTeams()) {
            Set<String> teamNames = new HashSet<>();
            team.getMembers().forEach(member -> teamNames.add(member.playerName()));
            names.add(teamNames);
        }
        return names;
    }

    private TeamImportService service() {
        return new TeamImportService(importFolder, teamRegistry, teamSizeSetting, () -> tournamentInProgress,
                () -> autoMatchAnnouncedOrRunning, () -> teamSaveCount++, uniqueId -> null, new Random(5),
                this::resolve, Runnable::run);
    }

    private CompletableFuture<Optional<TeamMember>> resolve(String playerName) {
        if (unknownNames.contains(playerName.toLowerCase(Locale.ROOT))) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.completedFuture(Optional.of(new TeamMember(
                UUID.nameUUIDFromBytes(playerName.getBytes(StandardCharsets.UTF_8)), playerName)));
    }

    private static Player createAdmin(List<String> messages) {
        UUID uniqueId = UUID.randomUUID();
        return SharedBukkitServer.proxy(Player.class, (method, arguments) -> switch (method.getName()) {
            case "getUniqueId" -> uniqueId;
            case "getName" -> "Admin";
            case "sendMessage" -> {
                messages.add(TournamentMessages.toPlainBody((Component) arguments[0]));
                yield null;
            }
            default -> throw new UnsupportedOperationException("Player." + method.getName());
        });
    }
}
