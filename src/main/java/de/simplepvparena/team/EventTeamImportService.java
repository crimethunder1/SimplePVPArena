package de.simplepvparena.team;

import de.simplepvparena.message.TournamentMessages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class EventTeamImportService {

    private final File importFile;
    private final TeamRegistry teamRegistry;
    private final Function<String, Optional<TeamMember>> playerLookup;
    private final Runnable teamConfigurationSaver;

    public EventTeamImportService(File importFile,
                                  TeamRegistry teamRegistry,
                                  Function<String, Optional<TeamMember>> playerLookup,
                                  Runnable teamConfigurationSaver) {
        this.importFile = Objects.requireNonNull(importFile, "importFile");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.playerLookup = Objects.requireNonNull(playerLookup, "playerLookup");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
    }

    public boolean isImportFileAvailable() {
        return importFile.isFile();
    }

    public String describeMissingImportFile() {
        return "There is no " + EventTeamImportFile.FILE_NAME + " in the plugin folder"
                + TournamentMessages.LINE_BREAK + "Put the file next to config.yml and click the tile again";
    }

    public List<String> describeImportFile() {
        if (!isImportFileAvailable()) {
            return List.of(EventTeamImportFile.FILE_NAME + " is not in the plugin folder",
                    "Put it next to config.yml, a click cannot import anything yet");
        }
        return List.of(EventTeamImportFile.FILE_NAME + " lies in the plugin folder");
    }

    public EventTeamImportResult importTeams() {
        EventTeamImportFile importedFile = EventTeamImportFile.read(importFile);
        List<String> problems = new ArrayList<>(importedFile.getProblems());
        if (!importedFile.isReadable()) {
            return EventTeamImportResult.failed(importedFile.getFailureReason().orElseThrow(), problems);
        }
        TeamRegistry importedTeams = new TeamRegistry();
        for (EventTeamEntry entry : importedFile.getEntries()) {
            registerImportedTeam(importedTeams, entry, problems);
        }
        if (importedTeams.getTeamCount() == 0) {
            return EventTeamImportResult.failed("Not a single team of " + EventTeamImportFile.FILE_NAME
                    + " could be imported", problems);
        }
        int replacedTeamCount = teamRegistry.getTeamCount();
        teamRegistry.clear();
        for (Team importedTeam : importedTeams.getTeams()) {
            List<TeamMember> members = importedTeam.getMembers();
            teamRegistry.registerTeam(importedTeam.getTeamName(), members.get(0), members.get(1));
        }
        teamConfigurationSaver.run();
        return EventTeamImportResult.imported(teamRegistry.getTeamNames(), replacedTeamCount, problems);
    }

    private void registerImportedTeam(TeamRegistry importedTeams, EventTeamEntry entry, List<String> problems) {
        Optional<TeamMember> firstMember = playerLookup.apply(entry.firstPlayerName());
        if (firstMember.isEmpty()) {
            problems.add(describeUnknownPlayer(entry, entry.firstPlayerName()));
            return;
        }
        Optional<TeamMember> secondMember = playerLookup.apply(entry.secondPlayerName());
        if (secondMember.isEmpty()) {
            problems.add(describeUnknownPlayer(entry, entry.secondPlayerName()));
            return;
        }
        try {
            importedTeams.registerTeam(entry.teamName(), firstMember.get(), secondMember.get());
        } catch (IllegalArgumentException | IllegalStateException failure) {
            problems.add("Team " + entry.teamName() + ": " + failure.getMessage());
        }
    }

    private static String describeUnknownPlayer(EventTeamEntry entry, String playerName) {
        return "Team " + entry.teamName() + ": unknown player " + playerName
                + ", the player has to be online or known to the server";
    }
}
