package de.simplepvparena.diagnostics;

import de.simplepvparena.arena.Arena;
import de.simplepvparena.arena.ArenaSetupService;
import de.simplepvparena.arena.ArenaShape;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PluginDryRunArena implements DryRunArena {

    private final Arena arena;
    private final ArenaSetupService arenaSetupService;

    public PluginDryRunArena(Arena arena, ArenaSetupService arenaSetupService) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.arenaSetupService = Objects.requireNonNull(arenaSetupService, "arenaSetupService");
    }

    @Override
    public boolean isReady() {
        return arenaSetupService.isArenaReady();
    }

    @Override
    public List<String> getMissingConfiguration() {
        return arena.getMissingConfiguration();
    }

    @Override
    public Optional<ArenaShape> getVolume() {
        return arenaSetupService.getShape();
    }

    @Override
    public boolean hasFirstTeamSpawn() {
        return arena.getConfiguredFirstTeamSpawn().isPresent();
    }

    @Override
    public boolean hasSecondTeamSpawn() {
        return arena.getConfiguredSecondTeamSpawn().isPresent();
    }

    @Override
    public boolean hasBaseline() {
        return arenaSetupService.hasBaseline();
    }

    @Override
    public long countCleaningBlocks() {
        return arenaSetupService.countCleaningBlocks();
    }

    @Override
    public Optional<String> findCleaningRejection() {
        return arenaSetupService.findCleaningRejection();
    }
}
