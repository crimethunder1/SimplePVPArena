package de.simplepvparena.arena;

import org.bukkit.Material;

public interface ArenaCleaningTarget {

    ArenaCleaningTarget EMPTY_ARENA = new ArenaCleaningTarget() {

        @Override
        public Material findMaterial(int blockX, int blockY, int blockZ) {
            return Material.AIR;
        }

        @Override
        public String describeChange() {
            return "were set to air";
        }
    };

    Material findMaterial(int blockX, int blockY, int blockZ);

    String describeChange();
}
