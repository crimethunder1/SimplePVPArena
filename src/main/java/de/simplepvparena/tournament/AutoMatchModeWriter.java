package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface AutoMatchModeWriter {

    void write(boolean enabled) throws IOException;
}
