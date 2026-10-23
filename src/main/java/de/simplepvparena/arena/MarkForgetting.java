package de.simplepvparena.arena;

import java.io.IOException;

@FunctionalInterface
public interface MarkForgetting {

    void forgetEveryMark() throws IOException;
}
