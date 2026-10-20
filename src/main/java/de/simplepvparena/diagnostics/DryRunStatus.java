package de.simplepvparena.diagnostics;

import de.simplepvparena.message.DryRunMessage;
import de.simplepvparena.message.Messages;

public enum DryRunStatus {

    PASSED(DryRunMessage.STATUS_PASSED),
    SKIPPED(DryRunMessage.STATUS_SKIPPED),
    FAILED(DryRunMessage.STATUS_FAILED);

    private final DryRunMessage marker;

    DryRunStatus(DryRunMessage marker) {
        this.marker = marker;
    }

    public String getMarker() {
        return Messages.text(marker);
    }
}
