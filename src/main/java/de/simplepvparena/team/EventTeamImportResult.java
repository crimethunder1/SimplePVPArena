package de.simplepvparena.team;

import java.util.ArrayList;
import java.util.List;

public final class EventTeamImportResult {

    private final List<String> importedTeamNames;
    private final List<String> problems;
    private final int replacedTeamCount;
    private final String failureReason;

    private EventTeamImportResult(List<String> importedTeamNames, List<String> problems, int replacedTeamCount,
                                  String failureReason) {
        this.importedTeamNames = List.copyOf(importedTeamNames);
        this.problems = List.copyOf(problems);
        this.replacedTeamCount = replacedTeamCount;
        this.failureReason = failureReason;
    }

    public static EventTeamImportResult imported(List<String> importedTeamNames, int replacedTeamCount,
                                                 List<String> problems) {
        return new EventTeamImportResult(importedTeamNames, problems, replacedTeamCount, null);
    }

    public static EventTeamImportResult failed(String failureReason, List<String> problems) {
        return new EventTeamImportResult(List.of(), problems, 0, failureReason);
    }

    public boolean isImported() {
        return failureReason == null;
    }

    public List<String> getImportedTeamNames() {
        return importedTeamNames;
    }

    public List<String> getProblems() {
        return problems;
    }

    public List<String> describe() {
        List<String> lines = new ArrayList<>();
        if (!isImported()) {
            lines.add(failureReason);
            lines.add("The registered teams stay exactly as they are");
            addProblems(lines);
            return List.copyOf(lines);
        }
        lines.add(importedTeamNames.size() + " " + teams(importedTeamNames.size()) + " imported from "
                + EventTeamImportFile.FILE_NAME + ", " + describeReplacedTeams());
        lines.add("Registered now: " + String.join(", ", importedTeamNames));
        lines.add("teams.yml was written, the imported teams survive a restart");
        addProblems(lines);
        return List.copyOf(lines);
    }

    private void addProblems(List<String> lines) {
        if (problems.isEmpty()) {
            return;
        }
        lines.add(problems.size() + " " + entries(problems.size()) + " of the file could not be used:");
        for (String problem : problems) {
            lines.add("  " + problem);
        }
    }

    private String describeReplacedTeams() {
        if (replacedTeamCount == 0) {
            return "no team was registered before";
        }
        if (replacedTeamCount == 1) {
            return "the one team that was registered before is gone";
        }
        return "the " + replacedTeamCount + " teams that were registered before are gone";
    }

    private static String teams(int teamCount) {
        return teamCount == 1 ? "team" : "teams";
    }

    private static String entries(int entryCount) {
        return entryCount == 1 ? "entry" : "entries";
    }
}
