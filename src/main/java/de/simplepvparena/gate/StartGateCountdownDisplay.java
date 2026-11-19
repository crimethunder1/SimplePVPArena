package de.simplepvparena.gate;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface StartGateCountdownDisplay {

    StartGateCountdownDisplay NONE = remainingSeconds -> { };

    void show(int remainingSeconds);
}
