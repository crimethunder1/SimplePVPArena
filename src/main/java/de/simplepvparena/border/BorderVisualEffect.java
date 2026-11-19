package de.simplepvparena.border;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
public interface BorderVisualEffect {

    void showAt(Player player, Location borderBlockCenter, BorderConnectivity connectivity);

    void showConfirmedAt(Player player, Location borderBlockCenter);

    void applyConfirmedParticleSettings(ConfirmedBorderParticleSettings settings, Consumer<String> problemReporter);

    void showColumnConnection(Player player, Location pointBetweenBorderColumns);

    void showConnection(Player player, Location pointBetweenBorderBlocks);
}
