package de.simplepvparena.match;

import java.util.Optional;

@FunctionalInterface
public interface CountdownImages {

    CountdownImages NONE = remainingSeconds -> Optional.empty();

    Optional<String> findImage(int remainingSeconds);
}
