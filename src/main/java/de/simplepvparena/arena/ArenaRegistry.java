package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;

public final class ArenaRegistry {

    public static final String ARENA_SECTION = "arena";
    public static final String LEGACY_ARENAS_SECTION = "arenas";
    public static final String LEGACY_FINAL_ARENA_KEY = "final-arena";
    public static final String LEGACY_REGIONS_SECTION = "regions";

    private static final String SHAPE_SECTION = "shape";
    private static final String SHAPE_WORLD_KEY = "world";
    private static final String SHAPE_REFERENCE_Y_KEY = "reference-y";
    private static final String SHAPE_WALL_TOP_Y_KEY = "wall-top-y";
    private static final String SHAPE_ROWS_KEY = "rows";
    private static final String SPAWNS_SECTION = "spawns";
    private static final String FIRST_TEAM_SPAWN_KEY = "first-team";
    private static final String SECOND_TEAM_SPAWN_KEY = "second-team";
    private static final String TRIBUNE_SPAWN_KEY = "tribune";

    private final Arena arena = new Arena();

    private String shapeLoadFailure;

    public Arena getArena() {
        return arena;
    }

    public boolean isArenaFullyConfigured() {
        return arena.isFullyConfigured();
    }

    public Optional<Arena> findFullyConfiguredArena() {
        return arena.isFullyConfigured() ? Optional.of(arena) : Optional.empty();
    }

    public boolean containsInArena(Location location) {
        return arena.contains(location);
    }

    public void clearArenaConfiguration() {
        arena.clearConfiguration();
    }

    public Optional<String> getShapeLoadFailure() {
        return Optional.ofNullable(shapeLoadFailure);
    }

    public static boolean containsLegacyMultiArenaConfiguration(ConfigurationSection rootSection) {
        return rootSection.isConfigurationSection(LEGACY_ARENAS_SECTION)
                || rootSection.contains(LEGACY_FINAL_ARENA_KEY);
    }

    public static boolean containsLegacyRegionConfiguration(ConfigurationSection rootSection) {
        ConfigurationSection arenaSection = rootSection.getConfigurationSection(ARENA_SECTION);
        if (arenaSection == null) {
            return false;
        }
        ConfigurationSection regionsSection = arenaSection.getConfigurationSection(LEGACY_REGIONS_SECTION);
        return regionsSection != null && !regionsSection.getKeys(false).isEmpty();
    }

    public void loadFromConfiguration(ConfigurationSection rootSection) {
        arena.clearConfiguration();
        shapeLoadFailure = null;
        ConfigurationSection arenaSection = rootSection.getConfigurationSection(ARENA_SECTION);
        if (arenaSection == null) {
            return;
        }
        readShape(arenaSection.getConfigurationSection(SHAPE_SECTION)).ifPresent(arena::setShape);
        readSpawns(arenaSection.getConfigurationSection(SPAWNS_SECTION));
    }

    public void saveToConfiguration(ConfigurationSection rootSection) {
        rootSection.set(LEGACY_ARENAS_SECTION, null);
        rootSection.set(LEGACY_FINAL_ARENA_KEY, null);
        ConfigurationSection arenaSection = rootSection.getConfigurationSection(ARENA_SECTION);
        if (arenaSection == null) {
            arenaSection = rootSection.createSection(ARENA_SECTION);
        }
        arenaSection.set(LEGACY_REGIONS_SECTION, null);
        arenaSection.set(SHAPE_SECTION, null);
        arenaSection.set(SPAWNS_SECTION, null);
        ConfigurationSection shapeSection = arenaSection.createSection(SHAPE_SECTION);
        arena.getShape().ifPresent(shape -> writeShape(shapeSection, shape));
        ConfigurationSection spawnsSection = arenaSection.createSection(SPAWNS_SECTION);
        arena.getConfiguredFirstTeamSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(FIRST_TEAM_SPAWN_KEY), position));
        arena.getConfiguredSecondTeamSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(SECOND_TEAM_SPAWN_KEY), position));
        arena.getConfiguredTribuneSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(TRIBUNE_SPAWN_KEY), position));
    }

    private Optional<ArenaShape> readShape(ConfigurationSection shapeSection) {
        if (shapeSection == null) {
            return Optional.empty();
        }
        String worldName = shapeSection.getString(SHAPE_WORLD_KEY);
        List<String> rows = shapeSection.getStringList(SHAPE_ROWS_KEY);
        if (worldName == null || rows.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ArenaShape(worldName,
                    shapeSection.getInt(SHAPE_REFERENCE_Y_KEY),
                    shapeSection.getInt(SHAPE_WALL_TOP_Y_KEY),
                    ArenaShapeCodec.decodeRows(rows)));
        } catch (IllegalArgumentException failure) {
            shapeLoadFailure = failure.getMessage();
            return Optional.empty();
        }
    }

    private void writeShape(ConfigurationSection shapeSection, ArenaShape shape) {
        shapeSection.set(SHAPE_WORLD_KEY, shape.getWorldName());
        shapeSection.set(SHAPE_REFERENCE_Y_KEY, shape.getReferenceY());
        shapeSection.set(SHAPE_WALL_TOP_Y_KEY, shape.getWallTopY());
        shapeSection.set(SHAPE_ROWS_KEY, ArenaShapeCodec.encodeRows(shape.getCells()));
    }

    private void readSpawns(ConfigurationSection spawnsSection) {
        if (spawnsSection == null) {
            return;
        }
        readPosition(spawnsSection.getConfigurationSection(FIRST_TEAM_SPAWN_KEY)).ifPresent(arena::setFirstTeamSpawn);
        readPosition(spawnsSection.getConfigurationSection(SECOND_TEAM_SPAWN_KEY)).ifPresent(arena::setSecondTeamSpawn);
        readPosition(spawnsSection.getConfigurationSection(TRIBUNE_SPAWN_KEY)).ifPresent(arena::setTribuneSpawn);
    }

    private Optional<PositionSnapshot> readPosition(ConfigurationSection positionSection) {
        if (positionSection == null) {
            return Optional.empty();
        }
        String worldName = positionSection.getString("world");
        if (worldName == null) {
            return Optional.empty();
        }
        return Optional.of(new PositionSnapshot(worldName,
                positionSection.getDouble("x"),
                positionSection.getDouble("y"),
                positionSection.getDouble("z"),
                (float) positionSection.getDouble("yaw"),
                (float) positionSection.getDouble("pitch")));
    }

    private void writePosition(ConfigurationSection positionSection, PositionSnapshot position) {
        positionSection.set("world", position.worldName());
        positionSection.set("x", position.x());
        positionSection.set("y", position.y());
        positionSection.set("z", position.z());
        positionSection.set("yaw", position.yaw());
        positionSection.set("pitch", position.pitch());
    }
}
