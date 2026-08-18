package de.simplepvparena.diagnostics;

public enum DryRunStatus {

    PASSED("ok"),
    SKIPPED("skipped"),
    FAILED("FAILED");

    private final String marker;

    DryRunStatus(String marker) {
        this.marker = marker;
    }

    public String getMarker() {
        return marker;
    }
}
