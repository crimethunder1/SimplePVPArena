package de.simplepvparena.arena;

import java.io.IOException;

@FunctionalInterface
public interface EffectMarginWriter {

    void write(int margin) throws IOException;
}
