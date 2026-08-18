package de.simplepvparena.diagnostics;

import java.util.List;
import java.util.Objects;

public record DryRunStep(String name, DryRunStatus status, List<String> details) {

    public DryRunStep {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        details = List.copyOf(details);
    }

    public static DryRunStep passed(String name, List<String> details) {
        return new DryRunStep(name, DryRunStatus.PASSED, details);
    }

    public static DryRunStep passed(String name, String... details) {
        return passed(name, List.of(details));
    }

    public static DryRunStep skipped(String name, String... details) {
        return new DryRunStep(name, DryRunStatus.SKIPPED, List.of(details));
    }

    public static DryRunStep failed(String name, List<String> details) {
        return new DryRunStep(name, DryRunStatus.FAILED, details);
    }

    public static DryRunStep failed(String name, String... details) {
        return failed(name, List.of(details));
    }

    public boolean isPassed() {
        return status == DryRunStatus.PASSED;
    }

    public boolean isFailed() {
        return status == DryRunStatus.FAILED;
    }
}
