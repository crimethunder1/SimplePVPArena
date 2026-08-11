package de.simplepvparena.arena;

import org.bukkit.World;

import java.util.function.Consumer;

public interface ArenaCleaningVisualEffect {

    ArenaCleaningVisualEffect NONE = new ArenaCleaningVisualEffect() {

        @Override
        public void showCleanedColumns(World world, ArenaShape shape, long[] cleanedBlockBatch) {
        }

        @Override
        public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                          Consumer<String> problemReporter) {
        }
    };

    void showCleanedColumns(World world, ArenaShape shape, long[] cleanedBlockBatch);

    void applyParticleSettings(ArenaCleaningParticleSettings settings, Consumer<String> problemReporter);
}
