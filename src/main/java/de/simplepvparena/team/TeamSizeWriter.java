package de.simplepvparena.team;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface TeamSizeWriter {

    void write(int teamSize) throws IOException;
}
