package de.simplepvparena.arena;

import de.simplepvparena.border.ActiveFighterLookup;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ArenaSafetyZone {

    public static final String RADIUS_CONFIGURATION_PATH = "arena.safety-zone-radius";
    public static final int DEFAULT_RADIUS = 64;

    private final Supplier<Optional<ArenaShape>> shapeSource;
    private final ActiveFighterLookup activeFighterLookup;

    private int radius = DEFAULT_RADIUS;
    private ArenaShape contourShape;
    private ArenaOutline contour;

    public ArenaSafetyZone(Supplier<Optional<ArenaShape>> shapeSource, ActiveFighterLookup activeFighterLookup) {
        this.shapeSource = Objects.requireNonNull(shapeSource, "shapeSource");
        this.activeFighterLookup = Objects.requireNonNull(activeFighterLookup, "activeFighterLookup");
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getRadius() {
        return radius;
    }

    public boolean protects(UUID playerUniqueId, String worldName, double x, double z) {
        if (radius <= 0) {
            return false;
        }
        Optional<ArenaShape> configuredShape = shapeSource.get();
        if (configuredShape.isEmpty()) {
            return false;
        }
        ArenaShape shape = configuredShape.get();
        if (!shape.getWorldName().equals(worldName)) {
            return false;
        }
        if (activeFighterLookup.isActiveFighter(playerUniqueId)) {
            return false;
        }
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        if (shape.containsCell(blockX, blockZ)) {
            return false;
        }
        return findContour(shape).isWithin(blockX, blockZ, radius);
    }

    public Optional<Double> findDistanceToContour(String worldName, double x, double z) {
        return shapeSource.get()
                .filter(shape -> shape.getWorldName().equals(worldName))
                .map(shape -> findContour(shape).distanceTo((int) Math.floor(x), (int) Math.floor(z)));
    }

    private ArenaOutline findContour(ArenaShape shape) {
        if (contour == null || contourShape != shape) {
            contourShape = shape;
            contour = ArenaOutline.of(shape);
        }
        return contour;
    }
}
