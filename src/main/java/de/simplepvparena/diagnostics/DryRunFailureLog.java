package de.simplepvparena.diagnostics;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@NullMarked
final class DryRunFailureLog {

    private final Logger logger;

    DryRunFailureLog(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    void write(RuntimeException failure) {
        logger.log(Level.WARNING, "The dry run failed", failure);
    }
}
