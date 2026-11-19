package de.simplepvparena.tournament;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.function.Consumer;

@NullMarked
public interface ArenaCleanup {

    Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler);

    boolean isCleaning();

    boolean hasBaseline();
}
