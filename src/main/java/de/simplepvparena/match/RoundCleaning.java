package de.simplepvparena.match;

import java.util.Optional;
import java.util.function.Consumer;

public interface RoundCleaning {

    boolean hasBaseline();

    Optional<String> startCleaningBetweenRounds(Consumer<String> completionHandler);
}
