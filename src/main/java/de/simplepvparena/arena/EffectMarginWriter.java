package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface EffectMarginWriter {

    void write(int margin) throws IOException;
}
