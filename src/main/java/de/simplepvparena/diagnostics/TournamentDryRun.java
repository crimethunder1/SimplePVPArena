package de.simplepvparena.diagnostics;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.gate.StartGateCountdown;
import de.simplepvparena.gate.StartGateDuration;
import de.simplepvparena.gate.StartGateMarks;
import de.simplepvparena.gate.StartGateOpeningSchedule;
import de.simplepvparena.kit.KitDefinition;
import de.simplepvparena.kit.KitService;
import de.simplepvparena.message.TournamentMessages;
import de.simplepvparena.team.Team;
import de.simplepvparena.team.TeamMember;
import de.simplepvparena.team.TeamRegistry;
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

    static final String TEAM_STEP = "Two test teams registered";
    static final String ARENA_STEP = "Arena, fight area and spawns";
    static final String KIT_STEP = "The kit a round start hands out";
    static final String GATE_STEP = "Start gate countdown";
    static final String MATCH_STEP = "Bracket and a simulated match";
    static final String CLEANING_STEP = "The cleaning after the match";
    static final String CLEANUP_STEP = "Test teams removed again";
    static final String FAILURE_STEP = "The dry run itself";

    private final Supplier<TournamentState> tournamentState;
    private final BooleanSupplier runningMatchLookup;
    private final TeamRegistry teamRegistry;
    private final DryRunArena arena;
    private final KitService kitService;
    private final StartGateMarks gateMarks;
    private final StartGateDuration gateDuration;

    public TournamentDryRun(Supplier<TournamentState> tournamentState,
                            BooleanSupplier runningMatchLookup,
                            TeamRegistry teamRegistry,
                            DryRunArena arena,
                            KitService kitService,
                            StartGateMarks gateMarks,
                            StartGateDuration gateDuration) {
        this.tournamentState = Objects.requireNonNull(tournamentState, "tournamentState");
        this.runningMatchLookup = Objects.requireNonNull(runningMatchLookup, "runningMatchLookup");
        this.teamRegistry = Objects.requireNonNull(teamRegistry, "teamRegistry");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.kitService = Objects.requireNonNull(kitService, "kitService");
        this.gateMarks = Objects.requireNonNull(gateMarks, "gateMarks");
        this.gateDuration = Objects.requireNonNull(gateDuration, "gateDuration");
    }

    public Optional<String> findRejection() {
        TournamentState state = tournamentState.get();
        if (state == TournamentState.RUNNING || state == TournamentState.PAUSED) {
            return Optional.of("A tournament is in progress, the dry run is a check for before an event"
                    + TournamentMessages.LINE_BREAK
                    + "It registers two test teams and would put them into the registry of a running "
                    + "bracket"
                    + TournamentMessages.LINE_BREAK
                    + "Wait until the event is over or cancel it in the match menu of /tournament");
        }
        if (runningMatchLookup.getAsBoolean()) {
            return Optional.of("A match stands on the arena, the dry run is a check for before an event"
                    + TournamentMessages.LINE_BREAK
                    + "Decide it with \"Force a result\" in the match menu of /tournament, or wait for it");
        }
        return Optional.empty();
    }

    public DryRunReport run() {
        Optional<String> rejection = findRejection();
        if (rejection.isPresent()) {
            return DryRunReport.rejected(rejection.get());
        }
        int teamCountBefore = teamRegistry.getTeamCount();
        List<DryRunTeam> testTeams = DryRunTeam.createTestTeams();
        List<DryRunTeam> registeredTeams = new ArrayList<>();
        List<DryRunStep> steps = new ArrayList<>();
        try {
            walkPipeline(testTeams, registeredTeams, steps);
        } catch (RuntimeException failure) {
            steps.add(DryRunStep.failed(FAILURE_STEP,
                    "The dry run ran into " + describeFailure(failure),
                    "Every step it got through is above, the server log holds the stack trace"));
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
                : DryRunStep.skipped(MATCH_STEP,
                        "The two test teams are not registered, so there is no bracket to walk"));
        steps.add(checkAutomaticCleaning());
    }

    private DryRunStep registerTestTeams(List<DryRunTeam> testTeams, List<DryRunTeam> registeredTeams) {
        List<String> takenNames = testTeams.stream()
                .map(DryRunTeam::teamName)
                .filter(teamName -> teamRegistry.findTeam(teamName).isPresent())
                .toList();
        if (!takenNames.isEmpty()) {
            return DryRunStep.failed(TEAM_STEP,
                    "A registered team is already called " + String.join(" and ", takenNames),
                    "The dry run registers exactly those names and never replaces a team of yours",
                    "Rename or remove that team in the team menu of /tournament and click again");
        }
        List<String> details = new ArrayList<>();
        for (DryRunTeam testTeam : testTeams) {
            try {
                Team team = teamRegistry.registerTeam(testTeam.teamName(), testTeam.members());
                registeredTeams.add(testTeam);
                details.add(team.getTeamName() + " with " + team.getMemberNamesJoined());
            } catch (IllegalArgumentException | IllegalStateException refusal) {
                return DryRunStep.failed(TEAM_STEP,
                        "The registry refused " + testTeam.teamName() + ": " + refusal.getMessage());
            }
        }
        Optional<String> lookupFailure = findTeamLookupFailure(testTeams);
        if (lookupFailure.isPresent()) {
            return DryRunStep.failed(TEAM_STEP, lookupFailure.get());
        }
        details.add("Both are found by name and " + describePlaceholderPlayers(testTeams)
                + " found by their unique id");
        details.add("The members are placeholders and no account of this server, and nothing was "
                + "written to teams.yml");
        return DryRunStep.passed(TEAM_STEP, details);
    }

    private static String describePlaceholderPlayers(List<DryRunTeam> testTeams) {
        boolean everyTeamHasTwoPlayers = testTeams.stream().allMatch(testTeam -> testTeam.members().size() == 2);
        return everyTeamHasTwoPlayers ? "both placeholder players are" : "every placeholder player is";
    }

    private Optional<String> findTeamLookupFailure(List<DryRunTeam> testTeams) {
        for (DryRunTeam testTeam : testTeams) {
            if (teamRegistry.findTeam(testTeam.teamName()).isEmpty()) {
                return Optional.of(testTeam.teamName() + " was registered but cannot be found by name");
            }
            for (TeamMember member : testTeam.members()) {
                Optional<Team> teamOfPlayer = teamRegistry.findTeamByPlayer(member.uniqueId());
                if (teamOfPlayer.isEmpty()
                        || !teamOfPlayer.get().getTeamName().equals(testTeam.teamName())) {
                    return Optional.of(member.playerName() + " is not found in " + testTeam.teamName()
                            + " by their unique id");
                }
            }
        }
        return Optional.empty();
    }

    private DryRunStep removeTestTeams(List<DryRunTeam> registeredTeams, int teamCountBefore) {
        if (registeredTeams.isEmpty()) {
            return DryRunStep.skipped(CLEANUP_STEP,
                    "No test team was registered, so there is nothing to remove",
                    "The registry still holds its own " + teamRegistry.getTeamCount() + " teams");
        }
        List<String> leftBehind = new ArrayList<>();
        for (DryRunTeam testTeam : registeredTeams) {
            teamRegistry.removeTeam(testTeam.teamName());
            if (teamRegistry.findTeam(testTeam.teamName()).isPresent()) {
                leftBehind.add(testTeam.teamName());
            }
        }
        if (!leftBehind.isEmpty()) {
            return DryRunStep.failed(CLEANUP_STEP,
                    "These test teams could not be removed: " + String.join(", ", leftBehind),
                    "Remove them by hand in the team menu of /tournament before the event starts");
        }
        int teamCountAfter = teamRegistry.getTeamCount();
        if (teamCountAfter != teamCountBefore) {
            return DryRunStep.failed(CLEANUP_STEP,
                    "The registry held " + teamCountBefore + " teams before the dry run and holds "
                            + teamCountAfter + " now",
                    "Check the team menu of /tournament before the event starts");
        }
        return DryRunStep.passed(CLEANUP_STEP,
                "Both test teams are gone, the registry holds its " + teamCountAfter
                        + " own teams again",
                "teams.yml was never written, so a restart could not bring them back either");
    }

    private DryRunStep checkArena() {
        if (!arena.isReady()) {
            List<String> details = new ArrayList<>();
            details.add("The arena cannot host a match, it still misses:");
            arena.getMissingConfiguration().forEach(missingPart -> details.add("  " + missingPart));
            details.add("The dry run never calculates the fight area itself: a calculation replaces "
                    + "the shape, the baseline and the marked heights");
            return DryRunStep.failed(ARENA_STEP, details);
        }
        List<String> missingSpawns = new ArrayList<>();
        if (!arena.hasFirstTeamSpawn()) {
            missingSpawns.add("the first team spawn");
        }
        if (!arena.hasSecondTeamSpawn()) {
            missingSpawns.add("the second team spawn");
        }
        if (!missingSpawns.isEmpty()) {
            return DryRunStep.failed(ARENA_STEP,
                    "The arena is ready but " + String.join(" and ", missingSpawns) + " is not set",
                    "An unset team spawn falls back to the middle of the fight area, so both teams "
                            + "would start on the same block",
                    "Set it in the arena menu of /tournament while standing on it");
        }
        List<String> details = new ArrayList<>();
        arena.getVolume().ifPresent(volume -> details.add("Fight area: " + volume.describe()));
        arena.findCleanedVolume().ifPresent(cleanedVolume -> details.add("A cleaning writes "
                + arena.countCleaningBlocks() + " blocks between y " + cleanedVolume.getFloorY()
                + " and y " + cleanedVolume.getWallTopY()));
        details.add("Both team spawns and the tribune spawn are set");
        return DryRunStep.passed(ARENA_STEP, details);
    }

    private DryRunStep checkKit() {
        KitDefinition kit = kitService.getKitDefinition();
        if (kit.isEmpty()) {
            return DryRunStep.failed(KIT_STEP,
                    "The kit is empty, every fighter would start the round with bare hands",
                    "Put the kit into your own inventory, create it with \"Create a kit from your "
                            + "inventory\" in the kit menu of /tournament and use it as the default kit");
        }
        List<String> details = new ArrayList<>(kit.describe());
        details.add("Every fighter is given these " + kit.itemCount()
                + " items at every round start, each as a copy of its own");
        return DryRunStep.passed(KIT_STEP, details);
    }

    private DryRunStep checkStartGate() {
        int durationSeconds = gateDuration.getSeconds();
        if (gateMarks.isEmpty()) {
            return DryRunStep.skipped(GATE_STEP,
                    "No gate block is marked, the rounds start without a gate",
                    "That is a working setup and nothing is refused because of it",
                    "Hand yourself gate blocks with \"Give start gate blocks\" in the arena menu of "
                            + "/tournament to build one");
        }
        Optional<String> countdownFailure = findCountdownFailure(durationSeconds);
        if (countdownFailure.isPresent()) {
            return DryRunStep.failed(GATE_STEP, countdownFailure.get());
        }
        List<String> details = new ArrayList<>();
        details.add("The countdown reads " + durationSeconds + " down to 1 and then \""
                + StartGateCountdown.RELEASE_TITLE + "\", checked without waiting a tick");
        for (String worldName : gateMarks.getWorldNames()) {
            Optional<String> gateFailure = describeGateOfWorld(worldName, durationSeconds, details);
            if (gateFailure.isPresent()) {
                return DryRunStep.failed(GATE_STEP, gateFailure.get());
            }
        }
        return DryRunStep.passed(GATE_STEP, details);
    }

    private Optional<String> describeGateOfWorld(String worldName, int durationSeconds, List<String> details) {
        long[][] layers = gateMarks.collectLayersTopDown(worldName);
        StartGateOpeningSchedule schedule = StartGateOpeningSchedule.of(layers.length, durationSeconds);
        long previousRemovalTick = 0L;
        for (int layerIndex = 0; layerIndex < layers.length; layerIndex++) {
            long removalTick = schedule.getRemovalTickOfLayer(layerIndex);
            if (removalTick <= previousRemovalTick) {
                return Optional.of("Layer " + (layerIndex + 1) + " of the gate in " + worldName
                        + " would be taken away on tick " + removalTick + ", not after the layer above it");
            }
            previousRemovalTick = removalTick;
        }
        long totalTickCount = schedule.getTotalTickCount();
        long countdownTicks = schedule.getTotalDurationTicks();
        if (totalTickCount < countdownTicks) {
            return Optional.of("The gate in " + worldName + " would be gone on tick " + totalTickCount
                    + ", before the countdown ends on tick " + countdownTicks);
        }
        details.add(worldName + ": " + gateMarks.getMarkedBlockCount(worldName) + " blocks in "
                + layers.length + (layers.length == 1 ? " layer" : " layers")
                + ", the last one falls on tick " + totalTickCount + " of " + countdownTicks);
        if (totalTickCount > countdownTicks) {
            details.add("  That gate is taller than the countdown has ticks, so it keeps coming down "
                    + "for " + (totalTickCount - countdownTicks) + " ticks after \""
                    + StartGateCountdown.RELEASE_TITLE + "\"");
        }
        return Optional.empty();
    }

    private static Optional<String> findCountdownFailure(int durationSeconds) {
        for (int remainingSeconds = durationSeconds; remainingSeconds >= 1; remainingSeconds--) {
            if (StartGateCountdown.isRelease(remainingSeconds)
                    || !String.valueOf(remainingSeconds).equals(
                            StartGateCountdown.describeTitle(remainingSeconds))) {
                return Optional.of("Second " + remainingSeconds + " of the countdown would read \""
                        + StartGateCountdown.describeTitle(remainingSeconds) + "\" over the arena");
            }
        }
        if (!StartGateCountdown.isRelease(StartGateCountdown.RELEASE_SECOND)
                || !StartGateCountdown.RELEASE_TITLE.equals(
                        StartGateCountdown.describeTitle(StartGateCountdown.RELEASE_SECOND))) {
            return Optional.of("The end of the countdown would read \""
                    + StartGateCountdown.describeTitle(StartGateCountdown.RELEASE_SECOND)
                    + "\" instead of \"" + StartGateCountdown.RELEASE_TITLE + "\"");
        }
        return Optional.empty();
    }

    private DryRunStep walkThroughTheSimulatedMatch(List<DryRunTeam> testTeams) {
        List<String> teamNames = testTeams.stream().map(DryRunTeam::teamName).toList();
        List<String> details = new ArrayList<>();
        Bracket bracket = Bracket.createSingleElimination(teamNames);
        details.add("Bracket built over " + teamNames.size() + " teams: " + bracket.getRoundCount()
                + (bracket.getRoundCount() == 1 ? " round, " : " rounds, ")
                + bracket.getAllMatches().size() + " match in it");
        BracketMatch finalMatch = bracket.getFinalMatch();
        if (!finalMatch.hasBothParticipants()) {
            return DryRunStep.failed(MATCH_STEP,
                    "The single match of the bracket did not get both test teams");
        }
        for (String teamName : teamNames) {
            if (teamRegistry.findTeam(teamName).isEmpty()) {
                return DryRunStep.failed(MATCH_STEP, "The bracket holds " + teamName
                        + ", but the registry does not. A real match would declare a walkover");
            }
        }
        details.add("Match " + finalMatch.getMatchId() + ": " + finalMatch.getFirstTeamName().orElseThrow()
                + " against " + finalMatch.getSecondTeamName().orElseThrow()
                + ", both teams resolve in the registry");
        if (MatchSequence.findNextMatch(bracket).isPresent()) {
            return DryRunStep.failed(MATCH_STEP,
                    "The match sequence would start the final by itself instead of leaving it to an admin");
        }
        details.add("The match sequence leaves it alone: the single match of a two team bracket is the "
                + "final, and the final is the one match an admin starts");
        Optional<String> gateFailure = walkThroughTheFinalGate(finalMatch, bracket, details);
        if (gateFailure.isPresent()) {
            return DryRunStep.failed(MATCH_STEP, gateFailure.get());
        }
        details.add("Nothing of this reached the arena: no round was begun, no gate was opened and no "
                + "fighter was teleported or equipped");
        return DryRunStep.passed(MATCH_STEP, details);
    }

    private Optional<String> walkThroughTheFinalGate(BracketMatch finalMatch, Bracket bracket,
                                                     List<String> details) {
        List<String> adminMessages = new ArrayList<>();
        FinalMatchGate finalGate = new FinalMatchGate(adminMessages::add);
        finalGate.synchronizeWith(finalMatch);
        if (finalGate.getStage() != FinalMatchStage.READY) {
            return Optional.of("Both finalists are known, but the final gate stands at "
                    + finalGate.getStage() + " instead of " + FinalMatchStage.READY);
        }
        if (finalGate.findStartRejection().isPresent()) {
            return Optional.of("The final is ready but would be refused: "
                    + finalGate.findStartRejection().orElseThrow());
        }
        if (adminMessages.size() != 1) {
            return Optional.of("The ready final announced itself " + adminMessages.size()
                    + " times to the admins instead of exactly once");
        }
        details.add("The final gate turns " + FinalMatchStage.READY + " and announces once: "
                + adminMessages.get(0));
        finalGate.markRunning();
        String winnerTeamName = finalMatch.getFirstTeamName().orElseThrow();
        bracket.recordWinner(finalMatch.getMatchId(), winnerTeamName);
        finalGate.synchronizeWith(finalMatch);
        if (!bracket.isComplete()) {
            return Optional.of("The decided final left the bracket incomplete");
        }
        if (!bracket.getChampionTeamName().equals(Optional.of(winnerTeamName))) {
            return Optional.of("The champion is " + bracket.getChampionTeamName().orElse("nobody")
                    + " instead of " + winnerTeamName);
        }
        if (finalGate.getStage() != FinalMatchStage.FINISHED) {
            return Optional.of("The decided final left the gate at " + finalGate.getStage()
                    + " instead of " + FinalMatchStage.FINISHED);
        }
        details.add("Simulated result for " + winnerTeamName + ": the bracket is complete, the champion "
                + "is " + winnerTeamName + " and the final gate is " + FinalMatchStage.FINISHED);
        return Optional.empty();
    }

    private DryRunStep checkAutomaticCleaning() {
        Optional<String> rejection = arena.findCleaningRejection();
        if (rejection.isPresent()) {
            List<String> details = new ArrayList<>(
                    List.of(rejection.get().split(TournamentMessages.LINE_BREAK, -1)));
            details.add("The match after it would be fought in the arena the match before it left behind");
            return DryRunStep.failed(CLEANING_STEP, details);
        }
        if (!arena.hasBaseline()) {
            return DryRunStep.failed(CLEANING_STEP,
                    "No baseline of the fight area is stored, so nothing is cleaned between two matches",
                    "A cleaning nobody asked for never empties the arena, it is skipped instead",
                    "The baseline lives in memory only: run " + Arena.CALCULATE_ACTION + " once");
        }
        List<String> details = new ArrayList<>();
        details.add("A cleaning would start right away and restore the baseline of the last calculation");
        arena.findCleanedVolume().ifPresent(cleanedVolume -> details.add(arena.countCleaningBlocks()
                + " blocks between y " + cleanedVolume.getFloorY() + " and y " + cleanedVolume.getWallTopY()));
        details.add("Nothing was written: the dry run checks the trigger, it never cleans the arena");
        return DryRunStep.passed(CLEANING_STEP, details);
    }

    private static String describeFailure(RuntimeException failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
                ? failure.getClass().getSimpleName()
                : failure.getClass().getSimpleName() + ": " + message;
    }
}
