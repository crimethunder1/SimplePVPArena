package de.simplepvparena.arena;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Arena {

    private final Map<ArenaRegionType, EnumMap<RegionCorner, PositionSnapshot>> cornersByRegionType =
            new EnumMap<>(ArenaRegionType.class);

    private PositionSnapshot firstTeamSpawn;
    private PositionSnapshot secondTeamSpawn;
    private PositionSnapshot tribuneSpawn;

    public Arena() {
        for (ArenaRegionType regionType : ArenaRegionType.values()) {
            cornersByRegionType.put(regionType, new EnumMap<>(RegionCorner.class));
        }
    }

    public void setCorner(ArenaRegionType regionType, RegionCorner corner, PositionSnapshot position) {
        cornersByRegionType.get(regionType).put(corner, position);
    }

    public Optional<PositionSnapshot> getCorner(ArenaRegionType regionType, RegionCorner corner) {
        return Optional.ofNullable(cornersByRegionType.get(regionType).get(corner));
    }

    public Optional<RegionBoundary> getRegion(ArenaRegionType regionType) {
        PositionSnapshot firstCorner = cornersByRegionType.get(regionType).get(RegionCorner.CORNER_ONE);
        PositionSnapshot secondCorner = cornersByRegionType.get(regionType).get(RegionCorner.CORNER_TWO);
        if (firstCorner == null || secondCorner == null) {
            return Optional.empty();
        }
        if (!firstCorner.worldName().equals(secondCorner.worldName())) {
            return Optional.empty();
        }
        return Optional.of(new RegionBoundary(firstCorner.worldName(),
                (int) Math.floor(firstCorner.x()), (int) Math.floor(firstCorner.y()), (int) Math.floor(firstCorner.z()),
                (int) Math.floor(secondCorner.x()), (int) Math.floor(secondCorner.y()), (int) Math.floor(secondCorner.z())));
    }

    public List<RegionBoundary> getConfiguredRegions() {
        List<RegionBoundary> regions = new ArrayList<>();
        for (ArenaRegionType regionType : ArenaRegionType.values()) {
            getRegion(regionType).ifPresent(regions::add);
        }
        return regions;
    }

    public boolean isFullyConfigured() {
        return Arrays.stream(ArenaRegionType.values()).allMatch(regionType -> getRegion(regionType).isPresent());
    }

    public List<ArenaRegionType> getMissingRegionTypes() {
        return Arrays.stream(ArenaRegionType.values())
                .filter(regionType -> getRegion(regionType).isEmpty())
                .toList();
    }

    public boolean containsInFightRegion(Location location) {
        return getRegion(ArenaRegionType.FIGHT).map(region -> region.contains(location)).orElse(false);
    }

    public boolean containsInSpectatorBox(Location location) {
        return getRegion(ArenaRegionType.SPECTATOR_BOX).map(region -> region.contains(location)).orElse(false);
    }

    public boolean containsInTribune(Location location) {
        return getRegion(ArenaRegionType.TRIBUNE).map(region -> region.contains(location)).orElse(false);
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
        return getConfiguredFirstTeamSpawn().or(() -> getRegion(ArenaRegionType.FIGHT).map(RegionBoundary::getCenterPosition));
    }

    public Optional<PositionSnapshot> getSecondTeamSpawn() {
        return getConfiguredSecondTeamSpawn().or(() -> getRegion(ArenaRegionType.FIGHT).map(RegionBoundary::getCenterPosition));
    }

    public Optional<PositionSnapshot> getTribuneSpawn() {
        return getConfiguredTribuneSpawn().or(() -> getRegion(ArenaRegionType.TRIBUNE).map(RegionBoundary::getCenterPosition));
    }

    public Optional<PositionSnapshot> getSpectatorBoxSpawn() {
        return getRegion(ArenaRegionType.SPECTATOR_BOX).map(RegionBoundary::getCenterPosition);
    }

    public void clearConfiguration() {
        cornersByRegionType.values().forEach(EnumMap::clear);
        firstTeamSpawn = null;
        secondTeamSpawn = null;
        tribuneSpawn = null;
    }
}
