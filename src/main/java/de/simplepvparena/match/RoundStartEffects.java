package de.simplepvparena.match;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface RoundStartEffects {

    void blind(Player player, int durationTicks);

    void removeBlindness(Player player);

    void playFightStartAnimation(Player player);
}
