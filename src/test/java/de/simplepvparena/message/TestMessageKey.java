package de.simplepvparena.message;

import java.util.Set;

record TestMessageKey(String path, Set<String> placeholderNames) implements MessageKey {

    static TestMessageKey of(String path, String... placeholderNames) {
        return new TestMessageKey(path, Set.of(placeholderNames));
    }
}
