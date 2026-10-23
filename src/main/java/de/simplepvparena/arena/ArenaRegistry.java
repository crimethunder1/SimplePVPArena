package de.simplepvparena.arena;

import de.simplepvparena.message.ArenaSetupMessage;
import de.simplepvparena.message.Messages;
import de.simplepvparena.message.Placeholder;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Optional;

public final class ArenaRegistry {

    public static final String ARENA_SECTION = "arena";
    public static final String LEGACY_ARENAS_SECTION = "arenas";
    public static final String LEGACY_FINAL_ARENA_KEY = "final-arena";
    public static final String LEGACY_REGIONS_SECTION = "regions";
    public static final boolean DEFAULT_TELEPORT_ON_JOIN = true;

    private static final String SHAPE_SECTION = "shape";
    private static final String SHAPE_WORLD_KEY = "world";
    private static final String SHAPE_REFERENCE_Y_KEY = "reference-y";
    private static final String SHAPE_FLOOR_Y_KEY = "floor-y";
    private static final String SHAPE_WALL_TOP_Y_KEY = "wall-top-y";
    private static final String SHAPE_HEIGHTS_MARKED_KEY = "heights-marked";
    private static final String SHAPE_ROWS_KEY = "rows";
    private static final String SPAWNS_SECTION = "spawns";
    private static final String FIRST_TEAM_SPAWN_KEY = "first-team";
    private static final String SECOND_TEAM_SPAWN_KEY = "second-team";
    private static final String TRIBUNE_SPAWN_KEY = "tribune";
    private static final String STANDINGS_BOARD_SECTION = "standings-board";

    private final Arena arena = new Arena();

    private String shapeLoadFailure;
    private int effectMargin = EffectArea.DEFAULT_MARGIN;
    private boolean teleportOnJoin = DEFAULT_TELEPORT_ON_JOIN;
    private ArenaShape effectAreaShape;
    private int effectAreaMargin;
    private EffectArea effectArea;

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

    public void setEffectMargin(int effectMargin) {
        this.effectMargin = effectMargin;
    }

    public int getEffectMargin() {
        return effectMargin;
    }

    public void setTeleportOnJoin(boolean teleportOnJoin) {
        this.teleportOnJoin = teleportOnJoin;
    }

    public boolean isTeleportOnJoin() {
        return teleportOnJoin;
    }

    public Optional<EffectArea> findEffectArea() {
        Optional<ArenaShape> shape = arena.getShape();
        if (shape.isEmpty()) {
            return Optional.empty();
        }
        if (effectArea == null || effectAreaShape != shape.get() || effectAreaMargin != effectMargin) {
            effectAreaShape = shape.get();
            effectAreaMargin = effectMargin;
            effectArea = EffectArea.around(effectAreaShape, effectMargin);
        }
        return Optional.of(effectArea);
    }

    public Optional<String> findTribuneOutsideOfTheEffectArea() {
        Optional<EffectArea> currentEffectArea = findEffectArea();
        Optional<PositionSnapshot> tribune = arena.getConfiguredTribuneSpawn();
        if (currentEffectArea.isEmpty() || tribune.isEmpty()) {
            return Optional.empty();
        }
        PositionSnapshot position = tribune.get();
        EffectArea area = currentEffectArea.get();
        if (area.contains(position.worldName(), position.x(), position.z())) {
            return Optional.empty();
        }
        return Optional.of(Messages.text(ArenaSetupMessage.TRIBUNE_OUTSIDE_OF_THE_EFFECT_AREA,
                Placeholder.of("position", position.describe()),
                Placeholder.of("distance", area.describeDistanceOf(position.worldName(), position.x(), position.z()))));
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
        readPosition(arenaSection.getConfigurationSection(STANDINGS_BOARD_SECTION)).ifPresent(arena::setStandingsBoard);
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
        arenaSection.set(STANDINGS_BOARD_SECTION, null);
        ConfigurationSection shapeSection = arenaSection.createSection(SHAPE_SECTION);
        arena.getShape().ifPresent(shape -> writeShape(shapeSection, shape));
        ConfigurationSection spawnsSection = arenaSection.createSection(SPAWNS_SECTION);
        arena.getConfiguredFirstTeamSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(FIRST_TEAM_SPAWN_KEY), position));
        arena.getConfiguredSecondTeamSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(SECOND_TEAM_SPAWN_KEY), position));
        arena.getConfiguredTribuneSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(TRIBUNE_SPAWN_KEY), position));
        ConfigurationSection standingsBoardSection = arenaSection.createSection(STANDINGS_BOARD_SECTION);
        arena.getStandingsBoard().ifPresent(position -> writePosition(standingsBoardSection, position));
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
        int referenceY = shapeSection.getInt(SHAPE_REFERENCE_Y_KEY);
        try {
            return Optional.of(new ArenaShape(worldName, referenceY,
                    shapeSection.getInt(SHAPE_FLOOR_Y_KEY, referenceY),
                    shapeSection.getInt(SHAPE_WALL_TOP_Y_KEY),
                    ArenaShapeCodec.decodeRows(rows),
                    shapeSection.getBoolean(SHAPE_HEIGHTS_MARKED_KEY, false)));
        } catch (IllegalArgumentException failure) {
            shapeLoadFailure = failure.getMessage();
            return Optional.empty();
        }
    }

    private void writeShape(ConfigurationSection shapeSection, ArenaShape shape) {
        shapeSection.set(SHAPE_WORLD_KEY, shape.getWorldName());
        shapeSection.set(SHAPE_REFERENCE_Y_KEY, shape.getReferenceY());
        shapeSection.set(SHAPE_FLOOR_Y_KEY, shape.getFloorY());
        shapeSection.set(SHAPE_WALL_TOP_Y_KEY, shape.getWallTopY());
        shapeSection.set(SHAPE_HEIGHTS_MARKED_KEY, shape.hasMarkedHeights());
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
