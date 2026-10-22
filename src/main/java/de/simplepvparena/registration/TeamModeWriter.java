package de.simplepvparena.registration;

import java.io.IOException;

@FunctionalInterface
public interface TeamModeWriter {

    void write(TeamMode teamMode) throws IOException;
}
