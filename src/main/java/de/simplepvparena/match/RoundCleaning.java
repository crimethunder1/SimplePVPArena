package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.function.Consumer;

@NullMarked
public interface RoundCleaning {

    boolean hasBaseline();

    Optional<String> startCleaningBetweenRounds(Consumer<String> completionHandler);
}
