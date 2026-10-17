package de.simplepvparena.arena;

import java.util.Objects;
import java.util.Optional;

public final class ActiveArenaState {

    private final ArenaRegistry arenaRegistry;
    private final ArenaHeightMarkers heightMarkers;

    public ActiveArenaState(ArenaRegistry arenaRegistry, ArenaHeightMarkers heightMarkers) {
        this.arenaRegistry = Objects.requireNonNull(arenaRegistry, "arenaRegistry");
        this.heightMarkers = Objects.requireNonNull(heightMarkers, "heightMarkers");
    }

    public Arena getArena() {
        return arenaRegistry.getArena();
    }

    public ArenaHeightMarkers getHeightMarkers() {
        return heightMarkers;
    }

    public boolean isArenaFullyConfigured() {
        return arenaRegistry.isArenaFullyConfigured();
    }

    public Optional<ArenaShape> getCleanedVolume() {
        return arenaRegistry.getArena().getShape();
    }

    public Optional<EffectArea> findEffectArea() {
        return arenaRegistry.findEffectArea();
    }

    public Optional<String> findTribuneOutsideOfTheEffectArea() {
        return arenaRegistry.findTribuneOutsideOfTheEffectArea();
    }

    public ArenaVolumeUpdate adoptCalculation(ArenaShape calculatedShape) {
        Objects.requireNonNull(calculatedShape, "calculatedShape");
        ArenaShape storedShape = getCleanedVolume().orElse(null);
        Optional<ArenaShape> markedShape = heightMarkers.applyTo(calculatedShape);
        if (markedShape.isPresent()) {
            heightMarkers.clear();
            return store(ArenaVolumeUpdate.marksTakenOver(markedShape.get()));
        }
        Optional<ArenaShape> keptShape = findKeptMarkedVolume(calculatedShape, storedShape);
        if (keptShape.isPresent()) {
            return store(ArenaVolumeUpdate.marksKept(keptShape.get()));
        }
        ArenaShape droppedMarkedVolume = storedShape != null && storedShape.hasMarkedHeights()
                ? storedShape : null;
        return store(ArenaVolumeUpdate.readOffTheBorderWall(calculatedShape, droppedMarkedVolume));
    }

    public void clear() {
        arenaRegistry.clearArenaConfiguration();
        heightMarkers.clear();
    }

    private ArenaVolumeUpdate store(ArenaVolumeUpdate update) {
        arenaRegistry.getArena().setShape(update.volume());
        return update;
    }

    private static Optional<ArenaShape> findKeptMarkedVolume(ArenaShape calculatedShape,
                                                             ArenaShape storedShape) {
        return Optional.ofNullable(storedShape)
                .filter(ArenaShape::hasMarkedHeights)
                .filter(calculatedShape::coversTheSameArea)
                .map(marked -> calculatedShape.withVerticalExtent(marked.getFloorY(), marked.getWallTopY()));
    }
}
