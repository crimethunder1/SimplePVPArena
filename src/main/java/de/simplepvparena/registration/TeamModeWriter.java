package de.simplepvparena.registration;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface TeamModeWriter {

    void write(TeamMode teamMode) throws IOException;
}
