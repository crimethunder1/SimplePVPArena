package de.simplepvparena.match;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
@FunctionalInterface
public interface CountdownImages {

    CountdownImages NONE = remainingSeconds -> Optional.empty();

    Optional<String> findImage(int remainingSeconds);
}
