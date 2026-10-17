package de.simplepvparena.team;

import java.io.IOException;

@FunctionalInterface
public interface TeamSizeWriter {

    void write(int teamSize) throws IOException;
}
