package de.simplepvparena.arena;

import org.bukkit.World;

import java.util.function.Consumer;

public interface ArenaCleaningVisualEffect {

    ArenaCleaningVisualEffect NONE = new ArenaCleaningVisualEffect() {

        @Override
        public void showFloodedLayer(World world, ArenaShape shape, ArenaCleaningPhase phase, int layerY) {
        }

        @Override
        public void applyParticleSettings(ArenaCleaningParticleSettings settings,
                                          Consumer<String> problemReporter) {
        }
    };

    void showFloodedLayer(World world, ArenaShape shape, ArenaCleaningPhase phase, int layerY);

    void applyParticleSettings(ArenaCleaningParticleSettings settings, Consumer<String> problemReporter);
}
