package de.simplepvparena.message;

import java.util.Set;

public enum DryRunMessage implements MessageKey {

    REJECTED("dry-run.report.rejected"),
    SUMMARY("dry-run.report.summary.plain", "passed", "count"),
    SUMMARY_WITH_FAILED("dry-run.report.summary.with-failed", "passed", "count", "failed"),
    SUMMARY_WITH_SKIPPED("dry-run.report.summary.with-skipped", "passed", "count", "skipped"),
    SUMMARY_WITH_FAILED_AND_SKIPPED("dry-run.report.summary.with-failed-and-skipped", "passed", "count", "failed",
            "skipped"),
    REAL_PLAYER_NOTE("dry-run.report.real-player-note"),
    STATUS_PASSED("dry-run.status.passed"),
    STATUS_SKIPPED("dry-run.status.skipped"),
    STATUS_FAILED("dry-run.status.failed"),
    TEAM_STEP("dry-run.step.teams"),
    ARENA_STEP("dry-run.step.arena"),
    KIT_STEP("dry-run.step.kit"),
    GATE_STEP("dry-run.step.gate"),
    MATCH_STEP("dry-run.step.match"),
    CLEANING_STEP("dry-run.step.cleaning"),
    CLEANUP_STEP("dry-run.step.cleanup"),
    FAILURE_STEP("dry-run.step.failure"),
    TOURNAMENT_IN_PROGRESS("dry-run.rejection.tournament-in-progress"),
    MATCH_ON_THE_ARENA("dry-run.rejection.match-on-the-arena", "action"),
    FAILURE("dry-run.failure", "failure"),
    TEAM_NAMES_TAKEN("dry-run.teams.names-taken", "teams"),
    TEAM_REGISTERED("dry-run.teams.registered", "team", "members"),
    TEAM_REFUSED("dry-run.teams.refused", "team", "reason"),
    TEAM_NOT_FOUND_BY_NAME("dry-run.teams.not-found-by-name", "team"),
    MEMBER_NOT_FOUND("dry-run.teams.member-not-found", "player", "team"),
    BOTH_PLACEHOLDER_PLAYERS_FOUND("dry-run.teams.both-placeholder-players-found"),
    EVERY_PLACEHOLDER_PLAYER_FOUND("dry-run.teams.every-placeholder-player-found"),
    PLACEHOLDER_MEMBERS("dry-run.teams.placeholder-members"),
    NOTHING_TO_REMOVE("dry-run.cleanup.nothing-to-remove", "count"),
    TEAMS_LEFT_BEHIND("dry-run.cleanup.left-behind", "teams"),
    TEAM_COUNT_CHANGED("dry-run.cleanup.count-changed", "count", "after"),
    TEST_TEAMS_REMOVED("dry-run.cleanup.removed", "count");

    private final String path;
    private final Set<String> placeholderNames;

    DryRunMessage(String path, String... placeholderNames) {
        this.path = path;
        this.placeholderNames = Set.of(placeholderNames);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public Set<String> placeholderNames() {
        return placeholderNames;
    }
}
