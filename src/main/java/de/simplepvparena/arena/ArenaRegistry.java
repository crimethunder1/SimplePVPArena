package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Optional;

public final class ArenaRegistry {

    public static final String ARENA_SECTION = "arena";
    public static final String LEGACY_ARENAS_SECTION = "arenas";
    public static final String LEGACY_FINAL_ARENA_KEY = "final-arena";

    private static final String REGIONS_SECTION = "regions";
    private static final String SPAWNS_SECTION = "spawns";
    private static final String FIRST_TEAM_SPAWN_KEY = "first-team";
    private static final String SECOND_TEAM_SPAWN_KEY = "second-team";
    private static final String TRIBUNE_SPAWN_KEY = "tribune";

    private final Arena arena = new Arena();

    public Arena getArena() {
        return arena;
    }

    public boolean isArenaFullyConfigured() {
        return arena.isFullyConfigured();
    }

    public Optional<Arena> findFullyConfiguredArena() {
        return arena.isFullyConfigured() ? Optional.of(arena) : Optional.empty();
    }

    public boolean containsInFightRegion(Location location) {
        return arena.containsInFightRegion(location);
    }

    public void clearArenaConfiguration() {
        arena.clearConfiguration();
    }

    public static boolean containsLegacyMultiArenaConfiguration(ConfigurationSection rootSection) {
        return rootSection.isConfigurationSection(LEGACY_ARENAS_SECTION)
                || rootSection.contains(LEGACY_FINAL_ARENA_KEY);
    }

    public void loadFromConfiguration(ConfigurationSection rootSection) {
        arena.clearConfiguration();
        ConfigurationSection arenaSection = rootSection.getConfigurationSection(ARENA_SECTION);
        if (arenaSection == null) {
            return;
        }
        readRegions(arenaSection.getConfigurationSection(REGIONS_SECTION));
        readSpawns(arenaSection.getConfigurationSection(SPAWNS_SECTION));
    }

    public void saveToConfiguration(ConfigurationSection rootSection) {
        rootSection.set(LEGACY_ARENAS_SECTION, null);
        rootSection.set(LEGACY_FINAL_ARENA_KEY, null);
        ConfigurationSection arenaSection = rootSection.getConfigurationSection(ARENA_SECTION);
        if (arenaSection == null) {
            arenaSection = rootSection.createSection(ARENA_SECTION);
        }
        arenaSection.set(REGIONS_SECTION, null);
        arenaSection.set(SPAWNS_SECTION, null);
        ConfigurationSection regionsSection = arenaSection.createSection(REGIONS_SECTION);
        for (ArenaRegionType regionType : ArenaRegionType.values()) {
            for (RegionCorner corner : RegionCorner.values()) {
                arena.getCorner(regionType, corner).ifPresent(position -> writePosition(
                        regionsSection.createSection(regionType.getCommandArgument() + "." + corner.getCommandArgument()),
                        position));
            }
        }
        ConfigurationSection spawnsSection = arenaSection.createSection(SPAWNS_SECTION);
        arena.getConfiguredFirstTeamSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(FIRST_TEAM_SPAWN_KEY), position));
        arena.getConfiguredSecondTeamSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(SECOND_TEAM_SPAWN_KEY), position));
        arena.getConfiguredTribuneSpawn().ifPresent(
                position -> writePosition(spawnsSection.createSection(TRIBUNE_SPAWN_KEY), position));
    }

    private void readRegions(ConfigurationSection regionsSection) {
        if (regionsSection == null) {
            return;
        }
        for (ArenaRegionType regionType : ArenaRegionType.values()) {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionType.getCommandArgument());
            if (regionSection == null) {
                continue;
            }
            for (RegionCorner corner : RegionCorner.values()) {
                readPosition(regionSection.getConfigurationSection(corner.getCommandArgument()))
                        .ifPresent(position -> arena.setCorner(regionType, corner, position));
            }
        }
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
