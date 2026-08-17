package de.simplepvparena.gate;

@FunctionalInterface
public interface StartGateCountdownDisplay {

    StartGateCountdownDisplay NONE = remainingSeconds -> { };

    void show(int remainingSeconds);
}
