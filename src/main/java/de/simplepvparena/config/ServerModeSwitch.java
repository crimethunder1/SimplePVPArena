package de.simplepvparena.config;

import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
@FunctionalInterface
public interface ServerModeSwitch {

    void switchTo(ServerMode serverMode) throws IOException;
}
