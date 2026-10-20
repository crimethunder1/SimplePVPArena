package de.simplepvparena.diagnostics;

import de.simplepvparena.message.DryRunMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import de.simplepvparena.message.TournamentMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DryRunReport {

    private static final String STEP_INDENT = "  ";
    private static final String DETAIL_INDENT = "    ";
    private static final String MARKER_OPENING = "[";
    private static final String MARKER_CLOSING = "] ";

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

    public static List<String> describeRealPlayerNote() {
        return Messages.lines(DryRunMessage.REAL_PLAYER_NOTE);
    }

    public List<String> describe() {
        if (isRejected()) {
            return describeRejection();
        }
        List<String> lines = new ArrayList<>();
        lines.add(describeSummary());
        for (DryRunStep step : steps) {
            lines.add(STEP_INDENT + MARKER_OPENING + step.status().getMarker() + MARKER_CLOSING + step.name());
            for (String detail : step.details()) {
                lines.add(DETAIL_INDENT + detail);
            }
        }
        lines.addAll(describeRealPlayerNote());
        return List.copyOf(lines);
    }

    private String describeSummary() {
        Placeholder passed = Placeholder.of("passed", countByStatus(DryRunStatus.PASSED));
        long failedCount = countByStatus(DryRunStatus.FAILED);
        long skippedCount = countByStatus(DryRunStatus.SKIPPED);
        Placeholder failed = Placeholder.of("failed", failedCount);
        Placeholder skipped = Placeholder.of("skipped", skippedCount);
        if (failedCount > 0L && skippedCount > 0L) {
            return Messages.plural(DryRunMessage.SUMMARY_WITH_FAILED_AND_SKIPPED, steps.size(), passed, failed,
                    skipped);
        }
        if (failedCount > 0L) {
            return Messages.plural(DryRunMessage.SUMMARY_WITH_FAILED, steps.size(), passed, failed);
        }
        if (skippedCount > 0L) {
            return Messages.plural(DryRunMessage.SUMMARY_WITH_SKIPPED, steps.size(), passed, skipped);
        }
        return Messages.plural(DryRunMessage.SUMMARY, steps.size(), passed);
    }

    private List<String> describeRejection() {
        List<String> lines = new ArrayList<>();
        lines.add(Messages.text(DryRunMessage.REJECTED));
        lines.addAll(List.of(rejectionReason.split(TournamentMessages.LINE_BREAK, -1)));
        return List.copyOf(lines);
    }
}
