package de.simplepvparena.diagnostics;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.gate.StartGateCountdown;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGateOpeningSchedule;
import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.match.MatchService;
import de.simplepvparena.message.ArenaMenuMessage;
import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.DryRunCheckMessage;
import de.simplepvparena.message.DryRunMessage;
import de.simplepvparena.message.KitMenuMessage;
import de.simplepvparena.message.MatchMenuMessage;
import de.simplepvparena.message.MessageKey;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
import de.simplepvparena.team.TeamSizeSetting;
import de.simplepvparena.tournament.Bracket;
import de.simplepvparena.tournament.BracketMatch;
import de.simplepvparena.tournament.FinalMatchGate;
import de.simplepvparena.tournament.FinalMatchStage;
import de.simplepvparena.tournament.MatchSequence;
import de.simplepvparena.tournament.TournamentState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class TournamentDryRun {

    static final DryRunMessage TEAM_STEP = DryRunMessage.TEAM_STEP;
    static final DryRunMessage ARENA_STEP = DryRunMessage.ARENA_STEP;
    static final DryRunMessage KIT_STEP = DryRunMessage.KIT_STEP;
    static final DryRunMessage GATE_STEP = DryRunMessage.GATE_STEP;
    static final DryRunMessage MATCH_STEP = DryRunMessage.MATCH_STEP;
    static final DryRunMessage CLEANING_STEP = DryRunMessage.CLEANING_STEP;
    static final DryRunMessage CLEANUP_STEP = DryRunMessage.CLEANUP_STEP;
    static final DryRunMessage FAILURE_STEP = DryRunMessage.FAILURE_STEP;

    private static final String MISSING_PART_INDENT = "  ";

    private final Supplier<TournamentState> tournamentState;
    private final BooleanSupplier runningMatchLookup;
    private final TeamRegistry teamRegistry;
    private final DryRunArena arena;
    private final KitService kitService;
    private final StartGateMarks gateMarks;
    private final StartGateDuration gateDuration;
    private final TeamSizeSetting teamSizeSetting;

    public TournamentDryRun(Supplier<TournamentState> tournamentState,
                            BooleanSupplier runningMatchLookup,
                            TeamRegistry teamRegistry,
                            DryRunArena arena,
                            KitService kitService,
                            StartGateMarks gateMarks,
                            StartGateDuration gateDuration) {
        this(tournamentState, runningMatchLookup, teamRegistry, arena, kitService, gateMarks, gateDuration,
                new TeamSizeSetting());
    }

    public TournamentDryRun(Supplier<TournamentState> tournamentState,
                            BooleanSupplier runningMatchLookup,
                            TeamRegistry teamRegistry,
                            DryRunArena arena,
                            KitService kitService,
                            StartGateMarks gateMarks,
                            StartGateDuration gateDuration,
                            TeamSizeSetting teamSizeSetting) {
        this.tournamentState = Objects.requireNonNull(tournamentState, "tournamentState");
        this.runningMatchLookup = Objects.requireNonNull(runningMatchLookup, "runningMatchLookup");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.gateMarks = Objects.requireNonNull(gateMarks, "gateMarks");
        this.gateDuration = Objects.requireNonNull(gateDuration, "gateDuration");
        this.teamSizeSetting = Objects.requireNonNull(teamSizeSetting, "teamSizeSetting");
    }

    public Optional<String> findRejection() {
        TournamentState state = tournamentState.get();
        if (state == TournamentState.RUNNING || state == TournamentState.PAUSED) {
            return Optional.of(Messages.text(DryRunMessage.TOURNAMENT_IN_PROGRESS));
        }
        if (runningMatchLookup.getAsBoolean()) {
            return Optional.of(Messages.text(DryRunMessage.MATCH_ON_THE_ARENA, Placeholder.of("action",
                    MatchService.describeMatchMenuTile(MatchMenuMessage.FORCE_RESULT_TITLE))));
        }
        return Optional.empty();
    }

    public DryRunReport run() {
        Optional<String> rejection = findRejection();
        if (rejection.isPresent()) {
            return DryRunReport.rejected(rejection.get());
        }
        int teamCountBefore = teamRegistry.getTeamCount();
        List<DryRunTeam> testTeams = DryRunTeam.createTestTeams(teamSizeSetting.getTeamSize());
        List<DryRunTeam> registeredTeams = new ArrayList<>();
        List<DryRunStep> steps = new ArrayList<>();
        try {
            walkPipeline(testTeams, registeredTeams, steps);
        } catch (RuntimeException failure) {
            steps.add(DryRunStep.failed(stepName(FAILURE_STEP), Messages.lines(DryRunMessage.FAILURE,
                    Placeholder.of("failure", describeFailure(failure)))));
        } finally {
            steps.add(removeTestTeams(registeredTeams, teamCountBefore));
        }
        return DryRunReport.of(steps);
    }

    private void walkPipeline(List<DryRunTeam> testTeams, List<DryRunTeam> registeredTeams,
                              List<DryRunStep> steps) {
        DryRunStep registration = registerTestTeams(testTeams, registeredTeams);
        steps.add(registration);
        steps.add(checkArena());
        steps.add(checkKit());
        steps.add(checkStartGate());
        steps.add(registration.isPassed()
                ? walkThroughTheSimulatedMatch(testTeams)
                : DryRunStep.skipped(stepName(MATCH_STEP),
                        Messages.text(DryRunCheckMessage.TEST_TEAMS_NOT_REGISTERED)));
        steps.add(checkAutomaticCleaning());
    }

    private DryRunStep registerTestTeams(List<DryRunTeam> testTeams, List<DryRunTeam> registeredTeams) {
        List<String> takenNames = testTeams.stream()
                .map(DryRunTeam::teamName)
                .filter(teamName -> teamRegistry.findTeam(teamName).isPresent())
                .toList();
        if (!takenNames.isEmpty()) {
            return DryRunStep.failed(stepName(TEAM_STEP), pluralLines(DryRunMessage.TEAM_NAMES_TAKEN,
                    takenNames.size(), Placeholder.of("teams", Messages.join(takenNames))));
        }
        List<String> details = new ArrayList<>();
        for (DryRunTeam testTeam : testTeams) {
            try {
                Team team = teamRegistry.registerTeam(testTeam.teamName(), testTeam.members());
                registeredTeams.add(testTeam);
                details.add(Messages.text(DryRunMessage.TEAM_REGISTERED, Placeholder.of("team", team.getTeamName()),
                        Placeholder.of("members", team.getMemberNamesJoined())));
            } catch (IllegalArgumentException | IllegalStateException refusal) {
                return DryRunStep.failed(stepName(TEAM_STEP), Messages.text(DryRunMessage.TEAM_REFUSED,
                        Placeholder.of("team", testTeam.teamName()), Placeholder.of("reason", refusal.getMessage())));
            }
        }
        Optional<String> lookupFailure = findTeamLookupFailure(testTeams);
        if (lookupFailure.isPresent()) {
            return DryRunStep.failed(stepName(TEAM_STEP), lookupFailure.get());
        }
        details.add(Messages.text(describePlaceholderPlayers(testTeams)));
        details.add(Messages.text(DryRunMessage.PLACEHOLDER_MEMBERS));
        return DryRunStep.passed(stepName(TEAM_STEP), details);
    }

    private static DryRunMessage describePlaceholderPlayers(List<DryRunTeam> testTeams) {
        boolean everyTeamHasTwoPlayers = testTeams.stream().allMatch(testTeam -> testTeam.members().size() == 2);
        return everyTeamHasTwoPlayers
                ? DryRunMessage.BOTH_PLACEHOLDER_PLAYERS_FOUND
                : DryRunMessage.EVERY_PLACEHOLDER_PLAYER_FOUND;
    }

    private Optional<String> findTeamLookupFailure(List<DryRunTeam> testTeams) {
        for (DryRunTeam testTeam : testTeams) {
            if (teamRegistry.findTeam(testTeam.teamName()).isEmpty()) {
                return Optional.of(Messages.text(DryRunMessage.TEAM_NOT_FOUND_BY_NAME,
                        Placeholder.of("team", testTeam.teamName())));
            }
            for (TeamMember member : testTeam.members()) {
                Optional<Team> teamOfPlayer = teamRegistry.findTeamByPlayer(member.uniqueId());
                if (teamOfPlayer.isEmpty()
                        || !teamOfPlayer.get().getTeamName().equals(testTeam.teamName())) {
                    return Optional.of(Messages.text(DryRunMessage.MEMBER_NOT_FOUND,
                            Placeholder.of("player", member.playerName()),
                            Placeholder.of("team", testTeam.teamName())));
                }
            }
        }
        return Optional.empty();
    }

    private DryRunStep removeTestTeams(List<DryRunTeam> registeredTeams, int teamCountBefore) {
        if (registeredTeams.isEmpty()) {
            return DryRunStep.skipped(stepName(CLEANUP_STEP),
                    pluralLines(DryRunMessage.NOTHING_TO_REMOVE, teamRegistry.getTeamCount()));
        }
        List<String> leftBehind = new ArrayList<>();
        for (DryRunTeam testTeam : registeredTeams) {
            teamRegistry.removeTeam(testTeam.teamName());
            if (teamRegistry.findTeam(testTeam.teamName()).isPresent()) {
                leftBehind.add(testTeam.teamName());
            }
        }
        if (!leftBehind.isEmpty()) {
            return DryRunStep.failed(stepName(CLEANUP_STEP), pluralLines(DryRunMessage.TEAMS_LEFT_BEHIND,
                    leftBehind.size(), Placeholder.of("teams", Messages.join(leftBehind))));
        }
        int teamCountAfter = teamRegistry.getTeamCount();
        if (teamCountAfter != teamCountBefore) {
            return DryRunStep.failed(stepName(CLEANUP_STEP), pluralLines(DryRunMessage.TEAM_COUNT_CHANGED,
                    teamCountBefore, Placeholder.of("after", teamCountAfter)));
        }
        return DryRunStep.passed(stepName(CLEANUP_STEP),
                pluralLines(DryRunMessage.TEST_TEAMS_REMOVED, teamCountAfter));
    }

    private DryRunStep checkArena() {
        if (!arena.isReady()) {
            List<String> details = new ArrayList<>();
            details.add(Messages.text(DryRunCheckMessage.ARENA_NOT_READY));
            arena.getMissingConfiguration().forEach(missingPart -> details.add(MISSING_PART_INDENT + missingPart));
            details.add(Messages.text(DryRunCheckMessage.ARENA_NEVER_CALCULATED));
            return DryRunStep.failed(stepName(ARENA_STEP), details);
        }
        Optional<DryRunCheckMessage> missingSpawns = findMissingSpawns();
        if (missingSpawns.isPresent()) {
            return DryRunStep.failed(stepName(ARENA_STEP), Messages.lines(missingSpawns.get()));
        }
        List<String> details = new ArrayList<>();
        arena.getVolume().ifPresent(volume -> details.add(Messages.text(DryRunCheckMessage.FIGHT_AREA,
                Placeholder.of("area", volume.describe()))));
        arena.findCleanedVolume().ifPresent(cleanedVolume -> details.add(Messages.plural(
                DryRunCheckMessage.CLEANING_WRITES_BLOCKS, arena.countCleaningBlocks(),
                Placeholder.of("floorY", cleanedVolume.getFloorY()),
                Placeholder.of("topY", cleanedVolume.getWallTopY()))));
        details.add(Messages.text(DryRunCheckMessage.SPAWNS_SET));
        return DryRunStep.passed(stepName(ARENA_STEP), details);
    }

    private Optional<DryRunCheckMessage> findMissingSpawns() {
        boolean firstTeamSpawnMissing = !arena.hasFirstTeamSpawn();
        boolean secondTeamSpawnMissing = !arena.hasSecondTeamSpawn();
        if (firstTeamSpawnMissing && secondTeamSpawnMissing) {
            return Optional.of(DryRunCheckMessage.BOTH_TEAM_SPAWNS_MISSING);
        }
        if (firstTeamSpawnMissing) {
            return Optional.of(DryRunCheckMessage.FIRST_TEAM_SPAWN_MISSING);
        }
        if (secondTeamSpawnMissing) {
            return Optional.of(DryRunCheckMessage.SECOND_TEAM_SPAWN_MISSING);
        }
        return Optional.empty();
    }

    private DryRunStep checkKit() {
        KitDefinition kit = kitService.getKitDefinition();
        if (kit.isEmpty()) {
            return DryRunStep.failed(stepName(KIT_STEP), Messages.lines(DryRunCheckMessage.KIT_EMPTY,
                    Placeholder.of("tile", Messages.text(KitMenuMessage.CREATE_TITLE))));
        }
        List<String> details = new ArrayList<>(kit.describe());
        details.add(Messages.plural(DryRunCheckMessage.KIT_ITEMS, kit.itemCount()));
        return DryRunStep.passed(stepName(KIT_STEP), details);
    }

    private DryRunStep checkStartGate() {
        int durationSeconds = gateDuration.getSeconds();
        if (gateMarks.isEmpty()) {
            return DryRunStep.skipped(stepName(GATE_STEP), Messages.lines(DryRunCheckMessage.NO_GATE_BLOCK,
                    Placeholder.of("action", Messages.text(ArenaSetupMessage.ARENA_MENU_REFERENCE,
                            Placeholder.of("tile", Messages.text(ArenaMenuMessage.START_GATE_TITLE))))));
        }
        Optional<String> countdownFailure = findCountdownFailure(durationSeconds);
        if (countdownFailure.isPresent()) {
            return DryRunStep.failed(stepName(GATE_STEP), countdownFailure.get());
        }
        List<String> details = new ArrayList<>();
        details.add(Messages.text(DryRunCheckMessage.COUNTDOWN, Placeholder.of("seconds", durationSeconds),
                Placeholder.of("release", StartGateCountdown.describeReleaseTitle())));
        for (String worldName : gateMarks.getWorldNames()) {
            Optional<String> gateFailure = describeGateOfWorld(worldName, durationSeconds, details);
            if (gateFailure.isPresent()) {
                return DryRunStep.failed(stepName(GATE_STEP), gateFailure.get());
            }
        }
        return DryRunStep.passed(stepName(GATE_STEP), details);
    }

    private Optional<String> describeGateOfWorld(String worldName, int durationSeconds, List<String> details) {
        long[][] layers = gateMarks.collectLayersTopDown(worldName);
        StartGateOpeningSchedule schedule = StartGateOpeningSchedule.of(layers.length, durationSeconds);
        long previousRemovalTick = 0L;
        for (int layerIndex = 0; layerIndex < layers.length; layerIndex++) {
            long removalTick = schedule.getRemovalTickOfLayer(layerIndex);
            if (removalTick <= previousRemovalTick) {
                return Optional.of(Messages.text(DryRunCheckMessage.LAYER_TOO_EARLY,
                        Placeholder.of("layer", layerIndex + 1), Placeholder.of("world", worldName),
                        Placeholder.of("tick", removalTick)));
            }
            previousRemovalTick = removalTick;
        }
        long totalTickCount = schedule.getTotalTickCount();
        long countdownTicks = schedule.getTotalDurationTicks();
        if (totalTickCount < countdownTicks) {
            return Optional.of(Messages.text(DryRunCheckMessage.GATE_GONE_TOO_EARLY,
                    Placeholder.of("world", worldName), Placeholder.of("tick", totalTickCount),
                    Placeholder.of("countdown", countdownTicks)));
        }
        details.add(Messages.text(DryRunCheckMessage.GATE_OF_WORLD, Placeholder.of("world", worldName),
                Placeholder.of("blocks", Messages.plural(DryRunCheckMessage.GATE_BLOCK_COUNT,
                        gateMarks.getMarkedBlockCount(worldName))),
                Placeholder.of("layers", Messages.plural(DryRunCheckMessage.GATE_LAYER_COUNT, layers.length)),
                Placeholder.of("tick", totalTickCount), Placeholder.of("countdown", countdownTicks)));
        if (totalTickCount > countdownTicks) {
            details.add(Messages.plural(DryRunCheckMessage.GATE_TALLER_THAN_COUNTDOWN,
                    totalTickCount - countdownTicks,
                    Placeholder.of("release", StartGateCountdown.describeReleaseTitle())));
        }
        return Optional.empty();
    }

    private static Optional<String> findCountdownFailure(int durationSeconds) {
        for (int remainingSeconds = durationSeconds; remainingSeconds >= 1; remainingSeconds--) {
            if (StartGateCountdown.isRelease(remainingSeconds)
                    || !String.valueOf(remainingSeconds).equals(
                            StartGateCountdown.describeTitle(remainingSeconds))) {
                return Optional.of(Messages.text(DryRunCheckMessage.SECOND_READS,
                        Placeholder.of("second", remainingSeconds),
                        Placeholder.of("title", StartGateCountdown.describeTitle(remainingSeconds))));
            }
        }
        if (!StartGateCountdown.isRelease(StartGateCountdown.RELEASE_SECOND)
                || !StartGateCountdown.describeReleaseTitle().equals(
                        StartGateCountdown.describeTitle(StartGateCountdown.RELEASE_SECOND))) {
            return Optional.of(Messages.text(DryRunCheckMessage.END_READS,
                    Placeholder.of("title", StartGateCountdown.describeTitle(StartGateCountdown.RELEASE_SECOND)),
                    Placeholder.of("release", StartGateCountdown.describeReleaseTitle())));
        }
        return Optional.empty();
    }

    private DryRunStep walkThroughTheSimulatedMatch(List<DryRunTeam> testTeams) {
        List<String> teamNames = testTeams.stream().map(DryRunTeam::teamName).toList();
        List<String> details = new ArrayList<>();
        Bracket bracket = Bracket.createSingleElimination(teamNames);
        details.add(Messages.text(DryRunCheckMessage.BRACKET_BUILT,
                Placeholder.of("teams", Messages.plural(DryRunCheckMessage.BRACKET_TEAM_COUNT, teamNames.size())),
                Placeholder.of("rounds", Messages.plural(DryRunCheckMessage.BRACKET_ROUND_COUNT,
                        bracket.getRoundCount())),
                Placeholder.of("matches", Messages.plural(DryRunCheckMessage.BRACKET_MATCH_COUNT,
                        bracket.getAllMatches().size()))));
        BracketMatch finalMatch = bracket.getFinalMatch();
        if (!finalMatch.hasBothParticipants()) {
            return DryRunStep.failed(stepName(MATCH_STEP),
                    Messages.text(DryRunCheckMessage.FINAL_WITHOUT_BOTH_TEST_TEAMS));
        }
        for (String teamName : teamNames) {
            if (teamRegistry.findTeam(teamName).isEmpty()) {
                return DryRunStep.failed(stepName(MATCH_STEP), Messages.text(DryRunCheckMessage.TEAM_ONLY_IN_BRACKET,
                        Placeholder.of("team", teamName)));
            }
        }
        details.add(Messages.text(DryRunCheckMessage.MATCH_RESOLVED, Placeholder.of("match", finalMatch.getMatchId()),
                Placeholder.of("firstTeam", finalMatch.getFirstTeamName().orElseThrow()),
                Placeholder.of("secondTeam", finalMatch.getSecondTeamName().orElseThrow())));
        if (MatchSequence.findNextMatch(bracket).isPresent()) {
            return DryRunStep.failed(stepName(MATCH_STEP), Messages.text(DryRunCheckMessage.SEQUENCE_STARTS_FINAL));
        }
        details.add(Messages.text(DryRunCheckMessage.SEQUENCE_LEAVES_FINAL));
        Optional<String> gateFailure = walkThroughTheFinalGate(finalMatch, bracket, details);
        if (gateFailure.isPresent()) {
            return DryRunStep.failed(stepName(MATCH_STEP), gateFailure.get());
        }
        details.add(Messages.text(DryRunCheckMessage.NOTHING_REACHED_THE_ARENA));
        return DryRunStep.passed(stepName(MATCH_STEP), details);
    }

    private Optional<String> walkThroughTheFinalGate(BracketMatch finalMatch, Bracket bracket,
                                                     List<String> details) {
        List<String> adminMessages = new ArrayList<>();
        FinalMatchGate finalGate = new FinalMatchGate(adminMessages::add);
        finalGate.synchronizeWith(finalMatch);
        if (finalGate.getStage() != FinalMatchStage.READY) {
            return Optional.of(Messages.text(DryRunCheckMessage.FINAL_STAGE_NOT_READY,
                    Placeholder.of("stage", finalGate.getStage().describe()),
                    Placeholder.of("expected", FinalMatchStage.READY.describe())));
        }
        if (finalGate.findStartRejection().isPresent()) {
            return Optional.of(Messages.text(DryRunCheckMessage.FINAL_REFUSED,
                    Placeholder.of("reason", finalGate.findStartRejection().orElseThrow())));
        }
        if (adminMessages.size() != 1) {
            return Optional.of(Messages.plural(DryRunCheckMessage.FINAL_ANNOUNCED_TIMES, adminMessages.size()));
        }
        details.add(Messages.text(DryRunCheckMessage.FINAL_TURNS_READY,
                Placeholder.of("stage", FinalMatchStage.READY.describe()),
                Placeholder.of("announcement", adminMessages.get(0))));
        finalGate.markRunning();
        String winnerTeamName = finalMatch.getFirstTeamName().orElseThrow();
        bracket.recordWinner(finalMatch.getMatchId(), winnerTeamName);
        finalGate.synchronizeWith(finalMatch);
        if (!bracket.isComplete()) {
            return Optional.of(Messages.text(DryRunCheckMessage.BRACKET_INCOMPLETE));
        }
        if (!bracket.getChampionTeamName().equals(Optional.of(winnerTeamName))) {
            return Optional.of(describeWrongChampion(bracket.getChampionTeamName(), winnerTeamName));
        }
        if (finalGate.getStage() != FinalMatchStage.FINISHED) {
            return Optional.of(Messages.text(DryRunCheckMessage.FINAL_STAGE_NOT_FINISHED,
                    Placeholder.of("stage", finalGate.getStage().describe()),
                    Placeholder.of("expected", FinalMatchStage.FINISHED.describe())));
        }
        details.add(Messages.text(DryRunCheckMessage.SIMULATED_RESULT, Placeholder.of("winner", winnerTeamName),
                Placeholder.of("stage", FinalMatchStage.FINISHED.describe())));
        return Optional.empty();
    }

    private static String describeWrongChampion(Optional<String> championTeamName, String winnerTeamName) {
        Placeholder winner = Placeholder.of("winner", winnerTeamName);
        return championTeamName
                .map(champion -> Messages.text(DryRunCheckMessage.WRONG_CHAMPION,
                        Placeholder.of("champion", champion), winner))
                .orElseGet(() -> Messages.text(DryRunCheckMessage.NO_CHAMPION, winner));
    }

    private DryRunStep checkAutomaticCleaning() {
        Optional<String> rejection = arena.findCleaningRejection();
        if (rejection.isPresent()) {
            List<String> details = new ArrayList<>(
                    List.of(rejection.get().split(TournamentMessages.LINE_BREAK, -1)));
            details.add(Messages.text(DryRunCheckMessage.CLEANING_REFUSED_ARENA_LEFT_BEHIND));
            return DryRunStep.failed(stepName(CLEANING_STEP), details);
        }
        if (!arena.hasBaseline()) {
            return DryRunStep.failed(stepName(CLEANING_STEP), Messages.lines(DryRunCheckMessage.NO_BASELINE,
                    Placeholder.of("action", Arena.describeCalculateAction())));
        }
        List<String> details = new ArrayList<>();
        details.add(Messages.text(DryRunCheckMessage.CLEANING_WOULD_START));
        arena.findCleanedVolume().ifPresent(cleanedVolume -> details.add(Messages.plural(
                DryRunCheckMessage.CLEANED_BLOCKS, arena.countCleaningBlocks(),
                Placeholder.of("floorY", cleanedVolume.getFloorY()),
                Placeholder.of("topY", cleanedVolume.getWallTopY()))));
        details.add(Messages.text(DryRunCheckMessage.NOTHING_CLEANED));
        return DryRunStep.passed(stepName(CLEANING_STEP), details);
    }

    private static String stepName(DryRunMessage step) {
        return Messages.text(step);
    }

    private static List<String> pluralLines(MessageKey key, long count, Placeholder... placeholders) {
        return List.of(Messages.plural(key, count, placeholders).split(TournamentMessages.LINE_BREAK, -1));
    }

    private static String describeFailure(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + ": " + message;
    }
}
