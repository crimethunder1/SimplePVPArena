package de.simplepvparena.arena;

import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
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
