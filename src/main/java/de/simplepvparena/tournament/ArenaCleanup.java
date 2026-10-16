package de.simplepvparena.tournament;

import java.util.Optional;
import java.util.function.Consumer;

public interface ArenaCleanup {

    Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler);

    boolean isCleaning();

    boolean hasBaseline();
}
