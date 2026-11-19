package de.simplepvparena.message;

import org.jspecify.annotations.NullMarked;

import java.util.Set;

@NullMarked
public interface MessageKey {

    String path();

    Set<String> placeholderNames();
}
