package de.simplepvparena.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ArenaRegistry {

    private static final String ARENAS_SECTION = "arenas";
    private static final String REGIONS_SECTION = "regions";
    private static final String SPAWNS_SECTION = "spawns";
    private static final String FIRST_TEAM_SPAWN_KEY = "first-team";
    private static final String SECOND_TEAM_SPAWN_KEY = "second-team";
    private static final String TRIBUNE_SPAWN_KEY = "tribune";

    private final Map<String, Arena> arenasByLowerCaseName = new LinkedHashMap<>();

    public Arena getOrCreateArena(String arenaName) {
        return arenasByLowerCaseName.computeIfAbsent(toKey(arenaName), key -> new Arena(arenaName));
    }

    public Optional<Arena> findArena(String arenaName) {
        if (arenaName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(arenasByLowerCaseName.get(toKey(arenaName)));
    }

    public boolean removeArena(String arenaName) {
        return arenasByLowerCaseName.remove(toKey(arenaName)) != null;
    }

    public Collection<Arena> getArenas() {
        return List.copyOf(arenasByLowerCaseName.values());
    }

    public List<String> getArenaNames() {
        return arenasByLowerCaseName.values().stream().map(Arena::getArenaName).toList();
    }

    public Optional<Arena> findFreeArena() {
        return arenasByLowerCaseName.values().stream()
                .filter(Arena::isFullyConfigured)
                .filter(arena -> !arena.isOccupied())
                .findFirst();
    }

    public Optional<Arena> findArenaByFightRegion(Location location) {
        return arenasByLowerCaseName.values().stream()
                .filter(arena -> arena.containsInFightRegion(location))
                .findFirst();
    }

    public Optional<Arena> findArenaByAnyRegion(Location location) {
        return arenasByLowerCaseName.values().stream()
                .filter(arena -> arena.containsInFightRegion(location)
                        || arena.containsInTribune(location)
                        || arena.containsInSpectatorBox(location))
                .findFirst();
    }

    public void loadFromConfiguration(ConfigurationSection rootSection) {
        arenasByLowerCaseName.clear();
        ConfigurationSection arenasSection = rootSection.getConfigurationSection(ARENAS_SECTION);
        if (arenasSection == null) {
            return;
        }
        for (String arenaName : arenasSection.getKeys(false)) {
            ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
            if (arenaSection == null) {
                continue;
            }
            Arena arena = getOrCreateArena(arenaName);
            readRegions(arenaSection.getConfigurationSection(REGIONS_SECTION), arena);
            readSpawns(arenaSection.getConfigurationSection(SPAWNS_SECTION), arena);
        }
    }

    public void saveToConfiguration(ConfigurationSection rootSection) {
        rootSection.set(ARENAS_SECTION, null);
        ConfigurationSection arenasSection = rootSection.createSection(ARENAS_SECTION);
        for (Arena arena : arenasByLowerCaseName.values()) {
            ConfigurationSection arenaSection = arenasSection.createSection(arena.getArenaName());
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
    }

    private void readRegions(ConfigurationSection regionsSection, Arena arena) {
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

    private void readSpawns(ConfigurationSection spawnsSection, Arena arena) {
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

    private String toKey(String arenaName) {
        return arenaName.toLowerCase(Locale.ROOT);
    }
}
