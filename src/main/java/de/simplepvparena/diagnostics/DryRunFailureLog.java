package de.simplepvparena.diagnostics;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class DryRunFailureLog {

    private final Logger logger;

    DryRunFailureLog(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    void write(RuntimeException failure) {
        logger.log(Level.WARNING, "The dry run failed", failure);
    }
}
