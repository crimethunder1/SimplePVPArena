package de.simplepvparena.arena;

@FunctionalInterface
public interface ArenaCleaningExclusion {

    ArenaCleaningExclusion NONE = (worldName, blockX, blockY, blockZ) -> false;

    boolean isExcluded(String worldName, int blockX, int blockY, int blockZ);
}
