package de.simplepvparena.tournament;

import java.util.Optional;
import java.util.function.Consumer;

@FunctionalInterface
public interface ArenaCleanup {

    Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler);
}
