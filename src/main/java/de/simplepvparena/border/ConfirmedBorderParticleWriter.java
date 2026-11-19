package de.simplepvparena.border;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface ConfirmedBorderParticleWriter {

    void write(boolean enabled) throws IOException;
}
