package de.simplepvparena.arena;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.Optional;

public interface ArenaCleaningTarget {

    ArenaCleaningTarget EMPTY_ARENA = new ArenaCleaningTarget() {

        @Override
        public Material findMaterial(int blockX, int blockY, int blockZ) {
            return Material.AIR;
        }

        @Override
        public Optional<BlockData> findBlockData(int blockX, int blockY, int blockZ) {
            return Optional.empty();
        }

        @Override
        public Optional<BlockState> findBlockEntity(int blockX, int blockY, int blockZ) {
            return Optional.empty();
        }

        @Override
        public String describeChange() {
            return "were set to air";
        }
    };

    Material findMaterial(int blockX, int blockY, int blockZ);

    Optional<BlockData> findBlockData(int blockX, int blockY, int blockZ);

    Optional<BlockState> findBlockEntity(int blockX, int blockY, int blockZ);

    String describeChange();
}
