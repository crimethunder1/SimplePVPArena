package de.simplepvparena.gate;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface StartGates {

    StartGates NONE = new StartGates() {

        @Override
        public boolean openAtRoundStart(StartGateCountdownDisplay display) {
            return false;
        }

        @Override
        public int getCountdownSeconds() {
            return 0;
        }

        @Override
        public void stopAndRestore() {
        }
    };

    boolean openAtRoundStart(StartGateCountdownDisplay display);

    int getCountdownSeconds();

    void stopAndRestore();
}
