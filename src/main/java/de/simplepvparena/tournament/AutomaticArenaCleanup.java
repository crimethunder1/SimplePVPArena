package de.simplepvparena.tournament;

import de.simplepvparena.arena.ArenaCleaningService;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.match.RoundCleaning;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class AutomaticArenaCleanup implements ArenaCleanup, RoundCleaning {

    private final ArenaSetupService arenaSetupService;
    private final ArenaCleaningService arenaCleaningService;

    public AutomaticArenaCleanup(ArenaSetupService arenaSetupService, ArenaCleaningService arenaCleaningService) {
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
        this.arenaCleaningService = Objects.requireNonNull(arenaCleaningService, "arenaCleaningService");
    }

    @Override
    public Optional<String> startCleaningAfterMatch(Consumer<String> completionHandler) {
        return arenaSetupService.startAutomaticCleaning(completionHandler);
    }

    @Override
    public Optional<String> startCleaningBetweenRounds(Consumer<String> completionHandler) {
        return arenaSetupService.startCleaningBetweenRounds(completionHandler);
    }

    @Override
    public boolean isCleaning() {
        return arenaCleaningService.isCleaning();
    }

    @Override
    public boolean hasBaseline() {
        return arenaSetupService.hasBaseline();
    }
}
