package de.simplepvparena.message;

import java.util.Set;

public interface MessageKey {

    String path();

    Set<String> placeholderNames();
}
