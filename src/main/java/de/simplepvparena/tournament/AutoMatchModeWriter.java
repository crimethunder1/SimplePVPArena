package de.simplepvparena.tournament;

import java.io.IOException;

@FunctionalInterface
public interface AutoMatchModeWriter {

    void write(boolean enabled) throws IOException;
}
