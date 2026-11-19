package de.simplepvparena.arena;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface MarkForgetting {

    void forgetEveryMark() throws IOException;
}
