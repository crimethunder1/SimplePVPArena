package de.simplepvparena.diagnostics;

import de.simplepvparena.arena.ArenaShape;

import java.util.List;
import java.util.Optional;

public interface DryRunArena {

    boolean isReady();

    List<String> getMissingConfiguration();

    Optional<ArenaShape> getVolume();

    boolean hasFirstTeamSpawn();

    boolean hasSecondTeamSpawn();

    boolean hasBaseline();

    long countCleaningBlocks();

    Optional<String> findCleaningRejection();
}
