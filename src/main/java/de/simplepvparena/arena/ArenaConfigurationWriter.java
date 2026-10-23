package de.simplepvparena.arena;

import java.io.IOException;

@FunctionalInterface
public interface ArenaConfigurationWriter {

    void write() throws IOException;
}
