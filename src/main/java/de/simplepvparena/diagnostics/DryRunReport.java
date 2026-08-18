package de.simplepvparena.diagnostics;

import de.simplepvparena.message.TournamentMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DryRunReport {

    private static final String REJECTED_HEAD_LINE = "The dry run was not started";

    public static final List<String> REAL_PLAYER_NOTE = List.of(
            "A real test player is still needed for the parts that act on a body:",
            "  the kit really landing in an inventory and the old one being cleared",
            "  the teleport onto the two team spawns behind the gate",
            "  who counts as an active fighter, which follows from who is online",
            "  the gate countdown and the titles really running over the arena",
            "The dry run checks that all of them are configured, never that they arrived");

    private final String rejectionReason;
    private final List<DryRunStep> steps;

    private DryRunReport(String rejectionReason, List<DryRunStep> steps) {
        this.rejectionReason = rejectionReason;
        this.steps = List.copyOf(steps);
    }

    public static DryRunReport rejected(String reason) {
        return new DryRunReport(Objects.requireNonNull(reason, "reason"), List.of());
    }

    public static DryRunReport of(List<DryRunStep> steps) {
        return new DryRunReport(null, steps);
    }

    public List<DryRunStep> getSteps() {
        return steps;
    }

    public Optional<String> getRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }

    public boolean isRejected() {
        return rejectionReason != null;
    }

    public boolean isSuccessful() {
        return !isRejected() && countByStatus(DryRunStatus.FAILED) == 0L;
    }

    public long countByStatus(DryRunStatus status) {
        return steps.stream().filter(step -> step.status() == status).count();
    }

    public List<String> describe() {
        if (isRejected()) {
            return describeRejection();
        }
        List<String> lines = new ArrayList<>();
        lines.add(describeSummary());
        for (DryRunStep step : steps) {
            lines.add("  [" + step.status().getMarker() + "] " + step.name());
            for (String detail : step.details()) {
                lines.add("    " + detail);
            }
        }
        lines.addAll(REAL_PLAYER_NOTE);
        return List.copyOf(lines);
    }

    private String describeSummary() {
        StringBuilder summary = new StringBuilder("Dry run of the tournament pipeline: ")
                .append(countByStatus(DryRunStatus.PASSED)).append(" of ").append(steps.size())
                .append(steps.size() == 1 ? " step passed" : " steps passed");
        long failedCount = countByStatus(DryRunStatus.FAILED);
        if (failedCount > 0L) {
            summary.append(", ").append(failedCount).append(" failed");
        }
        long skippedCount = countByStatus(DryRunStatus.SKIPPED);
        if (skippedCount > 0L) {
            summary.append(", ").append(skippedCount).append(" skipped");
        }
        return summary.toString();
    }

    private List<String> describeRejection() {
        List<String> lines = new ArrayList<>();
        lines.add(REJECTED_HEAD_LINE);
        lines.addAll(List.of(rejectionReason.split(TournamentMessages.LINE_BREAK, -1)));
        return List.copyOf(lines);
    }
}
