package de.simplepvparena.arena;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Arena {

    public static final String CALCULATE_COMMAND = "/tournament arena calculate";
    public static final String TRIBUNE_SPAWN_COMMAND = "/tournament arena setspawn tribune";

    private ArenaShape shape;
    private PositionSnapshot firstTeamSpawn;
    private PositionSnapshot secondTeamSpawn;
    private PositionSnapshot tribuneSpawn;

    public void setShape(ArenaShape shape) {
        this.shape = shape;
    }

    public Optional<ArenaShape> getShape() {
        return Optional.ofNullable(shape);
    }

    public boolean hasShape() {
        return shape != null;
    }

    public boolean contains(Location location) {
        return shape != null && shape.contains(location);
    }

    public boolean isFullyConfigured() {
        return shape != null && tribuneSpawn != null;
    }

    public List<String> getMissingConfiguration() {
        List<String> missingParts = new ArrayList<>();
        if (shape == null) {
            missingParts.add("the calculated fight area (" + CALCULATE_COMMAND + ")");
        }
        if (tribuneSpawn == null) {
            missingParts.add("the tribune spawn (" + TRIBUNE_SPAWN_COMMAND + ")");
        }
        return missingParts;
    }

    public List<String> findSpawnPlacementWarnings() {
        if (shape == null) {
            return List.of();
        }
        List<String> warnings = new ArrayList<>();
        addSpawnWarning(warnings, firstTeamSpawn, true, "The first team spawn");
        addSpawnWarning(warnings, secondTeamSpawn, true, "The second team spawn");
        addSpawnWarning(warnings, tribuneSpawn, false, "The tribune spawn");
        return warnings;
    }

    public void setFirstTeamSpawn(PositionSnapshot position) {
        firstTeamSpawn = position;
    }

    public void setSecondTeamSpawn(PositionSnapshot position) {
        secondTeamSpawn = position;
    }

    public void setTribuneSpawn(PositionSnapshot position) {
        tribuneSpawn = position;
    }

    public Optional<PositionSnapshot> getConfiguredFirstTeamSpawn() {
        return Optional.ofNullable(firstTeamSpawn);
    }

    public Optional<PositionSnapshot> getConfiguredSecondTeamSpawn() {
        return Optional.ofNullable(secondTeamSpawn);
    }

    public Optional<PositionSnapshot> getConfiguredTribuneSpawn() {
        return Optional.ofNullable(tribuneSpawn);
    }

    public Optional<PositionSnapshot> getFirstTeamSpawn() {
        return getConfiguredFirstTeamSpawn().or(this::getShapeCenter);
    }

    public Optional<PositionSnapshot> getSecondTeamSpawn() {
        return getConfiguredSecondTeamSpawn().or(this::getShapeCenter);
    }

    public Optional<PositionSnapshot> getTribuneSpawn() {
        return getConfiguredTribuneSpawn();
    }

    public Optional<PositionSnapshot> getSpectatorFlightSpawn() {
        return getShapeCenter();
    }

    public void clearConfiguration() {
        shape = null;
        firstTeamSpawn = null;
        secondTeamSpawn = null;
        tribuneSpawn = null;
    }

    private Optional<PositionSnapshot> getShapeCenter() {
        return getShape().map(arenaShape -> arenaShape.getCenterPosition());
    }

    private void addSpawnWarning(List<String> warnings, PositionSnapshot spawn, boolean expectedInside,
                                 String spawnName) {
        if (spawn == null) {
            return;
        }
        boolean insideArena = shape.contains(spawn.worldName(), spawn.x(), spawn.z());
        if (insideArena == expectedInside) {
            return;
        }
        warnings.add(spawnName + " lies " + (insideArena ? "inside" : "outside of")
                + " the calculated fight area, it should lie " + (expectedInside ? "inside" : "outside of") + " it");
    }
}
