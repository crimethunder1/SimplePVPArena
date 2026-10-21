package de.simplepvparena.border;

import java.io.IOException;

@FunctionalInterface
public interface ConfirmedBorderParticleWriter {

    void write(boolean enabled) throws IOException;
}
