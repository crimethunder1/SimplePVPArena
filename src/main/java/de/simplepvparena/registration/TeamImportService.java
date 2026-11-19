package de.simplepvparena.registration;

import de.simplepvparena.message.FailureDescriptions;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.RegistrationMessage;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

@NullMarked
public final class TeamImportService {

    public static final String FOLDER_NAME = "imports";
    public static final String FILE_SUFFIX = ".txt";

    private static final int LISTED_NAME_LIMIT = 20;

    private final Path importFolder;
    private final TeamRegistry teamRegistry;
    private final TeamSizeSetting teamSizeSetting;
    private final BooleanSupplier tournamentInProgress;
    private final BooleanSupplier autoMatchAnnouncedOrRunning;
    private final Runnable teamConfigurationSaver;
    private final Function<UUID, Player> onlinePlayerLookup;
    private final RandomGenerator randomGenerator;
    private final PlayerNameResolver nameResolver;
    private final Executor mainThreadExecutor;
    private boolean importRunning;

    public TeamImportService(Path importFolder, TeamRegistry teamRegistry, TeamSizeSetting teamSizeSetting,
                             BooleanSupplier tournamentInProgress, BooleanSupplier autoMatchAnnouncedOrRunning,
                             Runnable teamConfigurationSaver, Function<UUID, Player> onlinePlayerLookup,
                             RandomGenerator randomGenerator, PlayerNameResolver nameResolver,
                             Executor mainThreadExecutor) {
        this.importFolder = Objects.requireNonNull(importFolder, "importFolder");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
        this.tournamentInProgress = Objects.requireNonNull(tournamentInProgress, "tournamentInProgress");
        this.autoMatchAnnouncedOrRunning = Objects.requireNonNull(autoMatchAnnouncedOrRunning,
                "autoMatchAnnouncedOrRunning");
        this.teamConfigurationSaver = Objects.requireNonNull(teamConfigurationSaver, "teamConfigurationSaver");
        this.onlinePlayerLookup = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup");
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "randomGenerator");
        this.nameResolver = Objects.requireNonNull(nameResolver, "nameResolver");
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    }

    public void createFolder() throws IOException {
        Files.createDirectories(importFolder);
    }

    public String describeFolder() {
        return importFolder.getParent() == null
                ? importFolder.toString()
                : importFolder.getParent().getFileName() + "/" + importFolder.getFileName();
    }

    public List<String> listFileNames() {
        if (!Files.isDirectory(importFolder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(importFolder)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> file.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException | UncheckedIOException failure) {
            return List.of();
        }
    }

    public boolean isImportRunning() {
        return importRunning;
    }

    public Optional<String> findRejection() {
        if (tournamentInProgress.getAsBoolean()) {
            return Optional.of(Messages.text(RegistrationMessage.FORMING_TOURNAMENT_IN_PROGRESS));
        }
        if (autoMatchAnnouncedOrRunning.getAsBoolean()) {
            return Optional.of(Messages.text(RegistrationMessage.FORMING_AUTO_MATCH));
        }
        return Optional.empty();
    }

    public void showFiles(CommandSender admin) {
        List<String> fileNames = listFileNames();
        if (fileNames.isEmpty()) {
            TournamentMessages.sendInfo(admin, Messages.text(RegistrationMessage.IMPORT_NO_FILES,
                    Placeholder.of("folder", describeFolder())));
            return;
        }
        TournamentMessages.sendInfo(admin, Messages.text(RegistrationMessage.IMPORT_FILES,
                Placeholder.of("folder", describeFolder()), Placeholder.of("files", Messages.join(fileNames))));
    }

    public void importFile(CommandSender admin, String requestedName) {
        if (importRunning) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.IMPORT_ALREADY_RUNNING));
            return;
        }
        Optional<String> rejection = findRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(admin, rejection.get());
            return;
        }
        Optional<String> fileName = findFileName(requestedName);
        if (fileName.isEmpty()) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.IMPORT_FILE_NOT_FOUND,
                    Placeholder.of("file", requestedName), Placeholder.of("folder", describeFolder())));
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(importFolder.resolve(fileName.get()), StandardCharsets.UTF_8);
        } catch (IOException | UncheckedIOException failure) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.IMPORT_FILE_NOT_READ,
                    Placeholder.of("file", fileName.get()),
                    Placeholder.of("reason", FailureDescriptions.describe(failure))));
            return;
        }
        int teamSize = teamSizeSetting.getTeamSize();
        TeamImportFile importFile = TeamImportFile.parse(lines, teamSize);
        importFile.problems().forEach(problem -> TournamentMessages.sendWarning(admin, describe(problem, teamSize)));
        List<String> playerNames = importFile.playerNames();
        if (playerNames.isEmpty()) {
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.IMPORT_NO_PLAYERS,
                    Placeholder.of("file", fileName.get())));
            return;
        }
        importRunning = true;
        TournamentMessages.sendInfo(admin, Messages.plural(RegistrationMessage.IMPORT_LOOKING_UP,
                playerNames.size()));
        Map<String, CompletableFuture<Optional<TeamMember>>> lookups = new LinkedHashMap<>();
        try {
            for (String playerName : playerNames) {
                lookups.put(playerName.toLowerCase(Locale.ROOT), nameResolver.resolve(playerName)
                        .exceptionally(failure -> Optional.empty()));
            }
        } catch (RuntimeException failure) {
            importRunning = false;
            TournamentMessages.sendError(admin, Messages.text(RegistrationMessage.IMPORT_LOOKUP_FAILED,
                    Placeholder.of("reason", FailureDescriptions.describe(failure))));
            return;
        }
        CompletableFuture.allOf(lookups.values().toArray(CompletableFuture[]::new))
                .whenComplete((ignored, failure) -> mainThreadExecutor.execute(
                        () -> finishImport(admin, fileName.get(), importFile, lookups, teamSize)));
    }

    private void finishImport(CommandSender admin, String fileName, TeamImportFile importFile,
                              Map<String, CompletableFuture<Optional<TeamMember>>> lookups, int teamSize) {
        importRunning = false;
        List<String> unknownNames = new ArrayList<>();
        Set<UUID> usedPlayers = new HashSet<>();
        List<List<TeamMember>> memberGroups = new ArrayList<>();
        for (List<String> group : importFile.groups()) {
            List<TeamMember> members = new ArrayList<>();
            for (String playerName : group) {
                Optional<TeamMember> member = lookups.get(playerName.toLowerCase(Locale.ROOT)).getNow(Optional.empty());
                if (member.isEmpty()) {
                    unknownNames.add(playerName);
                    continue;
                }
                if (usedPlayers.add(member.get().uniqueId())) {
                    members.add(member.get());
                }
            }
            if (!members.isEmpty()) {
                memberGroups.add(members);
            }
        }
        if (!unknownNames.isEmpty()) {
            TournamentMessages.sendWarning(admin, Messages.plural(RegistrationMessage.IMPORT_UNKNOWN_PLAYERS,
                    unknownNames.size(), Placeholder.of("players", Messages.listLimited(unknownNames,
                            LISTED_NAME_LIMIT))));
        }
        Optional<String> rejection = findRejection();
        if (rejection.isPresent()) {
            TournamentMessages.sendError(admin, rejection.get());
            return;
        }
        List<List<TeamMember>> plannedTeams = ImportedTeamPlan.plan(memberGroups, teamSize, randomGenerator);
        if (plannedTeams.size() < RandomTeamFormation.MINIMUM_TEAM_COUNT) {
            TournamentMessages.sendError(admin, Messages.plural(RegistrationMessage.IMPORT_TOO_FEW_TEAMS,
                    plannedTeams.size(), Placeholder.of("minimum", RandomTeamFormation.MINIMUM_TEAM_COUNT)));
            return;
        }
        List<Team> teams = RandomTeamFormation.formNumberedTeams(plannedTeams);
        teamRegistry.clear();
        for (Team team : teams) {
            teamRegistry.registerTeam(team.getTeamName(), team.getMembers(), team.getColour().orElse(null));
        }
        teamConfigurationSaver.run();
        TournamentMessages.sendSuccess(admin, Messages.plural(RegistrationMessage.IMPORT_DONE, teams.size(),
                Placeholder.of("players", usedPlayers.size()), Placeholder.of("file", fileName)));
        teams.forEach(team -> RandomTeamFormation.tellTheMembers(team, onlinePlayerLookup));
    }

    private Optional<String> findFileName(String requestedName) {
        String wantedName = requestedName.strip();
        String wantedWithSuffix = wantedName.toLowerCase(Locale.ROOT).endsWith(FILE_SUFFIX)
                ? wantedName
                : wantedName + FILE_SUFFIX;
        return listFileNames().stream()
                .filter(name -> name.equalsIgnoreCase(wantedWithSuffix))
                .findFirst();
    }

    private static String describe(TeamImportProblem problem, int teamSize) {
        Placeholder line = Placeholder.of("line", problem.lineNumber());
        return switch (problem.kind()) {
            case INVALID_NAME -> Messages.text(RegistrationMessage.IMPORT_INVALID_NAME, line,
                    Placeholder.of("name", problem.value()));
            case NAMED_TWICE -> Messages.text(RegistrationMessage.IMPORT_NAMED_TWICE, line,
                    Placeholder.of("name", problem.value()));
            case GROUP_TOO_LARGE -> Messages.text(RegistrationMessage.IMPORT_GROUP_TOO_LARGE, line,
                    Placeholder.of("players", problem.value()), Placeholder.of("size", teamSize));
        };
    }
}
